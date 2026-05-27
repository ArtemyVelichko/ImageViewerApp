package com.example.imagesobserver.presentation.images.model

import com.example.imagesobserver.domain.model.ManifestGridRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * UI state for the images grid screen.
 * [items] is an [ImmutableList] so Compose treats content as value-based when the list is equal.
 */
data class ImagesUiState(
    val isLoading: Boolean = true,
    val items: ImmutableList<ManifestGridRow> = persistentListOf(),
    val error: String? = null,
    /** LazyVerticalGrid column count from viewport width (see [ImagesViewModel.updateGridLayout]). */
    val gridColumnCount: Int = 1,
)
