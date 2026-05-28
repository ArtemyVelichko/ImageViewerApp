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

/**
 * Single source of truth for the detail [androidx.compose.foundation.pager.HorizontalPager].
 *
 * - [initialPageIndex] — only for [androidx.compose.foundation.pager.rememberPagerState] on first entry.
 * - [settledPageIndex] — last settled pager page; updated together with [visibleUrl].
 * - [visibleUrl] — anchor URL for share/browser and for reconciling [urls] after manifest mutations.
 */
data class DetailPagerState(
    val urls: List<ImageUrl> = emptyList(),
    val initialPageIndex: InitialPageIndex = InitialPageIndex(0),
    val settledPageIndex: SettledPageIndex = SettledPageIndex(0),
    val visibleUrl: ImageUrl? = null,
)
