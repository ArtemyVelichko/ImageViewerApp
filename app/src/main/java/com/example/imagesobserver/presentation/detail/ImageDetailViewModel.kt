package com.example.imagesobserver.presentation.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.domain.model.DisplayableImageResult
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.sharing.ImageUrlShareGateway
import com.example.imagesobserver.domain.usecase.LoadCachedOriginalUseCase
import com.example.imagesobserver.domain.validation.DisplayableImageValidator
import com.example.imagesobserver.domain.validation.resolveDisplayable
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    private val imageGalleryRepository: ImageGalleryRepository,
    private val loadCachedOriginalUseCase: LoadCachedOriginalUseCase,
    private val displayableImageValidator: DisplayableImageValidator,
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
        val list = imageGalleryRepository.getUrls()
        _urls.value = list
        val page = if (list.isEmpty()) 0 else startIndex.coerceIn(0, list.lastIndex)
        _initialPageIndex.value = page
        _currentPageIndex.value = page
    }

    fun onPageChanged(index: Int) {
        _currentPageIndex.value = index
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
                syncUrlsFromRepository()
                null
            }
        }

    private fun syncUrlsFromRepository() {
        val list = imageGalleryRepository.getUrls()
        _urls.value = list
        if (list.isEmpty()) {
            _currentPageIndex.value = 0
            _initialPageIndex.value = 0
            return
        }
        val page = _currentPageIndex.value.coerceIn(0, list.lastIndex)
        _currentPageIndex.value = page
        _initialPageIndex.value = page
    }

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
