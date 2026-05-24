package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.repository.ImagesListRepository
import com.example.imagesobserver.domain.error.AppErrorFactory
import com.example.imagesobserver.domain.model.ManifestGridRow
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

/**
 * Offline-first grid load:
 * 1. Read and parse disk cache (local source of truth when offline).
 * 2. Best-effort refresh from network; update cache on success.
 * 3. Return fresh remote rows when available, otherwise cached rows.
 * 4. Fail only when both cache and remote are unavailable.
 */
class LoadImagesGridOfflineFirstUseCase @Inject constructor(
    private val imagesListRepository: ImagesListRepository,
    private val parseManifestLinesToGridRowsUseCase: ParseManifestLinesToGridRowsUseCase,
    private val appErrorFactory: AppErrorFactory,
) {

    suspend operator fun invoke(): Result<List<ManifestGridRow>> {
        val cachedRows = try {
            readCachedRows()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.e(e, "Failed to read cached images manifest")
            return Result.failure(e)
        }

        val refreshedRows = refreshFromRemote()

        return when {
            refreshedRows != null -> Result.success(refreshedRows)
            cachedRows != null -> Result.success(cachedRows)
            else -> Result.failure(appErrorFactory.noImagesListData())
        }
    }

    private suspend fun readCachedRows(): List<ManifestGridRow>? =
        imagesListRepository.readCachedList()
            ?.takeIf { it.isNotBlank() }
            ?.let { text -> parseManifestLinesToGridRowsUseCase(text) }
            ?.takeIf { it.isNotEmpty() }

    /**
     * Fetches remote list, parses it, persists only after a non-empty parse.
     * Returns fresh rows even when cache write fails (session still gets new data).
     */
    private suspend fun refreshFromRemote(): List<ManifestGridRow>? {
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
