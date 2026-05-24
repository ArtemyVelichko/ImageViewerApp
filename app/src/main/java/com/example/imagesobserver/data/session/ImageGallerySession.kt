package com.example.imagesobserver.data.session

import com.example.imagesobserver.domain.model.ImageUrl

/** In-memory gallery opened from the grid (shared between grid and detail screens). */
interface ImageGallerySession {

    fun setUrls(urls: List<ImageUrl>)

    fun getUrls(): List<ImageUrl>
}
