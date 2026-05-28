package com.example.imagesobserver.presentation.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.io.File

/** Full-screen detail image from disk cache only; reload is handled on the grid, not here. */
@Composable
fun DetailFullscreenCoilImage(
    imageUrl: ImageUrl,
    loadCachedOriginal: suspend (ImageUrl) -> File?,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier.fillMaxSize(),
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    var cachedFile by remember(imageUrl) { mutableStateOf<File?>(null) }

    LaunchedEffect(imageUrl, loadCachedOriginal) {
        cachedFile = loadCachedOriginal(imageUrl)
    }

    Box(modifier = modifier) {
        val file = cachedFile
        if (file == null) {
            DetailImagePlaceholder(
                placeholder = placeholder,
                modifier = imageModifier,
                contentScale = contentScale,
            )
            return@Box
        }

        val request = remember(file) {
            ImageRequest.Builder(context)
                .data(file)
                .build()
        }
        SubcomposeAsyncImage(
            model = request,
            contentDescription = stringResource(R.string.content_description_image),
            modifier = imageModifier,
            loading = {
                DetailImagePlaceholder(
                    placeholder = placeholder,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            },
            error = {
                DetailImagePlaceholder(
                    placeholder = placeholder,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
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
private fun DetailImagePlaceholder(
    placeholder: ColorPainter,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Image(
        painter = placeholder,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
    )
}
