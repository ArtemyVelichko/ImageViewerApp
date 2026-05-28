package com.example.imagesobserver.domain.model

/**
 * Shared grid/detail image load status.
 * Missing URLs are treated as [ImageGalleryUrlStatus.Loading].
 */
data class ImageGalleryLoadState(
    val urlStatuses: Map<String, ImageGalleryUrlStatus> = emptyMap(),
) {

    fun status(url: String): ImageGalleryUrlStatus = urlStatuses[url] ?: ImageGalleryUrlStatus.Loading

    fun isOpenable(url: String): Boolean = status(url) == ImageGalleryUrlStatus.Openable

    fun isBroken(url: String): Boolean = status(url) == ImageGalleryUrlStatus.Broken

    fun isLoading(url: String): Boolean = status(url) == ImageGalleryUrlStatus.Loading
}
