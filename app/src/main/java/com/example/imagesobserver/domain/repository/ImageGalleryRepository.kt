package com.example.imagesobserver.domain.repository

import com.example.imagesobserver.domain.model.ImageGalleryLoadState
import com.example.imagesobserver.domain.model.ImageGalleryUrlStatus
import com.example.imagesobserver.domain.model.ImageUrl
import kotlinx.coroutines.flow.StateFlow

/** In-memory gallery opened from the grid (shared between grid and detail screens). */
interface ImageGalleryRepository {

    /** Full manifest link order for the detail pager (fixed for the session). */
    fun setManifestLinks(links: List<ImageUrl>)

    fun getManifestLinks(): List<ImageUrl>

    val loadState: StateFlow<ImageGalleryLoadState>

    fun getUrlStatus(imageUrl: ImageUrl): ImageGalleryUrlStatus

    fun setUrlStatus(imageUrl: ImageUrl, status: ImageGalleryUrlStatus)

    fun markLoading(imageUrl: ImageUrl)

    fun markOpenable(imageUrl: ImageUrl)

    fun markBroken(imageUrl: ImageUrl)

    fun clearLoadState()
}
