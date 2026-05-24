package com.example.imagesobserver.presentation.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagesobserver.R
import com.example.imagesobserver.data.local.provider.ResourceProvider
import com.example.imagesobserver.data.session.ImageGallerySession
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.model.ManifestGridRow
import com.example.imagesobserver.domain.usecase.LoadGridThumbnailUseCase
import com.example.imagesobserver.domain.usecase.ObserveImagesGridUseCase
import com.example.imagesobserver.presentation.images.model.ImagesUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val observeImagesGridUseCase: ObserveImagesGridUseCase,
    private val loadGridThumbnailUseCase: LoadGridThumbnailUseCase,
    private val resourceProvider: ResourceProvider,
    private val gallerySession: ImageGallerySession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagesUiState())
    val uiState: StateFlow<ImagesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeImagesGridUseCase().collect { applyGridResult(it) }
        }
    }

    /**
     * Recomputes [ImagesUiState.gridColumnCount] from the grid content width in **physical pixels**
     * (after horizontal padding) and horizontal spacing between cells, targeting cell width **100–120 px**.
     * Call when configuration or width changes (e.g. rotation).
     */
    fun updateGridLayout(contentWidthPx: Int, cellSpacingPx: Int) {
        val columns = computeColumnCount(
            contentWidthPx = contentWidthPx,
            cellSpacingPx = cellSpacingPx,
        )
        _uiState.update { prev ->
            if (prev.gridColumnCount == columns) prev else prev.copy(gridColumnCount = columns)
        }
    }

    private fun applyGridResult(result: Result<List<ManifestGridRow>>) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        result
            .onSuccess { items ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = items.toImmutableList(),
                        error = null,
                    )
                }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message?.takeIf { msg -> msg.isNotBlank() }
                            ?: resourceProvider.getString(R.string.error_load_image_urls_default),
                    )
                }
            }
    }

    /** Grid thumbnail from disk cache (cell size in physical pixels). */
    suspend fun loadGridThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File? =
        loadGridThumbnailUseCase(imageUrl, widthPx, heightPx)

    fun prepareImageDetailOpen(clicked: ImageUrl): Int {
        val links = _uiState.value.items.mapNotNull { row ->
            when (row) {
                is ManifestGridRow.Link -> row.url
                is ManifestGridRow.InvalidLine -> null
            }
        }
        gallerySession.setUrls(links)
        return links.indexOf(clicked).takeIf { it >= 0 } ?: 0
    }

    private companion object {
        /** Target cell width band (physical px) for column count. */
        private const val GRID_CELL_MIN_WIDTH_PX = 100
        private const val GRID_CELL_MAX_WIDTH_PX = 120

        /**
         * Maximizes columns with cell width ≥ [GRID_CELL_MIN_WIDTH_PX]; if cells would exceed
         * [GRID_CELL_MAX_WIDTH_PX], adds columns. Narrow viewports may use one column wider than max.
         */
        fun computeColumnCount(contentWidthPx: Int, cellSpacingPx: Int): Int {
            val w = contentWidthPx
            val s = cellSpacingPx.coerceAtLeast(0)
            if (w <= 0) return 1
            var n = ((w + s) / (GRID_CELL_MIN_WIDTH_PX + s)).coerceAtLeast(1)
            fun cellWidthFor(columnCount: Int): Double =
                (w - (columnCount - 1) * s.toDouble()) / columnCount
            while (n < 512 && cellWidthFor(n) > GRID_CELL_MAX_WIDTH_PX) n++
            while (n > 1 && cellWidthFor(n) < GRID_CELL_MIN_WIDTH_PX) n--
            return n.coerceAtLeast(1)
        }
    }
}
