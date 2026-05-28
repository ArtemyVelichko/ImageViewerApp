package com.example.imagesobserver.domain.validation

import com.example.imagesobserver.domain.model.DisplayableImageResult
import java.io.File

/** Checks whether cached bytes can be decoded as a bitmap for display. */
interface DisplayableImageValidator {

    suspend fun isDisplayable(file: File): Boolean

    suspend fun isDisplayable(bytes: ByteArray): Boolean
}

suspend fun DisplayableImageValidator.resolveDisplayable(file: File?): DisplayableImageResult {
    if (file == null) return DisplayableImageResult.NotDisplayable
    return if (isDisplayable(file)) {
        DisplayableImageResult.Displayable(file)
    } else {
        DisplayableImageResult.NotDisplayable
    }
}
