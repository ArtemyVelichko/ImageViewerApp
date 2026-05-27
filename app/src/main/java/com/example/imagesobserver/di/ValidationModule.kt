package com.example.imagesobserver.di

import com.example.imagesobserver.data.validation.BitmapDisplayableImageValidator
import com.example.imagesobserver.domain.validation.DisplayableImageValidator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ValidationModule {

    @Binds
    @Singleton
    abstract fun bindDisplayableImageValidator(
        impl: BitmapDisplayableImageValidator,
    ): DisplayableImageValidator
}
