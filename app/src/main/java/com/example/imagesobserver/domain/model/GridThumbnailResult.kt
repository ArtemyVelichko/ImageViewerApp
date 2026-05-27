package com.example.imagesobserver.domain.model

import java.io.File

/** Resolved grid thumbnail: memory cache, disk load, and bitmap validation. */
sealed interface GridThumbnailResult {

    data class Displayable(val file: File) : GridThumbnailResult

    /** Network/cache miss — retryable from the grid cell. */
    data object LoadFailed : GridThumbnailResult

    /** Bytes are not a decodable image — not retryable. */
    data object Invalid : GridThumbnailResult
}
