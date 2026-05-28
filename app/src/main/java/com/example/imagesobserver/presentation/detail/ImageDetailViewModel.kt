package com.example.imagesobserver.presentation.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.imagesobserver.domain.model.DisplayableImageResult
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.domain.sharing.ImageUrlShareGateway
import com.example.imagesobserver.domain.usecase.LoadCachedOriginalUseCase
import com.example.imagesobserver.domain.validation.DisplayableImageValidator
import com.example.imagesobserver.domain.validation.resolveDisplayable
import com.example.imagesobserver.presentation.detail.model.DetailPagerState
import com.example.imagesobserver.presentation.detail.model.InitialPageIndex
import com.example.imagesobserver.presentation.detail.model.SettledPageIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    private val imageGalleryRepository: ImageGalleryRepository,
    private val loadCachedOriginalUseCase: LoadCachedOriginalUseCase,
    private val displayableImageValidator: DisplayableImageValidator,
    private val imageUrlShareGateway: ImageUrlShareGateway,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _pagerState = MutableStateFlow(DetailPagerState())
    val pagerState: StateFlow<DetailPagerState> = _pagerState.asStateFlow()

    fun load(startIndex: Int) {
        val list = imageGalleryRepository.getUrls()
        val pageIndex = pageIndexFor(list, startIndex)
        setPagerState(
            urls = list,
            initialPageIndex = InitialPageIndex(pageIndex),
            settledPageIndex = SettledPageIndex(pageIndex),
            visibleUrl = visibleUrlFor(list, pageIndex),
        )
    }

    fun onPageSettled(pageIndex: Int) {
        val list = _pagerState.value.urls
        if (list.isEmpty()) return
        val page = pageIndex.coerceIn(0, list.lastIndex)
        setPagerState(
            settledPageIndex = SettledPageIndex(page),
            visibleUrl = list[page],
        )
    }

    suspend fun loadCachedOriginal(imageUrl: ImageUrl): File? =
        when (
            val result = displayableImageValidator.resolveDisplayable(
                loadCachedOriginalUseCase(imageUrl),
            )
        ) {
            is DisplayableImageResult.Displayable -> result.file
            DisplayableImageResult.NotDisplayable -> {
                imageGalleryRepository.markBroken(imageUrl)
                applyUrlsFromRepository()
                null
            }
        }

    fun shareCurrentImageUrl() {
        _pagerState.value.visibleUrl?.let { url ->
            appContext.startActivity(imageUrlShareGateway.buildShareChooserIntent(url))
        }
    }

    fun openCurrentImageInBrowser() {
        _pagerState.value.visibleUrl?.let { url ->
            appContext.startActivity(imageUrlShareGateway.buildViewInBrowserIntent(url))
        }
    }

    private fun applyUrlsFromRepository() {
        val list = imageGalleryRepository.getUrls()
        if (list == _pagerState.value.urls) return
        val anchor = _pagerState.value.visibleUrl
        val settled = resolveSettledPageIndex(list, anchor, _pagerState.value.settledPageIndex)
        setPagerState(
            urls = list,
            settledPageIndex = settled,
            visibleUrl = list.getOrNull(settled.index),
        )
    }

    private fun setPagerState(
        urls: List<ImageUrl>? = null,
        initialPageIndex: InitialPageIndex? = null,
        settledPageIndex: SettledPageIndex? = null,
        visibleUrl: ImageUrl? = null,
    ) {
        _pagerState.update { current ->
            val nextUrls = urls ?: current.urls
            val maxIndex = nextUrls.lastIndex.coerceAtLeast(0)
            val nextSettled = settledPageIndex
                ?: current.settledPageIndex.coerceIn(0, maxIndex)
            current.copy(
                urls = nextUrls,
                initialPageIndex = initialPageIndex ?: current.initialPageIndex,
                settledPageIndex = if (nextUrls.isEmpty()) SettledPageIndex(0) else nextSettled,
                visibleUrl = when {
                    nextUrls.isEmpty() -> null
                    visibleUrl != null -> visibleUrl
                    else -> nextUrls.getOrNull(nextSettled.index)
                },
            )
        }
    }

    private fun resolveSettledPageIndex(
        urls: List<ImageUrl>,
        anchor: ImageUrl?,
        fallbackIndex: SettledPageIndex,
    ): SettledPageIndex {
        if (urls.isEmpty()) return SettledPageIndex(0)
        anchor?.let { url ->
            val index = urls.indexOfFirst { it.url == url.url }
            if (index >= 0) return SettledPageIndex(index)
        }
        return fallbackIndex.coerceIn(0, urls.lastIndex)
    }

    private fun pageIndexFor(urls: List<ImageUrl>, index: Int): Int =
        if (urls.isEmpty()) 0 else index.coerceIn(0, urls.lastIndex)

    private fun visibleUrlFor(urls: List<ImageUrl>, index: Int): ImageUrl? =
        urls.getOrNull(index)
}
