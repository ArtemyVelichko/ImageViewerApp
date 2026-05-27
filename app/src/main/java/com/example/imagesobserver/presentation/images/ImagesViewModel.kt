package com.example.imagesobserver.presentation.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagesobserver.R
import com.example.imagesobserver.data.local.provider.ResourceProvider
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.domain.model.GridThumbnailResult
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.model.ManifestGridRow
import com.example.imagesobserver.domain.usecase.ObserveImagesGridUseCase
import com.example.imagesobserver.domain.usecase.OpenImageGalleryFromGridUseCase
import com.example.imagesobserver.domain.usecase.ResetGridOnManifestRefreshUseCase
import com.example.imagesobserver.domain.usecase.ResolveGridThumbnailUseCase
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
    private val resolveGridThumbnailUseCase: ResolveGridThumbnailUseCase,
    private val resourceProvider: ResourceProvider,
    private val imageGalleryRepository: ImageGalleryRepository,
    private val openImageGalleryFromGridUseCase: OpenImageGalleryFromGridUseCase,
    private val resetGridOnManifestRefreshUseCase: ResetGridOnManifestRefreshUseCase,
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
                result.onSuccess { onManifestRefreshed() }
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
    suspend fun loadGridThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File? =
        when (val result = resolveGridThumbnailUseCase(imageUrl, widthPx, heightPx)) {
            is GridThumbnailResult.Displayable -> {
                markOpenable(imageUrl)
                result.file
            }
            GridThumbnailResult.LoadFailed -> {
                unmarkOpenable(imageUrl)
                null
            }
            GridThumbnailResult.Invalid -> {
                unmarkOpenable(imageUrl)
                imageGalleryRepository.markBroken(imageUrl)
                addBroken(imageUrl)
                null
            }
        }

    fun peekGridThumbnail(
        imageUrl: ImageUrl,
        widthPx: Int,
        heightPx: Int,
    ): GridThumbnailResult? = resolveGridThumbnailUseCase.peek(imageUrl, widthPx, heightPx)

    fun syncBrokenUrlsFromRepository() {
        val synced = imageGalleryRepository.getBrokenUrls()
        val next = persistentSetOf<String>().builder().apply { addAll(synced) }.build()
        if (next != _brokenUrls.value) {
            _brokenUrls.value = next
        }
    }

    fun onRetryGridThumbnail(imageUrl: ImageUrl) {
        resolveGridThumbnailUseCase.evict(imageUrl)
        imageGalleryRepository.unmarkBroken(imageUrl)
        removeBroken(imageUrl)
        unmarkOpenable(imageUrl)
    }

    fun openDetailFromGrid(clicked: ImageUrl): Int? =
        openImageGalleryFromGridUseCase(
            clicked = clicked,
            items = _uiState.value.items,
            openableUrls = _openableUrls.value,
        )

    private fun onManifestRefreshed() {
        resetGridOnManifestRefreshUseCase()
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

    private fun markOpenable(imageUrl: ImageUrl) {
        _openableUrls.update { current ->
            if (imageUrl.url in current) current
            else current.builder().apply { add(imageUrl.url) }.build()
        }
    }

    private fun unmarkOpenable(imageUrl: ImageUrl) {
        _openableUrls.update { current ->
            if (imageUrl.url !in current) current
            else current.builder().apply { remove(imageUrl.url) }.build()
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
