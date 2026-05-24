package com.example.imagesobserver.data.session

import com.example.imagesobserver.domain.model.ImageUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageGallerySessionImpl @Inject constructor() : ImageGallerySession {

    private val lock = Any()
    private var urls: List<ImageUrl> = emptyList()

    override fun setUrls(urls: List<ImageUrl>) {
        synchronized(lock) {
            this.urls = urls.toList()
        }
    }

    override fun getUrls(): List<ImageUrl> = synchronized(lock) {
        urls.toList()
    }
}
