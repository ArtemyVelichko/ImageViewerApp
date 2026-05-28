package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.model.ImageGalleryUrlStatus
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import javax.inject.Inject

/** Validates a grid tap and returns the start page index in manifest order. */
class OpenImageGalleryFromGridUseCase @Inject constructor(
    private val imageGalleryRepository: ImageGalleryRepository,
) {

    /**
     * @return Pager start index in manifest order, or null when detail must not open.
     */
    operator fun invoke(clicked: ImageUrl): Int? {
        if (imageGalleryRepository.getUrlStatus(clicked) != ImageGalleryUrlStatus.Openable) return null
        val links = imageGalleryRepository.getManifestLinks()
        val startIndex = links.indexOfFirst { it.url == clicked.url }
        if (startIndex < 0) return null
        return startIndex
    }
}
