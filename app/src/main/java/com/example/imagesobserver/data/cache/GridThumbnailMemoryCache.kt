package com.example.imagesobserver.data.cache

import android.util.LruCache
import com.example.imagesobserver.constants.ProjectConstants
import com.example.imagesobserver.domain.model.ImageUrl
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory LRU of resolved grid thumbnail loads (avoids reload flash when cells leave the viewport).
 *
 * [LruCache] synchronizes [get], [put], [remove], [evictAll], and [snapshot] individually.
 * [evict] wraps prefix deletion in the same monitor so no entry is inserted between snapshot and remove.
 */
@Singleton
class GridThumbnailMemoryCache @Inject constructor() {

    sealed interface Entry {
        data class Displayable(val file: File) : Entry
        data object LoadFailed : Entry
        data object Invalid : Entry
    }

    private val cache = object : LruCache<String, Entry>(
        ProjectConstants.GRID_THUMBNAIL_MEMORY_CACHE_MAX_ENTRIES,
    ) {}

    fun key(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): String =
        "${imageUrl.url}|$widthPx|$heightPx"

    fun get(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): Entry? = cache.get(key(imageUrl, widthPx, heightPx))

    fun put(imageUrl: ImageUrl, widthPx: Int, heightPx: Int, entry: Entry) {
        cache.put(key(imageUrl, widthPx, heightPx), entry)
    }

    fun evict(imageUrl: ImageUrl) {
        val prefix = "${imageUrl.url}|"
        synchronized(cache) {
            cache.snapshot().keys
                .filter { it.startsWith(prefix) }
                .forEach { cache.remove(it) }
        }
    }

    fun clear() {
        cache.evictAll()
    }
}
