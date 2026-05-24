package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.OriginalImageCacheRepository
import kotlinx.coroutines.CancellationException
import java.io.File
import javax.inject.Inject

/** Resolves a cached original [File] for the detail screen; null when cache/network failed. */
class LoadCachedOriginalUseCase @Inject constructor(
    private val originalImageCacheRepository: OriginalImageCacheRepository,
) {

    suspend operator fun invoke(imageUrl: ImageUrl): File? {
        originalImageCacheRepository.peekOriginal(imageUrl)?.let { return it }
        return try {
            originalImageCacheRepository.ensureOriginal(imageUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
