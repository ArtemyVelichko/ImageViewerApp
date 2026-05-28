package com.example.imagesobserver.data.sharing

import android.content.Intent
import com.example.imagesobserver.R
import com.example.imagesobserver.constants.ProjectConstants
import com.example.imagesobserver.data.local.provider.ContextProvider
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.sharing.ImageUrlShareGateway
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUrlShareGatewayImpl @Inject constructor(
    private val contextProvider: ContextProvider,
) : ImageUrlShareGateway {

    override fun shareImage(imageUrl: ImageUrl) {
        contextProvider.applicationContext().startActivity(buildShareChooserIntent(imageUrl))
    }

    override fun openImageInBrowser(imageUrl: ImageUrl) {
        contextProvider.applicationContext().startActivity(buildViewInBrowserIntent(imageUrl))
    }

    private fun buildShareChooserIntent(imageUrl: ImageUrl): Intent {
        val context = contextProvider.applicationContext()
        val url = imageUrl.url
        val contentUri = ImageUrlShareContentProvider.registerUrl(context, url)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = ProjectConstants.MIME_TEXT_PLAIN
            putExtra(Intent.EXTRA_TEXT, url)
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri(null, contentUri)
        }

        return Intent.createChooser(
            sendIntent,
            context.getString(R.string.image_share_chooser_title),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun buildViewInBrowserIntent(imageUrl: ImageUrl): Intent =
        Intent(Intent.ACTION_VIEW, android.net.Uri.parse(imageUrl.url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
