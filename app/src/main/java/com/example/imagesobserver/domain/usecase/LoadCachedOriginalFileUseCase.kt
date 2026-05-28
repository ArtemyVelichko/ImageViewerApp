package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.model.ImageGalleryUrlStatus
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.domain.repository.OriginalImageCacheRepository
import kotlinx.coroutines.CancellationException
import java.io.File
import javax.inject.Inject

/** Resolves a cached original [File] for the detail screen; null when cache/network failed. */
class LoadCachedOriginalFileUseCase @Inject constructor(
    private val originalImageCacheRepository: OriginalImageCacheRepository,
    private val imageGalleryRepository: ImageGalleryRepository,
) {

    suspend operator fun invoke(imageUrl: ImageUrl): File? {
        if (imageGalleryRepository.getUrlStatus(imageUrl) == ImageGalleryUrlStatus.Broken) return null
        originalImageCacheRepository.peekOriginal(imageUrl)?.let { return it }
        return try {
            originalImageCacheRepository.ensureOriginal(imageUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            imageGalleryRepository.markBroken(imageUrl)
            null
        }
    }
}
