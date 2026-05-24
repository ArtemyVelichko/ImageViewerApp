package com.example.imagesobserver.domain.connectivity

import kotlinx.coroutines.flow.SharedFlow

/** Emits when the device gains a default network (implementation in data layer). */
interface NetworkAvailabilitySource {
    val networkAvailable: SharedFlow<Unit>
}
