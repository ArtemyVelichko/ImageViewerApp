package com.example.imagesobserver.domain.sharing

import com.example.imagesobserver.domain.model.ImageUrl

/** Builds and launches implicit intents to share or open an image [ImageUrl]. */
interface ImageUrlShareGateway {

    fun shareImage(imageUrl: ImageUrl)

    fun openImageInBrowser(imageUrl: ImageUrl)
}
