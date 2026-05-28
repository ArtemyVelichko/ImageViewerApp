package com.example.imagesobserver.data.gallery

import com.example.imagesobserver.domain.model.GridThumbnailResult
import com.example.imagesobserver.domain.model.ImageGalleryUrlStatus
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.domain.usecase.ResolveGridThumbnailUseCase
import com.example.imagesobserver.di.ApplicationScope
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class GridThumbnailLoader @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val resolveGridThumbnailUseCase: ResolveGridThumbnailUseCase,
    private val imageGalleryRepository: ImageGalleryRepository,
) {

    private val loadMutexes = ConcurrentHashMap<String, Mutex>()

    suspend fun load(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File? {
        if (imageGalleryRepository.getUrlStatus(imageUrl) == ImageGalleryUrlStatus.Broken) return null
        when (resolveGridThumbnailUseCase.peek(imageUrl, widthPx, heightPx)) {
            GridThumbnailResult.Invalid -> {
                imageGalleryRepository.markUrlBroken(imageUrl)
                return null
            }
            is GridThumbnailResult.Displayable -> {
                markUrlOpenableIfLoading(imageUrl)
                return displayableFile(imageUrl, widthPx, heightPx)
            }
            GridThumbnailResult.LoadFailed, null -> Unit
        }
        if (isDisplayableCached(imageUrl, widthPx, heightPx)) {
            markUrlOpenableIfLoading(imageUrl)
            return displayableFile(imageUrl, widthPx, heightPx)
        }
        val mutex = loadMutexes.computeIfAbsent(jobKey(imageUrl, widthPx, heightPx)) { Mutex() }
        return mutex.withLock {
            if (isDisplayableCached(imageUrl, widthPx, heightPx)) {
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
        imageGalleryRepository.markUrlLoading(imageUrl)
        applicationScope.launch {
            load(imageUrl, widthPx, heightPx)
        }
    }

    fun isDisplayableCached(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): Boolean =
        resolveGridThumbnailUseCase.peek(imageUrl, widthPx, heightPx) is GridThumbnailResult.Displayable

    private fun markUrlOpenableIfLoading(imageUrl: ImageUrl) {
        if (imageGalleryRepository.getUrlStatus(imageUrl) == ImageGalleryUrlStatus.Loading) {
            imageGalleryRepository.markUrlOpenable(imageUrl)
        }
    }

    private fun displayableFile(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File? =
        (resolveGridThumbnailUseCase.peek(imageUrl, widthPx, heightPx)
            as? GridThumbnailResult.Displayable)?.file

    private fun applyResult(imageUrl: ImageUrl, result: GridThumbnailResult): File? =
        when (result) {
            is GridThumbnailResult.Displayable -> {
                imageGalleryRepository.markUrlOpenable(imageUrl)
                result.file
            }
            GridThumbnailResult.LoadFailed -> {
                imageGalleryRepository.markUrlLoading(imageUrl)
                null
            }
            GridThumbnailResult.Invalid -> {
                imageGalleryRepository.markUrlBroken(imageUrl)
                null
            }
        }

    private fun jobKey(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): String =
        "${imageUrl.url}|$widthPx|$heightPx"
}
