package com.example.imagesobserver.data.api

import com.example.imagesobserver.constants.ProjectConstants
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logs method, URL, HTTP status, and duration (Timber). Active only when [ProjectConstants.RETROFIT_DEBUG_ENABLED] is true.
 * Failures are logged via [NetworkLogEventListenerFactory]; this interceptor does not catch exceptions from [Interceptor.Chain.proceed].
 */
@Singleton
class NetworkLogInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!ProjectConstants.RETROFIT_DEBUG_ENABLED) {
            return chain.proceed(chain.request())
        }
        val request = chain.request()
        val startNs = System.nanoTime()
        return chain.proceed(request).also { response ->
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
            Timber.tag(TAG).i(
                ProjectConstants.NETWORK_LOG_HTTP_SUCCESS_FORMAT,
                request.method,
                request.url,
                response.code,
                elapsedMs,
            )
        }
    }

    companion object {
        private const val TAG = "OkHttp"
    }
}

@Singleton
class NetworkLogEventListenerFactory @Inject constructor() : EventListener.Factory {

    override fun create(call: Call): EventListener =
        if (ProjectConstants.RETROFIT_DEBUG_ENABLED) {
            NetworkLogEventListener()
        } else {
            EventListener.NONE
        }

    private class NetworkLogEventListener : EventListener() {
        private var startNs: Long = 0L

        override fun callStart(call: Call) {
            startNs = System.nanoTime()
        }

        override fun callFailed(call: Call, ioe: IOException) {
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
            val request = call.request()
            Timber.tag(TAG).e(
                ioe,
                ProjectConstants.NETWORK_LOG_HTTP_FAILED_FORMAT,
                request.method,
                request.url,
                elapsedMs,
            )
        }
    }

    private companion object {
        private const val TAG = "OkHttp"
    }
}
