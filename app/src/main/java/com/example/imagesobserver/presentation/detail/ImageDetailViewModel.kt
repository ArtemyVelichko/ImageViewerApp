package com.example.imagesobserver.presentation.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.imagesobserver.data.session.ImageGallerySession
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.sharing.ImageUrlShareGateway
import com.example.imagesobserver.domain.usecase.LoadCachedOriginalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    private val gallerySession: ImageGallerySession,
    private val loadCachedOriginalUseCase: LoadCachedOriginalUseCase,
    private val imageUrlShareGateway: ImageUrlShareGateway,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _urls = MutableStateFlow<List<ImageUrl>>(emptyList())
    val urls: StateFlow<List<ImageUrl>> = _urls.asStateFlow()

    private val _initialPageIndex = MutableStateFlow(0)
    val initialPageIndex: StateFlow<Int> = _initialPageIndex.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    fun load(startIndex: Int) {
        val list = gallerySession.getUrls()
        _urls.value = list
        val page = if (list.isEmpty()) 0 else startIndex.coerceIn(0, list.lastIndex)
        _initialPageIndex.value = page
        _currentPageIndex.value = page
    }

    fun onPageChanged(index: Int) {
        _currentPageIndex.value = index
    }

    suspend fun loadCachedOriginal(imageUrl: ImageUrl): File? =
        loadCachedOriginalUseCase(imageUrl)

    fun shareCurrentImageUrl() {
        currentImageUrl()?.let { url ->
            appContext.startActivity(imageUrlShareGateway.buildShareChooserIntent(url))
        }
    }

    fun openCurrentImageInBrowser() {
        currentImageUrl()?.let { url ->
            appContext.startActivity(imageUrlShareGateway.buildViewInBrowserIntent(url))
        }
    }

    private fun currentImageUrl(): ImageUrl? =
        _urls.value.getOrNull(_currentPageIndex.value)
}
