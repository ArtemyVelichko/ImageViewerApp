package com.example.imagesobserver.data.repository

import com.example.imagesobserver.domain.model.ImageGalleryLoadState
import com.example.imagesobserver.domain.model.ImageGalleryUrlStatus
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class ImageGalleryRepositoryImpl @Inject constructor() : ImageGalleryRepository {

    private val lock = Any()
    private var manifestLinks: List<ImageUrl> = emptyList()
    private val _loadState = MutableStateFlow(ImageGalleryLoadState())

    override val loadState: StateFlow<ImageGalleryLoadState> = _loadState.asStateFlow()

    override fun setManifestLinks(links: List<ImageUrl>) {
        synchronized(lock) {
            manifestLinks = links.toList()
        }
    }

    override fun getManifestLinks(): List<ImageUrl> = synchronized(lock) {
        manifestLinks.toList()
    }

    override fun getOpenableManifestLinks(): List<ImageUrl> {
        val loadState = _loadState.value
        return getManifestLinks().filter { loadState.isOpenable(it.url) }
    }

    override fun getUrlStatus(imageUrl: ImageUrl): ImageGalleryUrlStatus =
        _loadState.value.status(imageUrl.url)

    override fun setUrlStatus(imageUrl: ImageUrl, status: ImageGalleryUrlStatus) {
        _loadState.update { state ->
            if (state.urlStatuses[imageUrl.url] == status) state
            else state.copy(urlStatuses = state.urlStatuses + (imageUrl.url to status))
        }
    }

    override fun markLoading(imageUrl: ImageUrl) = setUrlStatus(imageUrl, ImageGalleryUrlStatus.Loading)

    override fun markOpenable(imageUrl: ImageUrl) = setUrlStatus(imageUrl, ImageGalleryUrlStatus.Openable)

    override fun markBroken(imageUrl: ImageUrl) = setUrlStatus(imageUrl, ImageGalleryUrlStatus.Broken)

    override fun clearLoadState() {
        synchronized(lock) {
            manifestLinks = emptyList()
        }
        _loadState.value = ImageGalleryLoadState()
    }
}
