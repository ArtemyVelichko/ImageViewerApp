package com.example.imagesobserver.data.gallery

import com.example.imagesobserver.domain.model.ManifestGridRow

/** Grid inputs for thumbnail prefetch (manifest, cell size, visible rows). */
data class GalleryGridSession(
    val manifestRows: List<ManifestGridRow> = emptyList(),
    val thumbnailWidthPx: Int = 0,
    val thumbnailHeightPx: Int = 0,
    val visibleRowIndices: IntRange? = null,
) {

    fun isReadyForPrefetch(): Boolean =
        thumbnailWidthPx > 0 && thumbnailHeightPx > 0 && manifestRows.isNotEmpty()

    fun thumbnailTargetPx(): Pair<Int, Int>? =
        if (isReadyForPrefetch()) thumbnailWidthPx to thumbnailHeightPx else null
}
