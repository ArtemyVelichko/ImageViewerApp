package com.example.imagesobserver.di

import com.example.imagesobserver.data.sharing.ImageUrlShareGatewayImpl
import com.example.imagesobserver.domain.sharing.ImageUrlShareGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SharingModule {

    @Binds
    @Singleton
    abstract fun bindImageUrlShareGateway(
        impl: ImageUrlShareGatewayImpl,
    ): ImageUrlShareGateway
}
