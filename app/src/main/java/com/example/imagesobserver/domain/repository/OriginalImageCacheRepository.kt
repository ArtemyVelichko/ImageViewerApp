package com.example.imagesobserver.domain.repository

import com.example.imagesobserver.domain.model.ImageUrl
import java.io.File

/** Domain port: full-size image bytes on disk (original quality). */
interface OriginalImageCacheRepository {

    suspend fun peekOriginal(imageUrl: ImageUrl): File?

    suspend fun ensureOriginal(imageUrl: ImageUrl): File
}
