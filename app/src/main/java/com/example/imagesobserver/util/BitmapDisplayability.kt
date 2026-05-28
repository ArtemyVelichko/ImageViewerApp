package com.example.imagesobserver.util

import android.graphics.BitmapFactory
import java.io.File

/** True when [bytes] are non-empty and [BitmapFactory] can read image bounds. */
fun ByteArray.isBitmapValid(): Boolean {
    if (isEmpty()) return false
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(this, 0, size, options)
    return options.outWidth > 0 && options.outHeight > 0
}

/** True when this file exists, is non-empty, and [BitmapFactory] can read image bounds. */
fun File.isBitmapValid(): Boolean {
    if (!isFile || length() <= 0L) return false
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(absolutePath, options)
    return options.outWidth > 0 && options.outHeight > 0
}
