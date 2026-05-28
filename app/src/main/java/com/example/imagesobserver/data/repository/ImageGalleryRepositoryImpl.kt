package com.example.imagesobserver.data.repository

import com.example.imagesobserver.domain.model.ImageGalleryLoadState
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
    private var urls: List<ImageUrl> = emptyList()
    private val _loadState = MutableStateFlow(ImageGalleryLoadState())

    override val loadState: StateFlow<ImageGalleryLoadState> = _loadState.asStateFlow()

    override fun setUrls(urls: List<ImageUrl>) {
        synchronized(lock) {
            this.urls = urls.toList()
        }
    }

    override fun getUrls(): List<ImageUrl> = synchronized(lock) {
        urls.toList()
    }

    override fun getOpenableUrls(): Set<String> = _loadState.value.openableUrls

    override fun getBrokenUrls(): Set<String> = _loadState.value.brokenUrls

    override fun markOpenable(imageUrl: ImageUrl) {
        _loadState.update { state ->
            if (imageUrl.url in state.openableUrls) state
            else state.copy(openableUrls = state.openableUrls + imageUrl.url)
        }
    }

    override fun unmarkOpenable(imageUrl: ImageUrl) {
        _loadState.update { state ->
            if (imageUrl.url !in state.openableUrls) state
            else state.copy(openableUrls = state.openableUrls - imageUrl.url)
        }
    }

    override fun markBroken(imageUrl: ImageUrl) {
        synchronized(lock) {
            if (imageUrl.url in _loadState.value.brokenUrls) return
            urls = urls.filter { it.url != imageUrl.url }
        }
        _loadState.update { state ->
            state.copy(
                brokenUrls = state.brokenUrls + imageUrl.url,
                openableUrls = state.openableUrls - imageUrl.url,
            )
        }
    }

    override fun unmarkBroken(imageUrl: ImageUrl) {
        _loadState.update { state ->
            state.copy(brokenUrls = state.brokenUrls - imageUrl.url)
        }
    }

    override fun clearLoadState() {
        _loadState.value = ImageGalleryLoadState()
    }
}
