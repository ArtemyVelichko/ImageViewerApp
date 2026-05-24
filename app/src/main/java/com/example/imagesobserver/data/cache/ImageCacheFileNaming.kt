package com.example.imagesobserver.data.cache

import com.example.imagesobserver.di.IoDispatcher
import com.example.imagesobserver.domain.model.ImageUrl
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Builds stable on-disk keys and extensions for cached image files. */
@Singleton
class ImageCacheFileNaming @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun urlKey(imageUrl: ImageUrl): String = withContext(ioDispatcher) {
        val digest = MessageDigest.getInstance("SHA-256").digest(imageUrl.url.toByteArray())
        digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    /** Preserves original extension when recognized; defaults to `.jpg`. */
    suspend fun extensionFromUrl(imageUrl: ImageUrl): String = withContext(ioDispatcher) {
        val path = imageUrl.url.substringBefore('?').substringAfterLast('/')
        val dotIndex = path.lastIndexOf('.')
        if (dotIndex in 1 until path.lastIndex) {
            val ext = path.substring(dotIndex + 1).lowercase()
            if (ext in SUPPORTED_EXTENSIONS) return@withContext ".$ext"
        }
        ".jpg"
    }

    private companion object {
        private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")
    }
}
