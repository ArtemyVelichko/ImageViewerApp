package com.example.imagesobserver.data.local.provider

import android.content.Context

/**
 * Access to the application [Context] for Android system services.
 * Prefer this over injecting [android.content.Context] into data-layer types.
 */
fun interface ContextProvider {

    fun applicationContext(): Context
}
