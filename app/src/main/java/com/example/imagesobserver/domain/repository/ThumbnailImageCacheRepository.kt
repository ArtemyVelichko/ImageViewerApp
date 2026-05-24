package com.example.imagesobserver.domain.repository

import com.example.imagesobserver.domain.model.ImageUrl
import java.io.File

/** Domain port: grid thumbnail files on disk keyed by URL and display pixel size. */
interface ThumbnailImageCacheRepository {

    suspend fun peekThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File?

    suspend fun ensureThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File
}
