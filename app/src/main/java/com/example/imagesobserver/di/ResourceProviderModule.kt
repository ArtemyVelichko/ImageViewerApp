package com.example.imagesobserver.di

import com.example.imagesobserver.data.local.provider.AppExceptionMessages
import com.example.imagesobserver.data.local.provider.ContextProvider
import com.example.imagesobserver.data.local.provider.ContextProviderImpl
import com.example.imagesobserver.data.local.provider.ResourceProvider
import com.example.imagesobserver.data.local.provider.ResourceProviderImpl
import com.example.imagesobserver.domain.error.AppErrorFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ResourceProviderModule {

    @Binds
    @Singleton
    abstract fun bindContextProvider(impl: ContextProviderImpl): ContextProvider

    @Binds
    @Singleton
    abstract fun bindResourceProvider(impl: ResourceProviderImpl): ResourceProvider

    @Binds
    @Singleton
    abstract fun bindAppErrorFactory(impl: AppExceptionMessages): AppErrorFactory
}
