package com.example.imagesobserver.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagesobserver.domain.model.ImageGalleryLoadState
import com.example.imagesobserver.domain.model.ImageGalleryUrlStatus
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.domain.sharing.ImageUrlShareGateway
import com.example.imagesobserver.domain.usecase.LoadCachedOriginalFileUseCase
import com.example.imagesobserver.presentation.detail.model.DetailPagerState
import com.example.imagesobserver.presentation.detail.model.InitialPageIndex
import com.example.imagesobserver.presentation.detail.model.SettledPageIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    private val imageGalleryRepository: ImageGalleryRepository,
    private val loadCachedOriginalFileUseCase: LoadCachedOriginalFileUseCase,
    private val imageUrlShareGateway: ImageUrlShareGateway,
) : ViewModel() {

    private val _pagerState = MutableStateFlow(DetailPagerState())
    val pagerState: StateFlow<DetailPagerState> = _pagerState.asStateFlow()

    private var detailSessionStarted = false
    private var manifestStartIndex: Int = 0

    init {
        viewModelScope.launch {
            imageGalleryRepository.loadState.collect(::applyLoadState)
        }
    }

    fun load(manifestStartIndex: Int) {
        detailSessionStarted = true
        this.manifestStartIndex = manifestStartIndex
        applyLoadState(imageGalleryRepository.loadState.value, isInitialLoad = true)
    }

    fun onPageSettled(pageIndex: Int) {
        val pages = _pagerState.value.pages
        if (pages.isEmpty()) return
        val settled = pageIndex.coerceIn(0, pages.lastIndex)
        _pagerState.update {
            it.copy(
                settledPageIndex = SettledPageIndex(settled),
                visibleUrl = pages[settled],
            )
        }
    }

    suspend fun loadCachedOriginal(imageUrl: ImageUrl): File? {
        if (imageGalleryRepository.getUrlStatus(imageUrl) != ImageGalleryUrlStatus.Openable) return null
        return loadCachedOriginalFileUseCase(imageUrl)
    }

    fun shareCurrentImageUrl() {
        _pagerState.value.visibleUrl?.let(imageUrlShareGateway::shareImage)
    }

    fun openCurrentImageInBrowser() {
        _pagerState.value.visibleUrl?.let(imageUrlShareGateway::openImageInBrowser)
    }

    private fun applyLoadState(loadState: ImageGalleryLoadState, isInitialLoad: Boolean = false) {
        _pagerState.update { current ->
            if (!detailSessionStarted && current.pages.isEmpty()) return@update current

            val openablePages = imageGalleryRepository.getOpenableManifestLinks()
            if (openablePages.isEmpty()) {
                return@update current.copy(pages = emptyList(), visibleUrl = null)
            }

            val pageIndex = when {
                isInitialLoad -> resolveInitialPageIndex(openablePages)
                else -> {
                    val anchor = current.visibleUrl?.takeUnless { loadState.isBroken(it.url) }
                    when {
                        anchor != null -> openablePages.indexOfFirst { it.url == anchor.url }.takeIf { it >= 0 }
                        else -> null
                    } ?: current.settledPageIndex.index.coerceIn(0, openablePages.lastIndex)
                }
            }

            current.copy(
                pages = openablePages,
                initialPageIndex = if (isInitialLoad) InitialPageIndex(pageIndex) else current.initialPageIndex,
                settledPageIndex = SettledPageIndex(pageIndex),
                visibleUrl = openablePages.getOrNull(pageIndex),
            )
        }
    }

    private fun resolveInitialPageIndex(openablePages: List<ImageUrl>): Int {
        val manifest = imageGalleryRepository.getManifestLinks()
        val startUrl = manifest.getOrNull(manifestStartIndex) ?: return 0
        return openablePages.indexOfFirst { it.url == startUrl.url }.takeIf { it >= 0 }
            ?: pageIndexFor(openablePages.size, manifestStartIndex)
    }

    private fun pageIndexFor(pageCount: Int, index: Int): Int =
        if (pageCount == 0) 0 else index.coerceIn(0, pageCount - 1)
}
