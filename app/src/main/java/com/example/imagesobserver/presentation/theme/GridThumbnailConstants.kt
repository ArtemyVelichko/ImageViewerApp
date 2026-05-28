package com.example.imagesobserver.presentation.theme

import androidx.compose.ui.layout.ContentScale

/** UI tokens for [com.example.imagesobserver.presentation.images.components.GridThumbnailCoilImage]. */
object GridThumbnailConstants {

    /** Broken placeholder and cells that cannot open detail. */
    const val DISABLED_CELL_ALPHA = 0.72f

    const val FULL_ALPHA = 1f

    /** First load attempt; skips network when memory cache already has a displayable file. */
    const val INITIAL_RELOAD_ATTEMPT = 0

    val contentScale = ContentScale.Crop
}
