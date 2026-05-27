package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import javax.inject.Inject

/** Clears in-memory grid thumbnail cache and pager broken URLs when the manifest is refreshed. */
class ResetGridOnManifestRefreshUseCase @Inject constructor(
    private val resolveGridThumbnailUseCase: ResolveGridThumbnailUseCase,
    private val imageGalleryRepository: ImageGalleryRepository,
) {

    operator fun invoke() {
        resolveGridThumbnailUseCase.clearMemoryCache()
        imageGalleryRepository.clearBroken()
    }
}
