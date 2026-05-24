package com.example.imagesobserver.data.repository

import com.example.imagesobserver.data.cache.ImageBytesDownloader
import com.example.imagesobserver.data.cache.ImageCacheFileNaming
import com.example.imagesobserver.data.cache.ImageDiskCacheStore
import com.example.imagesobserver.di.IoDispatcher
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.OriginalImageCacheRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Disk + network cache for full-size image bytes. */
@Singleton
class OriginalImageCacheRepositoryImpl @Inject constructor(
    private val diskCacheStore: ImageDiskCacheStore,
    private val imageBytesDownloader: ImageBytesDownloader,
    private val fileNaming: ImageCacheFileNaming,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OriginalImageCacheRepository {

    override suspend fun peekOriginal(imageUrl: ImageUrl): File? =
        withContext(ioDispatcher) {
            val urlKey = fileNaming.urlKey(imageUrl)
            val extension = fileNaming.extensionFromUrl(imageUrl)
            diskCacheStore.readExisting(diskCacheStore.originalFile(urlKey, extension))
        }

    override suspend fun ensureOriginal(imageUrl: ImageUrl): File =
        withContext(ioDispatcher) {
            val urlKey = fileNaming.urlKey(imageUrl)
            val extension = fileNaming.extensionFromUrl(imageUrl)
            val cachedFile = diskCacheStore.originalFile(urlKey, extension)
            diskCacheStore.readExisting(cachedFile)?.let { return@withContext it }

            val bytes = imageBytesDownloader.download(imageUrl)
            diskCacheStore.writeBytes(cachedFile, bytes)
            cachedFile
        }
}
