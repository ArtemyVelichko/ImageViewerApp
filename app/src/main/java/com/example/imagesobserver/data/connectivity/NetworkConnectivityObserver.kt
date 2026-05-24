package com.example.imagesobserver.data.connectivity

import android.net.ConnectivityManager
import android.net.Network
import com.example.imagesobserver.data.local.provider.ContextProvider
import com.example.imagesobserver.domain.connectivity.NetworkAvailabilitySource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Android [ConnectivityManager] → [NetworkAvailabilitySource]. Emits only; no domain use cases here. */
@Singleton
class NetworkConnectivityObserver @Inject constructor(
    private val contextProvider: ContextProvider,
) : NetworkAvailabilitySource {

    private val _networkAvailable = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val networkAvailable: SharedFlow<Unit> = _networkAvailable.asSharedFlow()

    private val connectivityManager: ConnectivityManager
        get() = requireNotNull(
            contextProvider.applicationContext().getSystemService(ConnectivityManager::class.java),
        )

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _networkAvailable.tryEmit(Unit)
        }
    }

    fun register() {
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.e(e, "Failed to unregister network connectivity callback")
        }
    }
}
