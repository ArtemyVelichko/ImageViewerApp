package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.ThumbnailImageCacheRepository
import kotlinx.coroutines.CancellationException
import java.io.File
import javax.inject.Inject

/** Grid preview: disk peek, then download/encode thumbnail at cell size in physical pixels. */
class LoadGridThumbnailUseCase @Inject constructor(
    private val thumbnailImageCacheRepository: ThumbnailImageCacheRepository,
) {

    suspend operator fun invoke(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File? {
        thumbnailImageCacheRepository.peekThumbnail(imageUrl, widthPx, heightPx)?.let { return it }
        return try {
            thumbnailImageCacheRepository.ensureThumbnail(imageUrl, widthPx, heightPx)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
