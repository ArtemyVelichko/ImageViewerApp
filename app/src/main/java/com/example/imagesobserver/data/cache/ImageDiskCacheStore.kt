package com.example.imagesobserver.data.cache

import com.example.imagesobserver.constants.ProjectConstants
import com.example.imagesobserver.data.local.provider.ResourceProvider
import com.example.imagesobserver.di.IoDispatcher
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** File-backed cache for original and thumbnail image bytes with LRU eviction by total size. */
@Singleton
class ImageDiskCacheStore @Inject constructor(
    private val resourceProvider: ResourceProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    private val mutex = Mutex()

    suspend fun originalFile(urlKey: String, extension: String): File = withContext(ioDispatcher) {
        mutex.withLock {
            File(resourceProvider.imageCacheOriginalsDir(), "$urlKey$extension")
        }
    }

    suspend fun thumbnailFile(urlKey: String, widthPx: Int, heightPx: Int): File = withContext(ioDispatcher) {
        mutex.withLock {
            File(
                resourceProvider.imageCacheThumbnailsDir(),
                "${urlKey}_${widthPx}x${heightPx}.jpg",
            )
        }
    }

    suspend fun readExisting(file: File): File? = withContext(ioDispatcher) {
        mutex.withLock {
            file.takeIf { it.isFile && it.length() > 0L }?.also { touch(it) }
        }
    }

    suspend fun writeBytes(file: File, bytes: ByteArray) = withContext(ioDispatcher) {
        mutex.withLock {
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            touch(file)
            evictIfOverLimit()
        }
    }

    private fun touch(file: File) {
        file.setLastModified(System.currentTimeMillis())
    }

    private fun evictIfOverLimit() {
        val maxBytes = ProjectConstants.IMAGE_CACHE_MAX_BYTES
        val cacheFiles = cachedFiles()
        var totalBytes = cacheFiles.sumOf { it.length() }
        if (totalBytes <= maxBytes) return

        val targetBytes = (maxBytes * ProjectConstants.IMAGE_CACHE_EVICT_TARGET_RATIO).toLong()
        cacheFiles
            .sortedBy { it.lastModified() }
            .forEach { candidate ->
                if (totalBytes <= targetBytes) return
                val size = candidate.length()
                if (candidate.delete()) {
                    totalBytes -= size
                }
            }
    }

    private fun cachedFiles(): List<File> =
        listOf(
            resourceProvider.imageCacheOriginalsDir(),
            resourceProvider.imageCacheThumbnailsDir(),
        ).flatMap { dir -> dir.listFiles()?.filter { it.isFile }.orEmpty() }
}
