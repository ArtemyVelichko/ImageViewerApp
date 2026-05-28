package com.example.imagesobserver.presentation.detail

import androidx.lifecycle.ViewModel
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.sharing.ImageUrlShareGateway
import com.example.imagesobserver.domain.usecase.LoadCachedOriginalUseCase
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

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    private val imageGalleryRepository: ImageGalleryRepository,
    private val loadCachedOriginalUseCase: LoadCachedOriginalUseCase,
    private val imageUrlShareGateway: ImageUrlShareGateway,
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
        loadCachedOriginalUseCase(imageUrl)

    fun shareCurrentImageUrl() {
        _pagerState.value.visibleUrl?.let(imageUrlShareGateway::shareImage)
    }

    fun openCurrentImageInBrowser() {
        _pagerState.value.visibleUrl?.let(imageUrlShareGateway::openImageInBrowser)
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

    private fun pageIndexFor(urls: List<ImageUrl>, index: Int): Int =
        if (urls.isEmpty()) 0 else index.coerceIn(0, urls.lastIndex)

    private fun visibleUrlFor(urls: List<ImageUrl>, index: Int): ImageUrl? =
        urls.getOrNull(index)
}
