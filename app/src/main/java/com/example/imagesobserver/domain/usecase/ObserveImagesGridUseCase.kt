package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.connectivity.NetworkAvailabilitySource
import com.example.imagesobserver.domain.model.ManifestGridRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject

/**
 * Offline-first grid stream: initial load, manual [requestRefresh], and automatic retry
 * when the network returns after a failed or empty load.
 */
class ObserveImagesGridUseCase @Inject constructor(
    private val loadImagesGridOfflineFirstUseCase: LoadImagesGridOfflineFirstUseCase,
    private val networkAvailabilitySource: NetworkAvailabilitySource,
) {

    operator fun invoke(): Flow<Result<List<ManifestGridRow>>> = channelFlow {
        var lastResult: Result<List<ManifestGridRow>>? = null

        suspend fun loadAndEmit() {
            val result = loadImagesGridOfflineFirstUseCase()
            lastResult = result
            send(result)
        }

        loadAndEmit()
        networkAvailabilitySource.networkAvailable.collect {
            if (shouldRetryAfterNetworkReturn(lastResult)) {
                loadAndEmit()
            }
        }
    }

    private fun shouldRetryAfterNetworkReturn(last: Result<List<ManifestGridRow>>?): Boolean =
        when {
            last == null -> true
            last.isFailure -> true
            last.getOrNull().isNullOrEmpty() -> true
            else -> false
        }
}
