package com.example.imagesobserver.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Images

@Serializable
data class ImageDetail(val startIndex: Int)
