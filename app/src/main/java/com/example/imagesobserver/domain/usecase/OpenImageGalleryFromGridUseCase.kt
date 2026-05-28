package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.model.ManifestGridRow
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import javax.inject.Inject

/** Prepares the detail pager from a grid tap and returns the start page index. */
class OpenImageGalleryFromGridUseCase @Inject constructor(
    private val imageGalleryRepository: ImageGalleryRepository,
) {

    /**
     * @return Pager start index, or null when detail must not open.
     */
    operator fun invoke(
        clicked: ImageUrl,
        items: List<ManifestGridRow>,
    ): Int? {
        val openableUrls = imageGalleryRepository.getOpenableUrls()
        if (clicked.url !in openableUrls) return null
        val brokenUrls = imageGalleryRepository.getBrokenUrls()
        val pagerUrls = items.mapNotNull { row ->
            when (row) {
                is ManifestGridRow.Link -> row.url
                is ManifestGridRow.InvalidLine -> null
            }
        }.filter { it.url !in brokenUrls && it.url in openableUrls }
        if (clicked !in pagerUrls) return null
        imageGalleryRepository.setUrls(pagerUrls)
        return pagerUrls.indexOf(clicked)
    }
}
