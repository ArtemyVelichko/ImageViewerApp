package com.example.imagesobserver.domain.prefetch

import javax.inject.Inject
import javax.inject.Singleton

/** Tracks whether the last background images-list prefetch failed (retry when network returns). */
@Singleton
class ImagesListPrefetchGate @Inject constructor() {

    @Volatile
    private var lastPrefetchFailed: Boolean = false

    fun reportOutcome(success: Boolean) {
        lastPrefetchFailed = !success
    }

    fun shouldRetryWhenNetworkAvailable(): Boolean = lastPrefetchFailed
}
