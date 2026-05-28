package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.connectivity.NetworkAvailabilitySource
import com.example.imagesobserver.domain.error.AppErrorFactory
import com.example.imagesobserver.domain.model.ManifestGridRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject

/**
 * Offline-first grid stream: emit disk cache immediately, refresh from network in background,
 * and retry when the network returns after a failed or empty load.
 */
class ObserveImagesGridUseCase @Inject constructor(
    private val loadCachedManifestRowsUseCase: LoadCachedManifestRowsUseCase,
    private val refreshImagesGridFromRemoteUseCase: RefreshImagesGridFromRemoteUseCase,
    private val networkAvailabilitySource: NetworkAvailabilitySource,
    private val appErrorFactory: AppErrorFactory,
) {

    operator fun invoke(): Flow<Result<List<ManifestGridRow>>> = channelFlow {
        var lastResult: Result<List<ManifestGridRow>>? = null

        suspend fun loadAndEmit() {
            val cachedRows = try {
                loadCachedManifestRowsUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val result = Result.failure<List<ManifestGridRow>>(e)
                lastResult = result
                send(result)
                return
            }

            if (cachedRows != null) {
                val result = Result.success(cachedRows)
                lastResult = result
                send(result)
            }

            val refreshedRows = try {
                refreshImagesGridFromRemoteUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }

            when {
                refreshedRows != null && refreshedRows != cachedRows -> {
                    val result = Result.success(refreshedRows)
                    lastResult = result
                    send(result)
                }
                cachedRows == null && refreshedRows == null -> {
                    val result = Result.failure<List<ManifestGridRow>>(
                        appErrorFactory.noImagesListData(),
                    )
                    lastResult = result
                    send(result)
                }
            }
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
