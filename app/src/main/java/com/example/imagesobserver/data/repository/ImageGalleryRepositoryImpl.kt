package com.example.imagesobserver.data.repository

import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageGalleryRepositoryImpl @Inject constructor() : ImageGalleryRepository {

    private val lock = Any()
    private var urls: List<ImageUrl> = emptyList()
    private var brokenUrls: Set<String> = emptySet()

    override fun setUrls(urls: List<ImageUrl>) {
        synchronized(lock) {
            this.urls = urls.toList()
        }
    }

    override fun getUrls(): List<ImageUrl> = synchronized(lock) {
        urls.toList()
    }

    override fun getBrokenUrls(): Set<String> = synchronized(lock) {
        brokenUrls.toSet()
    }

    override fun markBroken(imageUrl: ImageUrl) {
        synchronized(lock) {
            if (imageUrl.url in brokenUrls) return
            brokenUrls = brokenUrls + imageUrl.url
            urls = urls.filter { it.url != imageUrl.url }
        }
    }

    override fun unmarkBroken(imageUrl: ImageUrl) {
        synchronized(lock) {
            brokenUrls = brokenUrls - imageUrl.url
        }
    }

    override fun clearBroken() {
        synchronized(lock) {
            brokenUrls = emptySet()
        }
    }
}
