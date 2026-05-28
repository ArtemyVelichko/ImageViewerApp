package com.example.imagesobserver.data.repository

import com.example.imagesobserver.data.cache.ImageCacheFileNaming
import com.example.imagesobserver.data.cache.ThumbnailJpegEncoder
import com.example.imagesobserver.data.cache.ImageDiskCacheStore
import com.example.imagesobserver.di.IoDispatcher
import com.example.imagesobserver.domain.error.AppErrorFactory
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.OriginalImageCacheRepository
import com.example.imagesobserver.domain.repository.ThumbnailImageCacheRepository
import com.example.imagesobserver.domain.validation.DisplayableImageValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Disk cache for grid thumbnails derived from cached originals. Only displayable JPEGs are kept. */
@Singleton
class ThumbnailImageCacheRepositoryImpl @Inject constructor(
    private val diskCacheStore: ImageDiskCacheStore,
    private val originalImageCacheRepository: OriginalImageCacheRepository,
    private val thumbnailJpegEncoder: ThumbnailJpegEncoder,
    private val fileNaming: ImageCacheFileNaming,
    private val displayableImageValidator: DisplayableImageValidator,
    private val appErrorFactory: AppErrorFactory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ThumbnailImageCacheRepository {

    override suspend fun peekThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File? =
        withContext(ioDispatcher) {
            val urlKey = fileNaming.urlKey(imageUrl)
            readValidExisting(diskCacheStore.thumbnailFile(urlKey, widthPx, heightPx))
        }

    override suspend fun ensureThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File =
        withContext(ioDispatcher) {
            val urlKey = fileNaming.urlKey(imageUrl)
            val cachedThumb = diskCacheStore.thumbnailFile(urlKey, widthPx, heightPx)
            readValidExisting(cachedThumb)?.let { return@withContext it }

            val originalFile = originalImageCacheRepository.ensureOriginal(imageUrl)
            val thumbnailBytes = thumbnailJpegEncoder.encode(
                originalFile = originalFile,
                widthPx = widthPx,
                heightPx = heightPx,
            )
            if (!displayableImageValidator.isDisplayable(thumbnailBytes)) {
                throw appErrorFactory.decodeImageBytesFailed()
            }
            diskCacheStore.writeBytes(cachedThumb, thumbnailBytes)
            cachedThumb
        }

    private suspend fun readValidExisting(file: File): File? {
        val existing = diskCacheStore.readExisting(file) ?: return null
        return existing.takeIf { displayableImageValidator.isDisplayable(it) }
    }
}
