package com.example.imagesobserver.presentation.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagesobserver.R
import com.example.imagesobserver.data.gallery.ImageGalleryLoader
import com.example.imagesobserver.data.local.provider.ResourceProvider
import com.example.imagesobserver.domain.model.GridThumbnailResult
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.model.ManifestGridRow
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.domain.usecase.ObserveImagesGridUseCase
import com.example.imagesobserver.domain.usecase.OpenImageGalleryFromGridUseCase
import com.example.imagesobserver.domain.usecase.ResetGridOnManifestRefreshUseCase
import com.example.imagesobserver.domain.usecase.ResolveGridThumbnailUseCase
import com.example.imagesobserver.presentation.images.model.ImagesUiState
import com.example.imagesobserver.presentation.theme.Dimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val observeImagesGridUseCase: ObserveImagesGridUseCase,
    private val resolveGridThumbnailUseCase: ResolveGridThumbnailUseCase,
    private val imageGalleryLoader: ImageGalleryLoader,
    private val resourceProvider: ResourceProvider,
    private val imageGalleryRepository: ImageGalleryRepository,
    private val openImageGalleryFromGridUseCase: OpenImageGalleryFromGridUseCase,
    private val resetGridOnManifestRefreshUseCase: ResetGridOnManifestRefreshUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagesUiState())
    val uiState: StateFlow<ImagesUiState> = _uiState.asStateFlow()

    val openableUrls: StateFlow<PersistentSet<String>> = imageGalleryRepository.loadState
        .map { it.openableUrls.toPersistentSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentSetOf())

    val brokenUrls: StateFlow<PersistentSet<String>> = imageGalleryRepository.loadState
        .map { it.brokenUrls.toPersistentSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentSetOf())

    init {
        viewModelScope.launch {
            observeImagesGridUseCase().collect { result ->
                result.onSuccess { items ->
                    val previousItems = _uiState.value.items
                    if (previousItems.isNotEmpty() && items != previousItems) {
                        onManifestRefreshed()
                    }
                }
                applyGridResult(result)
            }
        }
    }

    fun onGridContentWidthChanged(gridWidthPx: Int) {
        if (gridWidthPx <= 0) return
        val cellSpacingDp = Dimens.imageGridCellSpacing.value
        val contentWidthDp = gridWidthPx / resourceProvider.displayDensity()
        val columns = computeColumnCount(
            contentWidthDp = contentWidthDp,
            cellSpacingDp = cellSpacingDp,
        )
        _uiState.update { prev ->
            if (prev.gridColumnCount == columns) prev else prev.copy(gridColumnCount = columns)
        }
        val spacingPx = resourceProvider.dpToPx(cellSpacingDp)
        val thumbnailHeightPx = resourceProvider.dpToPx(Dimens.imageThumbnailMaxHeight.value)
        val cellWidthPx = (
            (gridWidthPx - (columns - 1) * spacingPx) / columns
            ).coerceAtLeast(1)
        imageGalleryLoader.onGridThumbnailTargetChanged(cellWidthPx, thumbnailHeightPx)
    }

    fun onGridViewportChanged(visibleRowIndices: List<Int>) {
        if (visibleRowIndices.isEmpty()) return
        imageGalleryLoader.onVisibleRowsChanged(
            visibleRowIndices.min(),
            visibleRowIndices.max(),
        )
    }

    private fun applyGridResult(result: Result<List<ManifestGridRow>>) {
        if (_uiState.value.items.isEmpty()) {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }
        result
            .onSuccess { items ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = items.toImmutableList(),
                        error = null,
                    )
                }
                imageGalleryLoader.onManifestRowsChanged(items)
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

    suspend fun loadGridThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File? =
        imageGalleryLoader.loadThumbnail(imageUrl, widthPx, heightPx)

    fun peekGridThumbnail(
        imageUrl: ImageUrl,
        widthPx: Int,
        heightPx: Int,
    ): GridThumbnailResult? = resolveGridThumbnailUseCase.peek(imageUrl, widthPx, heightPx)

    fun onRetryGridThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int) {
        imageGalleryLoader.retry(imageUrl, widthPx, heightPx)
    }

    fun openDetailFromGrid(clicked: ImageUrl): Int? =
        openImageGalleryFromGridUseCase(
            clicked = clicked,
            items = _uiState.value.items,
        )

    private fun onManifestRefreshed() {
        imageGalleryLoader.cancelAll()
        resetGridOnManifestRefreshUseCase()
    }

    private companion object {
        fun computeColumnCount(contentWidthDp: Float, cellSpacingDp: Float): Int {
            val w = contentWidthDp
            val s = cellSpacingDp.coerceAtLeast(0f)
            val minW = Dimens.imageGridCellMinWidth.value
            val maxW = Dimens.imageGridCellMaxWidth.value
            if (w <= 0f) return 1
            var n = ((w + s) / (minW + s)).toInt().coerceAtLeast(1)
            fun cellWidthFor(columnCount: Int): Float =
                (w - (columnCount - 1) * s) / columnCount
            while (n < 512 && cellWidthFor(n) > maxW) n++
            while (n > 1 && cellWidthFor(n) < minW) n--
            return n.coerceAtLeast(1)
        }
    }
}
