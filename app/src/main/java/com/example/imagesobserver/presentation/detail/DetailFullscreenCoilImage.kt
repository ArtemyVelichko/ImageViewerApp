package com.example.imagesobserver.presentation.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.imagesobserver.R
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.presentation.theme.Dimens
import java.io.File

/** Full-screen detail image; reload is handled on the grid, not here. */
@Composable
fun DetailFullscreenCoilImage(
    imageUrl: ImageUrl,
    loadCachedOriginal: suspend (ImageUrl) -> File?,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier.fillMaxSize(),
    contentScale: ContentScale = ContentScale.Fit,
) {
    val request = rememberDetailCoilImageRequest(
        imageUrl = imageUrl,
        loadCachedOriginal = loadCachedOriginal,
    )

    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)

    Box(modifier = modifier) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = stringResource(R.string.content_description_image),
            modifier = imageModifier,
            loading = {
                Image(
                    painter = placeholder,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            },
            error = {
                ImageLoadFailedContent(modifier = Modifier.fillMaxSize())
            },
            success = {
                SubcomposeAsyncImageContent(
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            },
        )
    }
}

@Composable
private fun rememberDetailCoilImageRequest(
    imageUrl: ImageUrl,
    loadCachedOriginal: suspend (ImageUrl) -> File?,
): ImageRequest {
    val context = LocalContext.current
    var dataSource by remember(imageUrl) { mutableStateOf<Any>(imageUrl.url) }

    LaunchedEffect(imageUrl, loadCachedOriginal) {
        val cachedFile = loadCachedOriginal(imageUrl)
        if (cachedFile != null) {
            dataSource = cachedFile
        }
    }

    return remember(dataSource) {
        ImageRequest.Builder(context)
            .data(dataSource)
            .crossfade(true)
            .build()
    }
}

@Composable
private fun ImageLoadFailedContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.image_load_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Dimens.imagesListErrorScreenPadding),
        )
    }
}
