package com.example.imagesobserver.data.gallery

import com.example.imagesobserver.constants.ProjectConstants
import com.example.imagesobserver.domain.model.ImageGalleryUrlStatus
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Singleton
class GridThumbnailPrefetchWorker @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val thumbnailLoader: GridThumbnailLoader,
    private val imageGalleryRepository: ImageGalleryRepository,
) {

    private val workerLock = Any()
    private var prefetchWorker: Job? = null

    fun restart(session: GalleryGridSession) {
        if (!session.isReadyForPrefetch()) return
        val orderedUrls = GridThumbnailPrefetchOrder.orderedUrls(
            rows = session.manifestRows,
            visibleRowIndices = session.visibleRowIndices,
        )
        if (orderedUrls.isEmpty()) return
        val widthPx = session.thumbnailWidthPx
        val heightPx = session.thumbnailHeightPx
        synchronized(workerLock) {
            prefetchWorker?.cancel()
            prefetchWorker = applicationScope.launch {
                runPrefetchBatch(orderedUrls, widthPx, heightPx)
            }
        }
    }

    fun cancel() {
        synchronized(workerLock) {
            prefetchWorker?.cancel()
            prefetchWorker = null
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
                        if (thumbnailLoader.isDisplayableCached(imageUrl, widthPx, heightPx)) return@async
                        thumbnailLoader.load(imageUrl, widthPx, heightPx)
                    }
                }
            }.awaitAll()
        }
    }
}
