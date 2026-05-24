package com.example.imagesobserver.data.local.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextProviderImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ContextProvider {

    override fun applicationContext(): Context = context.applicationContext
}
