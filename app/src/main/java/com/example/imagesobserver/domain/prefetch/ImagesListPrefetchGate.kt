package com.example.imagesobserver.domain.prefetch

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Tracks whether the last background images-list prefetch failed (retry when network returns). */
@Singleton
class ImagesListPrefetchGate @Inject constructor() {

    private val _manifestCached = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val manifestCached: SharedFlow<Unit> = _manifestCached.asSharedFlow()

    @Volatile
    private var lastPrefetchFailed: Boolean = false

    fun reportOutcome(success: Boolean) {
        lastPrefetchFailed = !success
        if (success) {
            _manifestCached.tryEmit(Unit)
        }
    }

    fun shouldRetryWhenNetworkAvailable(): Boolean = lastPrefetchFailed
}
