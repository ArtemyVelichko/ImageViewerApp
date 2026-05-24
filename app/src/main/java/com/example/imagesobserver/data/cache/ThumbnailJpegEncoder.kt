package com.example.imagesobserver.data.cache

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import com.example.imagesobserver.constants.ProjectConstants
import com.example.imagesobserver.di.IoDispatcher
import com.example.imagesobserver.domain.error.AppErrorFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Downsample decode + scale + JPEG encode for grid thumbnails. */
@Singleton
class ThumbnailJpegEncoder @Inject constructor(
    private val appErrorFactory: AppErrorFactory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun encode(originalFile: File, widthPx: Int, heightPx: Int): ByteArray =
        withContext(ioDispatcher) {
            encodeBytes(
                originalBytes = originalFile.readBytes(),
                widthPx = widthPx,
                heightPx = heightPx,
            )
        }

    private fun encodeBytes(originalBytes: ByteArray, widthPx: Int, heightPx: Int): ByteArray {
        val decoded = decodeSampledBitmap(
            bytes = originalBytes,
            reqWidth = widthPx,
            reqHeight = heightPx,
        )
        val scaled = decoded.scale(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1))
        if (scaled !== decoded) {
            decoded.recycle()
        }
        return scaled.useBitmapBytes { bitmap ->
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                ProjectConstants.IMAGE_CACHE_THUMBNAIL_JPEG_QUALITY,
                stream,
            )
            stream.toByteArray()
        }
    }

    private inline fun <T> Bitmap.useBitmapBytes(block: (Bitmap) -> T): T {
        try {
            return block(this)
        } finally {
            recycle()
        }
    }

    private fun decodeSampledBitmap(bytes: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        bounds.inSampleSize = calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            reqWidth = reqWidth,
            reqHeight = reqHeight,
        )
        bounds.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            ?: throw appErrorFactory.decodeImageBytesFailed()
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        if (width <= 0 || height <= 0) return 1
        var inSampleSize = 1
        val halfWidth = width / 2
        val halfHeight = height / 2
        while (halfWidth / inSampleSize >= reqWidth && halfHeight / inSampleSize >= reqHeight) {
            inSampleSize *= 2
        }
        return max(inSampleSize, 1)
    }
}
