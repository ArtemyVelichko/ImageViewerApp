package com.example.imagesobserver.domain.repository

import com.example.imagesobserver.domain.model.ImageUrl

/** In-memory gallery opened from the grid (shared between grid and detail screens). */
interface ImageGalleryRepository {

    fun setUrls(urls: List<ImageUrl>)

    fun getUrls(): List<ImageUrl>

    /** URLs that failed bitmap validation — excluded from detail pager. */
    fun getBrokenUrls(): Set<String>

    fun markBroken(imageUrl: ImageUrl)

    fun unmarkBroken(imageUrl: ImageUrl)

    fun clearBroken()
}
