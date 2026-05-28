package com.example.imagesobserver.domain.repository

import com.example.imagesobserver.domain.model.ImageUrl
import java.io.File

/** Domain port: grid thumbnail files on disk keyed by URL and display pixel size. */
interface ThumbnailImageCacheRepository {

    /** Returns a cached thumbnail [File] when present and displayable; never downloads or encodes. */
    suspend fun peekThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File?

    /**
     * Returns a displayable thumbnail [File], creating it from the cached original when missing.
     * @throws AppException when encoded bytes fail validation.
     */
    suspend fun ensureThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File
}
