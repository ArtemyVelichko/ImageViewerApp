package com.example.imagesobserver.domain.repository

import com.example.imagesobserver.domain.model.ImageGalleryLoadState
import com.example.imagesobserver.domain.model.ImageUrl
import kotlinx.coroutines.flow.StateFlow

/** In-memory gallery opened from the grid (shared between grid and detail screens). */
interface ImageGalleryRepository {

    fun setUrls(urls: List<ImageUrl>)

    fun getUrls(): List<ImageUrl>

    val loadState: StateFlow<ImageGalleryLoadState>

    fun getOpenableUrls(): Set<String>

    fun getBrokenUrls(): Set<String>

    fun markOpenable(imageUrl: ImageUrl)

    fun unmarkOpenable(imageUrl: ImageUrl)

    /** URLs that failed bitmap validation — excluded from detail pager. */
    fun markBroken(imageUrl: ImageUrl)

    fun unmarkBroken(imageUrl: ImageUrl)

    fun clearLoadState()
}
