package com.example.imagesobserver.presentation.detail.model

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

/** Openable-only pages for the detail [androidx.compose.foundation.pager.HorizontalPager]. */
data class DetailPagerState(
    val pages: List<ImageUrl> = emptyList(),
    val initialPageIndex: InitialPageIndex = InitialPageIndex(0),
    val settledPageIndex: SettledPageIndex = SettledPageIndex(0),
    val visibleUrl: ImageUrl? = null,
)
