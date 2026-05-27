package com.example.imagesobserver.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * All Compose [androidx.compose.ui.unit.Dp] tokens for the UI layer.
 */
object Dimens {

    /** Padding around the full-screen images list error block. */
    val imagesListErrorScreenPadding = 24.dp

    /** Space between error message and retry button on the images list. */
    val imagesListErrorItemSpacing = 16.dp

    /** Top/bottom padding inside the images grid scroll area. */
    val imageGridContentPaddingVertical = 16.dp

    /** Inset for the grid refresh indicator shown while reloading. */
    val imageGridLoadingIndicatorPadding = 16.dp

    /** Horizontal inset for the image grid (left + right). */
    val imageGridHorizontalMargin = 40.dp

    /** Thumbnail row height in the grid (decode/cache target height). */
    val imageThumbnailMaxHeight = 100.dp

    val imageGridCellSpacing = 8.dp

    /** Target cell width band for column count (TZ: 100–120 dp). */
    val imageGridCellMinWidth = 100.dp

    val imageGridCellMaxWidth = 120.dp

    val imageThumbnailCorner = 8.dp

    /** Grid cell reload button — fills most of the cell width (cells are 100–120 dp). */
    val imageGridRetryButtonMinHeight = 52.dp

    val imageGridRetryContentPadding = 6.dp

    /** Small inline progress indicator (e.g. thumbnail slot while building Coil request). */
    val circularProgressIndicatorSmall = 24.dp

    /**
     * Horizontal drag shorter than this is treated as image pan (when zoomed).
     * At or above — pager swipe between images.
     */
    val imageHorizontalPanSwipeThreshold = 48.dp
}
