package com.example.imagesobserver.domain.model

/** Per-URL thumbnail/original load status shared by grid and detail. */
sealed interface ImageGalleryUrlStatus {

    data object Loading : ImageGalleryUrlStatus

    data object Openable : ImageGalleryUrlStatus

    data object Broken : ImageGalleryUrlStatus
}
