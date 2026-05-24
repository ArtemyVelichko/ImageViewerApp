package com.example.imagesobserver.data.api

import com.example.imagesobserver.constants.ProjectConstants
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrofit wrapper for the images list API.
 */
@Singleton
class ImagesRetrofit @Inject constructor(
    retrofit: Retrofit,
) {

    val imagesManifestApi: ImagesManifestApiService by lazy {
        retrofit.create(ImagesManifestApiService::class.java)
    }

    val imageDownloadApi: ImageDownloadApiService by lazy {
        retrofit.create(ImageDownloadApiService::class.java)
    }

    companion object {
        const val BASE_URL = "https://it-link.ru/test/"

        private const val TIMEOUT_SEC = 10L

        fun createHttpLoggingInterceptor(): HttpLoggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = if (ProjectConstants.RETROFIT_DEBUG_ENABLED) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

        fun createOkHttpClient(
            networkLogInterceptor: NetworkLogInterceptor,
            httpLoggingInterceptor: HttpLoggingInterceptor,
            networkLogEventListenerFactory: EventListener.Factory,
        ): OkHttpClient =
            OkHttpClient.Builder()
                .eventListenerFactory(networkLogEventListenerFactory)
                .addInterceptor(networkLogInterceptor)
                .addInterceptor(httpLoggingInterceptor)
                .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .build()

        fun createRetrofit(client: OkHttpClient): Retrofit =
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
    }
}
