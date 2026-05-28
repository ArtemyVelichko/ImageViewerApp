package com.example.imagesobserver.data.local.provider

import android.content.Context
import androidx.annotation.StringRes
import com.example.imagesobserver.constants.ProjectConstants.IMAGE_CACHE_ORIGINALS_DIR_NAME
import com.example.imagesobserver.constants.ProjectConstants.IMAGE_CACHE_THUMBNAILS_DIR_NAME
import com.example.imagesobserver.constants.ProjectConstants.REMOTE_IMAGES_LIST_CACHE_FILE_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ResourceProvider {

    override fun remoteImagesListCacheFile(): File = File(context.filesDir, REMOTE_IMAGES_LIST_CACHE_FILE_NAME)

    override fun imageCacheOriginalsDir(): File =
        File(context.filesDir, IMAGE_CACHE_ORIGINALS_DIR_NAME).also { it.mkdirs() }

    override fun imageCacheThumbnailsDir(): File =
        File(context.filesDir, IMAGE_CACHE_THUMBNAILS_DIR_NAME).also { it.mkdirs() }

    override fun getString(@StringRes resId: Int): String = context.getString(resId)

    override fun getString(@StringRes resId: Int, vararg formatArgs: Any): String =
        context.getString(resId, *formatArgs)

    override fun displayDensity(): Float = context.resources.displayMetrics.density

    override fun dpToPx(dp: Float): Int =
        (dp * displayDensity() + 0.5f).toInt()
}
