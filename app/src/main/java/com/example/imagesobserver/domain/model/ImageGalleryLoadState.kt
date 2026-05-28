package com.example.imagesobserver.domain.model

/** Shared grid/detail image load status (openable thumbnails and permanently broken URLs). */
data class ImageGalleryLoadState(
    val openableUrls: Set<String> = emptySet(),
    val brokenUrls: Set<String> = emptySet(),
)
