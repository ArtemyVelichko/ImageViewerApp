package com.example.imagesobserver.presentation.detail.model

import com.example.imagesobserver.domain.model.ImageGalleryUrlStatus
import com.example.imagesobserver.domain.model.ImageUrl

@JvmInline
value class InitialPageIndex(val index: Int) {
    fun coerceIn(minimumValue: Int, maximumValue: Int): InitialPageIndex =
        InitialPageIndex(index.coerceIn(minimumValue, maximumValue))
}

@JvmInline
value class SettledPageIndex(val index: Int) {
    fun coerceIn(minimumValue: Int, maximumValue: Int): SettledPageIndex =
        SettledPageIndex(index.coerceIn(minimumValue, maximumValue))
}

/**
 * Single source of truth for the detail [androidx.compose.foundation.pager.HorizontalPager].
 */
data class DetailPagerState(
    val urls: List<ImageUrl> = emptyList(),
    val urlStatuses: Map<String, ImageGalleryUrlStatus> = emptyMap(),
    val initialPageIndex: InitialPageIndex = InitialPageIndex(0),
    val settledPageIndex: SettledPageIndex = SettledPageIndex(0),
    val visibleUrl: ImageUrl? = null,
) {

    fun statusFor(imageUrl: ImageUrl): ImageGalleryUrlStatus =
        urlStatuses[imageUrl.url] ?: ImageGalleryUrlStatus.Loading
}
