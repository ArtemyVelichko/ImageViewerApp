package com.example.imagesobserver.domain.sharing

import android.content.Intent
import com.example.imagesobserver.domain.model.ImageUrl

/** Builds implicit intents to share or open an image [ImageUrl]. */
interface ImageUrlShareGateway {

    fun buildShareChooserIntent(imageUrl: ImageUrl): Intent

    fun buildViewInBrowserIntent(imageUrl: ImageUrl): Intent
}
