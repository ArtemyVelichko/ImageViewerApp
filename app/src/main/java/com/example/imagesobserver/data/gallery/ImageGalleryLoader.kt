package com.example.imagesobserver.data.gallery

import com.example.imagesobserver.constants.ProjectConstants
import com.example.imagesobserver.domain.model.GridThumbnailResult
import com.example.imagesobserver.domain.model.ImageGalleryUrlStatus
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.model.ManifestGridRow
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.domain.usecase.ResolveGridThumbnailUseCase
import com.example.imagesobserver.di.ApplicationScope
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * App-wide thumbnail/original loading for grid and detail.
 *
 * Thumbnail target size and manifest rows live here — not in a ViewModel.
 * Grid reports cell size + visible rows; loader runs ordered prefetch (32 parallel max).
 */
@Singleton
class ImageGalleryLoader @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val resolveGridThumbnailUseCase: ResolveGridThumbnailUseCase,
    private val imageGalleryRepository: ImageGalleryRepository,
) {

    private val workerLock = Any()
    private val sessionLock = Any()
    private var prefetchWorker: Job? = null
    private val loadMutexes = ConcurrentHashMap<String, Mutex>()

    private var manifestRows: List<ManifestGridRow> = emptyList()
    private var thumbnailTargetWidthPx: Int = 0
    private var thumbnailTargetHeightPx: Int = 0
    private var visibleRowIndices: IntRange? = null

    /** Manifest list changed (initial load or refresh). Re-runs prefetch when target size is known. */
    fun onManifestRowsChanged(rows: List<ManifestGridRow>) {
        synchronized(sessionLock) {
            manifestRows = rows
        }
        restartPrefetchIfReady()
    }

    /** Grid cell size in physical pixels (from [androidx.compose.ui.layout.onSizeChanged] / layout math). */
    fun onGridThumbnailTargetChanged(widthPx: Int, heightPx: Int) {
        synchronized(sessionLock) {
            thumbnailTargetWidthPx = widthPx
            thumbnailTargetHeightPx = heightPx
        }
        restartPrefetchIfReady()
    }

    fun onVisibleRowsChanged(firstVisibleIndex: Int, lastVisibleIndex: Int) {
        if (firstVisibleIndex > lastVisibleIndex) return
        val next = firstVisibleIndex..lastVisibleIndex
        synchronized(sessionLock) {
            if (next == visibleRowIndices) return
            visibleRowIndices = next
        }
        restartPrefetchIfReady()
    }

    suspend fun loadThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File? {
        if (imageGalleryRepository.getUrlStatus(imageUrl) == ImageGalleryUrlStatus.Broken) return null
        when (resolveGridThumbnailUseCase.peek(imageUrl, widthPx, heightPx)) {
            GridThumbnailResult.Invalid -> {
                imageGalleryRepository.markBroken(imageUrl)
                return null
            }
            is GridThumbnailResult.Displayable -> {
                if (imageGalleryRepository.getUrlStatus(imageUrl) == ImageGalleryUrlStatus.Loading) {
                    imageGalleryRepository.markOpenable(imageUrl)
                }
                return displayableFile(imageUrl, widthPx, heightPx)
            }
            GridThumbnailResult.LoadFailed, null -> Unit
        }
        if (isAlreadyDisplayable(imageUrl, widthPx, heightPx)) {
            if (imageGalleryRepository.getUrlStatus(imageUrl) == ImageGalleryUrlStatus.Loading) {
                imageGalleryRepository.markOpenable(imageUrl)
            }
            return displayableFile(imageUrl, widthPx, heightPx)
        }
        val mutex = loadMutexes.computeIfAbsent(jobKey(imageUrl, widthPx, heightPx)) { Mutex() }
        return mutex.withLock {
            if (isAlreadyDisplayable(imageUrl, widthPx, heightPx)) {
                return@withLock displayableFile(imageUrl, widthPx, heightPx)
            }
            applyResult(
                imageUrl = imageUrl,
                result = resolveGridThumbnailUseCase(imageUrl, widthPx, heightPx),
            )
        }
    }

    fun retry(imageUrl: ImageUrl, widthPx: Int, heightPx: Int) {
        resolveGridThumbnailUseCase.evict(imageUrl)
        imageGalleryRepository.markLoading(imageUrl)
        applicationScope.launch {
            loadThumbnail(imageUrl, widthPx, heightPx)
        }
    }

    fun getGridThumbnailTargetPx(): Pair<Int, Int>? = synchronized(sessionLock) {
        if (thumbnailTargetWidthPx <= 0 || thumbnailTargetHeightPx <= 0) return null
        thumbnailTargetWidthPx to thumbnailTargetHeightPx
    }

    fun cancelAll() {
        synchronized(workerLock) {
            prefetchWorker?.cancel()
            prefetchWorker = null
        }
        synchronized(sessionLock) {
            manifestRows = emptyList()
            thumbnailTargetWidthPx = 0
            thumbnailTargetHeightPx = 0
            visibleRowIndices = null
        }
    }

    private fun restartPrefetchIfReady() {
        val session = synchronized(sessionLock) {
            if (thumbnailTargetWidthPx <= 0 || thumbnailTargetHeightPx <= 0 || manifestRows.isEmpty()) {
                return
            }
            PrefetchSession(
                rows = manifestRows,
                widthPx = thumbnailTargetWidthPx,
                heightPx = thumbnailTargetHeightPx,
                visibleRowIndices = visibleRowIndices,
            )
        }
        val orderedUrls = buildPrefetchOrder(session.rows, session.visibleRowIndices)
        if (orderedUrls.isEmpty()) return
        synchronized(workerLock) {
            prefetchWorker?.cancel()
            prefetchWorker = applicationScope.launch {
                runPrefetchBatch(orderedUrls, session.widthPx, session.heightPx)
            }
        }
    }

    private suspend fun runPrefetchBatch(
        orderedUrls: List<ImageUrl>,
        widthPx: Int,
        heightPx: Int,
    ) {
        val semaphore = Semaphore(ProjectConstants.IMAGE_GALLERY_PREFETCH_MAX_CONCURRENCY)
        coroutineScope {
            orderedUrls.map { imageUrl ->
                async {
                    semaphore.withPermit {
                        if (!isActive) return@async
                        if (imageGalleryRepository.getUrlStatus(imageUrl) == ImageGalleryUrlStatus.Broken) {
                            return@async
                        }
                        if (isAlreadyDisplayable(imageUrl, widthPx, heightPx)) return@async
                        loadThumbnail(imageUrl, widthPx, heightPx)
                    }
                }
            }.awaitAll()
        }
    }

    private fun buildPrefetchOrder(
        rows: List<ManifestGridRow>,
        visibleRowIndices: IntRange?,
    ): List<ImageUrl> {
        val linksByRowIndex = rows.mapIndexedNotNull { index, row ->
            when (row) {
                is ManifestGridRow.Link -> index to row.url
                is ManifestGridRow.InvalidLine -> null
            }
        }
        if (linksByRowIndex.isEmpty()) return emptyList()
        if (visibleRowIndices == null) {
            return linksByRowIndex.map { it.second }
        }
        val visibleRows = visibleRowIndices.filter { it in rows.indices }.toSet()
        val priority = linksByRowIndex
            .filter { (rowIndex, _) -> rowIndex in visibleRows }
            .sortedBy { it.first }
            .map { it.second }
        val priorityUrls = priority.map { it.url }.toSet()
        val rest = linksByRowIndex
            .filter { (_, url) -> url.url !in priorityUrls }
            .sortedBy { it.first }
            .map { it.second }
        return priority + rest
    }

    private fun displayableFile(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File? =
        (resolveGridThumbnailUseCase.peek(imageUrl, widthPx, heightPx)
            as? GridThumbnailResult.Displayable)?.file

    private fun isAlreadyDisplayable(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): Boolean =
        resolveGridThumbnailUseCase.peek(imageUrl, widthPx, heightPx) is GridThumbnailResult.Displayable

    private fun applyResult(imageUrl: ImageUrl, result: GridThumbnailResult): File? =
        when (result) {
            is GridThumbnailResult.Displayable -> {
                imageGalleryRepository.markOpenable(imageUrl)
                result.file
            }
            GridThumbnailResult.LoadFailed -> {
                imageGalleryRepository.markLoading(imageUrl)
                null
            }
            GridThumbnailResult.Invalid -> {
                imageGalleryRepository.markBroken(imageUrl)
                null
            }
        }

    private fun jobKey(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): String =
        "${imageUrl.url}|$widthPx|$heightPx"

    private data class PrefetchSession(
        val rows: List<ManifestGridRow>,
        val widthPx: Int,
        val heightPx: Int,
        val visibleRowIndices: IntRange?,
    )
}
