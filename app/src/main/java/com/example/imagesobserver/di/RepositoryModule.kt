package com.example.imagesobserver.di

import com.example.imagesobserver.data.connectivity.NetworkConnectivityObserver
import com.example.imagesobserver.data.repository.ImageGalleryRepositoryImpl
import com.example.imagesobserver.data.repository.ImagesListRepositoryImpl
import com.example.imagesobserver.data.repository.OriginalImageCacheRepositoryImpl
import com.example.imagesobserver.data.repository.ThumbnailImageCacheRepositoryImpl
import com.example.imagesobserver.domain.connectivity.NetworkAvailabilitySource
import com.example.imagesobserver.domain.repository.ImageGalleryRepository
import com.example.imagesobserver.domain.repository.ImagesListRepository
import com.example.imagesobserver.domain.repository.OriginalImageCacheRepository
import com.example.imagesobserver.domain.repository.ThumbnailImageCacheRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindImagesListRepository(impl: ImagesListRepositoryImpl): ImagesListRepository

    @Binds
    @Singleton
    abstract fun bindOriginalImageCacheRepository(
        impl: OriginalImageCacheRepositoryImpl,
    ): OriginalImageCacheRepository

    @Binds
    @Singleton
    abstract fun bindThumbnailImageCacheRepository(
        impl: ThumbnailImageCacheRepositoryImpl,
    ): ThumbnailImageCacheRepository

    @Binds
    @Singleton
    abstract fun bindImageGalleryRepository(impl: ImageGalleryRepositoryImpl): ImageGalleryRepository

    @Binds
    @Singleton
    abstract fun bindNetworkAvailabilitySource(
        impl: NetworkConnectivityObserver,
    ): NetworkAvailabilitySource
}
