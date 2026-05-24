package com.example.imagesobserver.presentation.images.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.imagesobserver.R
import com.example.imagesobserver.domain.model.ImageUrl
import java.io.File

/**
 * Grid cell image for [com.example.imagesobserver.presentation.images.ImagesScreen] only.
 * Shows a cached thumbnail file or a neutral «—» placeholder — never loads the remote URL here,
 * so there is no Coil error UI and no «Load failed» / Retry on the grid.
 */
@Composable
fun GridThumbnailCoilImage(
    imageUrl: ImageUrl,
    loadGridThumbnail: suspend (ImageUrl, Int, Int) -> File?,
    onOpenDetail: () -> Unit,
    targetWidthPx: Int,
    targetHeightPx: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var thumbnailFile by remember(imageUrl, targetWidthPx, targetHeightPx) { mutableStateOf<File?>(null) }
    var resolved by remember(imageUrl, targetWidthPx, targetHeightPx) { mutableStateOf(false) }

    LaunchedEffect(imageUrl, targetWidthPx, targetHeightPx, loadGridThumbnail) {
        resolved = false
        thumbnailFile = loadGridThumbnail(imageUrl, targetWidthPx, targetHeightPx)
        resolved = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onOpenDetail),
    ) {
        when {
            thumbnailFile != null -> {
                val file = thumbnailFile!!
                val request = remember(file) {
                    ImageRequest.Builder(context)
                        .data(file)
                        .crossfade(true)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = stringResource(R.string.content_description_image),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        ImageGridPlaceholderCell(modifier = Modifier.fillMaxSize())
                    },
                    success = {
                        SubcomposeAsyncImageContent(modifier = Modifier.fillMaxSize())
                    },
                )
            }

            !resolved -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            else -> {
                ImageGridPlaceholderCell(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
