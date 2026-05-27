package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.model.ManifestGridRow
import com.example.imagesobserver.domain.repository.ImagesListRepository
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

/** Downloads the images manifest from the network, parses it, and persists it to disk. */
class RefreshImagesGridFromRemoteUseCase @Inject constructor(
    private val imagesListRepository: ImagesListRepository,
    private val parseManifestLinesToGridRowsUseCase: ParseManifestLinesToGridRowsUseCase,
) {

    suspend operator fun invoke(): List<ManifestGridRow>? {
        return try {
            val remoteList = imagesListRepository.fetchRemoteList()
            if (remoteList.isBlank()) return null

            val rows = parseManifestLinesToGridRowsUseCase(remoteList)
            if (rows.isEmpty()) return null

            persistRemoteListBestEffort(remoteList)
            rows
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.e(e, "Remote images manifest refresh failed")
            null
        }
    }

    private suspend fun persistRemoteListBestEffort(remoteList: String) {
        try {
            imagesListRepository.writeCachedList(remoteList)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.e(e, "Failed to persist remote images manifest to cache")
        }
    }
}
