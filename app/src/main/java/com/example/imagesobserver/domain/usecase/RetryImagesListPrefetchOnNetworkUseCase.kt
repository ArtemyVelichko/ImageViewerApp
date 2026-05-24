package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.connectivity.NetworkAvailabilitySource
import com.example.imagesobserver.domain.prefetch.ImagesListPrefetchGate
import javax.inject.Inject

/**
 * When the network becomes available again, re-runs [PrefetchImagesListUseCase] if the last
 * background prefetch failed. Wired from the app layer — not from [data].
 */
class RetryImagesListPrefetchOnNetworkUseCase @Inject constructor(
    private val networkAvailabilitySource: NetworkAvailabilitySource,
    private val prefetchImagesListUseCase: PrefetchImagesListUseCase,
    private val imagesListPrefetchGate: ImagesListPrefetchGate,
) {

    suspend operator fun invoke() {
        networkAvailabilitySource.networkAvailable.collect {
            if (imagesListPrefetchGate.shouldRetryWhenNetworkAvailable()) {
                prefetchImagesListUseCase()
            }
        }
    }
}
