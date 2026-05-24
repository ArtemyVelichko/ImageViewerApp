package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.repository.ImagesListRepository
import com.example.imagesobserver.domain.prefetch.ImagesListPrefetchGate
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

/** Background download of [images.txt] into private disk cache after app start. */
class PrefetchImagesListUseCase @Inject constructor(
    private val imagesListRepository: ImagesListRepository,
    private val imagesListPrefetchGate: ImagesListPrefetchGate,
) {

    suspend operator fun invoke(): Result<Unit> = try {
        val list = imagesListRepository.fetchRemoteList()
        imagesListRepository.writeCachedList(list)
        imagesListPrefetchGate.reportOutcome(success = true)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.printStackTrace()
        Timber.e(e, "Prefetch remote images list failed")
        imagesListPrefetchGate.reportOutcome(success = false)
        Result.failure(e)
    }
}
