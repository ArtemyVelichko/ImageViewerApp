package com.example.imagesobserver

import android.app.Application
import android.content.pm.ApplicationInfo
import com.example.imagesobserver.data.connectivity.NetworkConnectivityObserver
import com.example.imagesobserver.di.IoDispatcher
import com.example.imagesobserver.domain.usecase.PrefetchImagesListUseCase
import com.example.imagesobserver.domain.usecase.RetryImagesListPrefetchOnNetworkUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ImagesObserverApplication : Application() {

    @Inject
    lateinit var prefetchImagesListUseCase: PrefetchImagesListUseCase

    @Inject
    lateinit var retryImagesListPrefetchOnNetworkUseCase: RetryImagesListPrefetchOnNetworkUseCase

    @Inject
    lateinit var networkConnectivityObserver: NetworkConnectivityObserver

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val applicationJob = SupervisorJob()
    private lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        val debuggable =
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) {
            Timber.plant(Timber.DebugTree())
        }
        applicationScope = CoroutineScope(applicationJob + ioDispatcher)
        networkConnectivityObserver.register()
        applicationScope.launch {
            prefetchImagesListUseCase()
        }
        applicationScope.launch {
            retryImagesListPrefetchOnNetworkUseCase()
        }
    }

    override fun onTerminate() {
        networkConnectivityObserver.unregister()
        applicationScope.cancel()
        super.onTerminate()
    }
}
