package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.data.gallery.ImageGalleryLoader
import com.example.imagesobserver.domain.model.ImageUrl
import javax.inject.Inject

/** Loads grid thumbnail for a detail pager page (promotes URL to openable when successful). */
class LoadDetailPageUseCase @Inject constructor(
    private val imageGalleryLoader: ImageGalleryLoader,
) {

    suspend operator fun invoke(imageUrl: ImageUrl) {
        val (widthPx, heightPx) = imageGalleryLoader.getGridThumbnailTargetPx() ?: return
        imageGalleryLoader.loadThumbnail(imageUrl, widthPx, heightPx)
    }
}
