package com.example.imagesobserver.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagesobserver.domain.model.ImageGalleryLoadState
import com.example.imagesobserver.domain.model.ImageGalleryUrlStatus
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.model.detailPagerUrls
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.domain.sharing.ImageUrlShareGateway
import com.example.imagesobserver.domain.usecase.LoadCachedOriginalUseCase
import com.example.imagesobserver.domain.usecase.LoadDetailPageUseCase
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
    private val loadCachedOriginalUseCase: LoadCachedOriginalUseCase,
    private val loadDetailPageUseCase: LoadDetailPageUseCase,
    private val imageUrlShareGateway: ImageUrlShareGateway,
) : ViewModel() {

    private val _pagerState = MutableStateFlow(DetailPagerState())
    val pagerState: StateFlow<DetailPagerState> = _pagerState.asStateFlow()

    private var detailSessionStarted = false

    init {
        viewModelScope.launch {
            imageGalleryRepository.loadState.collect(::applyLoadState)
        }
    }

    fun load(manifestStartIndex: Int) {
        detailSessionStarted = true
        val manifest = imageGalleryRepository.getManifestLinks()
        val loadState = imageGalleryRepository.loadState.value
        val pagerUrls = manifest.detailPagerUrls(loadState)
        val startUrl = manifest.getOrNull(manifestStartIndex)
        val pageIndex = startUrl?.let { url ->
            pagerUrls.indexOfFirst { it.url == url.url }.takeIf { it >= 0 }
        } ?: pageIndexFor(pagerUrls, manifestStartIndex)
        setPagerState(
            urls = pagerUrls,
            urlStatuses = loadState.urlStatuses,
            initialPageIndex = InitialPageIndex(pageIndex),
            settledPageIndex = SettledPageIndex(pageIndex),
            visibleUrl = pagerUrls.getOrNull(pageIndex),
        )
    }

    fun onPageSettled(pageIndex: Int) {
        val list = _pagerState.value.urls
        if (list.isEmpty()) return
        val page = pageIndex.coerceIn(0, list.lastIndex)
        val url = list[page]
        if (imageGalleryRepository.getUrlStatus(url) == ImageGalleryUrlStatus.Broken) return
        setPagerState(
            settledPageIndex = SettledPageIndex(page),
            visibleUrl = url,
        )
        if (imageGalleryRepository.getUrlStatus(url) == ImageGalleryUrlStatus.Loading) {
            loadPage(url)
        }
    }

    fun loadPage(imageUrl: ImageUrl) {
        if (imageGalleryRepository.getUrlStatus(imageUrl) == ImageGalleryUrlStatus.Broken) return
        if (_pagerState.value.urls.none { it.url == imageUrl.url }) return
        viewModelScope.launch {
            loadDetailPageUseCase(imageUrl)
        }
    }

    suspend fun loadCachedOriginal(imageUrl: ImageUrl): File? {
        if (imageGalleryRepository.getUrlStatus(imageUrl) != ImageGalleryUrlStatus.Openable) return null
        return loadCachedOriginalUseCase(imageUrl)
    }

    fun shareCurrentImageUrl() {
        _pagerState.value.visibleUrl?.let(imageUrlShareGateway::shareImage)
    }

    fun openCurrentImageInBrowser() {
        _pagerState.value.visibleUrl?.let(imageUrlShareGateway::openImageInBrowser)
    }

    private fun applyLoadState(loadState: ImageGalleryLoadState) {
        _pagerState.update { current ->
            if (!detailSessionStarted && current.urls.isEmpty()) {
                return@update current.copy(urlStatuses = loadState.urlStatuses)
            }

            val manifest = imageGalleryRepository.getManifestLinks()
            if (manifest.isEmpty()) {
                return@update current.copy(urlStatuses = loadState.urlStatuses)
            }

            val nextUrls = manifest.detailPagerUrls(loadState)
            if (nextUrls.isEmpty()) {
                return@update current.copy(
                    urlStatuses = loadState.urlStatuses,
                    urls = emptyList(),
                    visibleUrl = null,
                )
            }

            val anchor = current.visibleUrl?.takeUnless { loadState.isBroken(it.url) }
            val pageIndex = when {
                anchor != null -> nextUrls.indexOfFirst { it.url == anchor.url }.takeIf { it >= 0 }
                else -> null
            } ?: current.settledPageIndex.index.coerceIn(0, nextUrls.lastIndex)

            current.copy(
                urlStatuses = loadState.urlStatuses,
                urls = nextUrls,
                settledPageIndex = SettledPageIndex(pageIndex),
                visibleUrl = nextUrls.getOrNull(pageIndex),
            )
        }
    }

    private fun setPagerState(
        urls: List<ImageUrl>? = null,
        urlStatuses: Map<String, ImageGalleryUrlStatus>? = null,
        initialPageIndex: InitialPageIndex? = null,
        settledPageIndex: SettledPageIndex? = null,
        visibleUrl: ImageUrl? = null,
    ) {
        _pagerState.update { current ->
            val loadState = urlStatuses?.let(::ImageGalleryLoadState)
                ?: ImageGalleryLoadState(current.urlStatuses)
            val nextUrls = (urls ?: current.urls).detailPagerUrls(loadState)
            val maxIndex = nextUrls.lastIndex.coerceAtLeast(0)
            val nextSettled = settledPageIndex
                ?: current.settledPageIndex.coerceIn(0, maxIndex)
            val nextVisible = when {
                nextUrls.isEmpty() -> null
                visibleUrl != null && nextUrls.any { it.url == visibleUrl.url } -> visibleUrl
                else -> nextUrls.getOrNull(nextSettled.index)
            }
            current.copy(
                urls = nextUrls,
                urlStatuses = loadState.urlStatuses,
                initialPageIndex = initialPageIndex ?: current.initialPageIndex,
                settledPageIndex = if (nextUrls.isEmpty()) SettledPageIndex(0) else nextSettled,
                visibleUrl = nextVisible,
            )
        }
    }

    private fun pageIndexFor(urls: List<ImageUrl>, index: Int): Int =
        if (urls.isEmpty()) 0 else index.coerceIn(0, urls.lastIndex)
}
