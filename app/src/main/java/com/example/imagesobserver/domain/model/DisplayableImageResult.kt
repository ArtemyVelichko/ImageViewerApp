package com.example.imagesobserver.domain.model

import java.io.File

/** Cached image file that passed [com.example.imagesobserver.domain.validation.DisplayableImageValidator]. */
sealed interface DisplayableImageResult {

    data class Displayable(val file: File) : DisplayableImageResult

    data object NotDisplayable : DisplayableImageResult
}
