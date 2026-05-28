package com.example.imagesobserver.data.repository

import com.example.imagesobserver.data.cache.ImageBytesDownloader
import com.example.imagesobserver.data.cache.ImageCacheFileNaming
import com.example.imagesobserver.data.cache.ImageDiskCacheStore
import com.example.imagesobserver.di.IoDispatcher
import com.example.imagesobserver.domain.error.AppErrorFactory
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.OriginalImageCacheRepository
import com.example.imagesobserver.domain.validation.DisplayableImageValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Disk + network cache for full-size image bytes. Only displayable bitmaps are kept on disk. */
@Singleton
class OriginalImageCacheRepositoryImpl @Inject constructor(
    private val diskCacheStore: ImageDiskCacheStore,
    private val imageBytesDownloader: ImageBytesDownloader,
    private val fileNaming: ImageCacheFileNaming,
    private val displayableImageValidator: DisplayableImageValidator,
    private val appErrorFactory: AppErrorFactory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OriginalImageCacheRepository {

    override suspend fun peekOriginal(imageUrl: ImageUrl): File? =
        withContext(ioDispatcher) {
            val urlKey = fileNaming.urlKey(imageUrl)
            val extension = fileNaming.extensionFromUrl(imageUrl)
            readValidExisting(diskCacheStore.originalFile(urlKey, extension))
        }

    override suspend fun ensureOriginal(imageUrl: ImageUrl): File =
        withContext(ioDispatcher) {
            val urlKey = fileNaming.urlKey(imageUrl)
            val extension = fileNaming.extensionFromUrl(imageUrl)
            val cachedFile = diskCacheStore.originalFile(urlKey, extension)
            readValidExisting(cachedFile)?.let { return@withContext it }

            val bytes = imageBytesDownloader.download(imageUrl)
            if (!displayableImageValidator.isDisplayable(bytes)) {
                throw appErrorFactory.decodeImageBytesFailed()
            }
            diskCacheStore.writeBytes(cachedFile, bytes)
            cachedFile
        }

    private suspend fun readValidExisting(file: File): File? {
        val existing = diskCacheStore.readExisting(file) ?: return null
        return existing.takeIf { displayableImageValidator.isDisplayable(it) }
    }
}
