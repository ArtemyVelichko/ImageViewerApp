package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.data.gallery.ImageGalleryLoader
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import javax.inject.Inject

/** Cancels prefetch, clears loader session, thumbnail memory cache, and shared load state on manifest refresh. */
class ResetGridOnManifestRefreshUseCase @Inject constructor(
    private val imageGalleryLoader: ImageGalleryLoader,
    private val resolveGridThumbnailUseCase: ResolveGridThumbnailUseCase,
    private val imageGalleryRepository: ImageGalleryRepository,
) {

    operator fun invoke() {
        imageGalleryLoader.cancelAll()
        resolveGridThumbnailUseCase.clearMemoryCache()
        imageGalleryRepository.clearLoadState()
    }
}
