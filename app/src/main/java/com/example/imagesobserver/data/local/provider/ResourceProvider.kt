package com.example.imagesobserver.data.local.provider

import androidx.annotation.StringRes
import java.io.File

/**
 * App-level paths and resources without exposing [android.content.Context] to data stores.
 */
interface ResourceProvider {

    /** Private app files dir + cached remote images list (not in project sources). */
    fun remoteImagesListCacheFile(): File

    /** Directory for cached original image bytes. */
    fun imageCacheOriginalsDir(): File

    /** Directory for cached thumbnail image files. */
    fun imageCacheThumbnailsDir(): File

    fun getString(@StringRes resId: Int): String

    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
}
