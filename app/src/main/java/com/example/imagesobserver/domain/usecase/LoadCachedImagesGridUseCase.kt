package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.model.ManifestGridRow
import com.example.imagesobserver.domain.repository.ImagesListRepository
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

/** Reads and parses the cached images manifest from private disk storage. */
class LoadCachedImagesGridUseCase @Inject constructor(
    private val imagesListRepository: ImagesListRepository,
    private val parseManifestLinesToGridRowsUseCase: ParseManifestLinesToGridRowsUseCase,
) {

    suspend operator fun invoke(): List<ManifestGridRow>? = try {
        imagesListRepository.readCachedList()
            ?.takeIf { it.isNotBlank() }
            ?.let { text -> parseManifestLinesToGridRowsUseCase(text) }
            ?.takeIf { it.isNotEmpty() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.printStackTrace()
        Timber.e(e, "Failed to read cached images manifest")
        throw e
    }
}
