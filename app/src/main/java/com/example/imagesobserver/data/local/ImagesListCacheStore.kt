package com.example.imagesobserver.data.local

import com.example.imagesobserver.data.local.provider.ResourceProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** On-disk cache for the remote images list ([images.txt] body). */
@Singleton
class ImagesListCacheStore @Inject constructor(
    private val resourceProvider: ResourceProvider,
) {

    private val cacheMutex = Mutex()

    private val cacheFile: File by lazy { resourceProvider.remoteImagesListCacheFile() }

    suspend fun readList(): String? = cacheMutex.withLock {
        if (!cacheFile.exists()) return@withLock null
        cacheFile.readText()
    }

    suspend fun writeList(content: String) = cacheMutex.withLock {
        cacheFile.parentFile?.mkdirs()
        cacheFile.writeText(content)
    }
}
