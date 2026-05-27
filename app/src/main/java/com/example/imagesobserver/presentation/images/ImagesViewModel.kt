package com.example.imagesobserver.presentation.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagesobserver.R
import com.example.imagesobserver.data.local.provider.ResourceProvider
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.domain.model.DisplayableImageResult
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.model.ManifestGridRow
import com.example.imagesobserver.domain.usecase.LoadGridThumbnailUseCase
import com.example.imagesobserver.domain.usecase.ObserveImagesGridUseCase
import com.example.imagesobserver.domain.usecase.OpenImageGalleryFromGridUseCase
import com.example.imagesobserver.domain.validation.DisplayableImageValidator
import com.example.imagesobserver.domain.validation.resolveDisplayable
import com.example.imagesobserver.presentation.images.model.ImagesUiState
import com.example.imagesobserver.presentation.theme.Dimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.persistentSetOf
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
    private val displayableImageValidator: DisplayableImageValidator,
    private val resourceProvider: ResourceProvider,
    private val imageGalleryRepository: ImageGalleryRepository,
    private val openImageGalleryFromGridUseCase: OpenImageGalleryFromGridUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagesUiState())
    val uiState: StateFlow<ImagesUiState> = _uiState.asStateFlow()

    private val _openableUrls = MutableStateFlow<PersistentSet<String>>(persistentSetOf())
    val openableUrls: StateFlow<PersistentSet<String>> = _openableUrls.asStateFlow()

    private val _brokenUrls = MutableStateFlow<PersistentSet<String>>(persistentSetOf())
    val brokenUrls: StateFlow<PersistentSet<String>> = _brokenUrls.asStateFlow()

    init {
        viewModelScope.launch {
            observeImagesGridUseCase().collect { result ->
                result.onSuccess { resetGridThumbnailState() }
                applyGridResult(result)
            }
        }
    }

    fun updateGridLayout(contentWidthDp: Float, cellSpacingDp: Float) {
        val columns = computeColumnCount(
            contentWidthDp = contentWidthDp,
            cellSpacingDp = cellSpacingDp,
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

    /** Loads grid thumbnail; updates [openableUrls]. Network errors stay retryable (not broken). */
    suspend fun loadGridThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File? {
        val file = loadGridThumbnailUseCase(imageUrl, widthPx, heightPx)
        if (file == null) {
            setOpenable(imageUrl, false)
            return null
        }
        return when (val result = displayableImageValidator.resolveDisplayable(file)) {
            is DisplayableImageResult.Displayable -> {
                setOpenable(imageUrl, true)
                result.file
            }
            DisplayableImageResult.NotDisplayable -> {
                setOpenable(imageUrl, false)
                imageGalleryRepository.markBroken(imageUrl)
                addBroken(imageUrl)
                null
            }
        }
    }

    fun syncBrokenUrlsFromRepository() {
        val synced = imageGalleryRepository.getBrokenUrls()
        val next = persistentSetOf<String>().builder().apply { addAll(synced) }.build()
        if (next != _brokenUrls.value) {
            _brokenUrls.value = next
        }
    }

    fun onRetryGridThumbnail(imageUrl: ImageUrl) {
        imageGalleryRepository.unmarkBroken(imageUrl)
        removeBroken(imageUrl)
        setOpenable(imageUrl, false)
    }

    fun openDetailFromGrid(clicked: ImageUrl): Int? =
        openImageGalleryFromGridUseCase(
            clicked = clicked,
            items = _uiState.value.items,
            openableUrls = _openableUrls.value,
        )

    /** Clears pager broken list and grid open/broken UI state when manifest is refreshed. */
    private fun resetGridThumbnailState() {
        imageGalleryRepository.clearBroken()
        _openableUrls.value = persistentSetOf()
        _brokenUrls.value = persistentSetOf()
    }

    private fun addBroken(imageUrl: ImageUrl) {
        val next = _brokenUrls.value.builder().apply { add(imageUrl.url) }.build()
        if (next != _brokenUrls.value) {
            _brokenUrls.value = next
        }
    }

    private fun removeBroken(imageUrl: ImageUrl) {
        val next = _brokenUrls.value.builder().apply { remove(imageUrl.url) }.build()
        if (next != _brokenUrls.value) {
            _brokenUrls.value = next
        }
    }

    private fun setOpenable(imageUrl: ImageUrl, openable: Boolean) {
        val next = _openableUrls.value.builder().apply {
            if (openable) add(imageUrl.url) else remove(imageUrl.url)
        }.build()
        if (next != _openableUrls.value) {
            _openableUrls.value = next
        }
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
