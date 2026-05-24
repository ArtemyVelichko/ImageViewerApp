package com.example.imagesobserver.di

import com.example.imagesobserver.data.api.ImagesRetrofit
import com.example.imagesobserver.data.api.NetworkLogEventListenerFactory
import com.example.imagesobserver.data.api.NetworkLogInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        ImagesRetrofit.createHttpLoggingInterceptor()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        networkLogInterceptor: NetworkLogInterceptor,
        httpLoggingInterceptor: HttpLoggingInterceptor,
        networkLogEventListenerFactory: NetworkLogEventListenerFactory,
    ): OkHttpClient =
        ImagesRetrofit.createOkHttpClient(
            networkLogInterceptor,
            httpLoggingInterceptor,
            networkLogEventListenerFactory,
        )

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        ImagesRetrofit.createRetrofit(client)

    @Provides
    @Singleton
    fun provideImagesRetrofit(retrofit: Retrofit): ImagesRetrofit =
        ImagesRetrofit(retrofit)
}
