package com.example.imagesobserver.presentation.images.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.imagesobserver.R
import com.example.imagesobserver.domain.model.GridThumbnailResult
import com.example.imagesobserver.domain.model.ImageGalleryUrlStatus
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.presentation.theme.Dimens
import java.io.File

/**
 * Grid cell image for [com.example.imagesobserver.presentation.images.ImagesScreen] only.
 * [ImageGalleryUrlStatus.Broken] shows placeholder; loading/openable show thumbnail flow.
 */
@Composable
fun GridThumbnailCoilImage(
    imageUrl: ImageUrl,
    loadGridThumbnail: suspend (ImageUrl, Int, Int) -> File?,
    peekGridThumbnail: (ImageUrl, Int, Int) -> GridThumbnailResult?,
    urlStatus: ImageGalleryUrlStatus,
    onRetryGridThumbnail: (ImageUrl, Int, Int) -> Unit,
    onClick: (() -> Unit)?,
    targetWidthPx: Int,
    targetHeightPx: Int,
    modifier: Modifier = Modifier,
) {
    if (urlStatus == ImageGalleryUrlStatus.Broken) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .alpha(0.72f),
        ) {
            ImageGridPlaceholderCell(modifier = Modifier.fillMaxSize())
        }
        return
    }

    val context = LocalContext.current
    val cachedEntry = remember(imageUrl, targetWidthPx, targetHeightPx) {
        peekGridThumbnail(imageUrl, targetWidthPx, targetHeightPx)
    }
    var thumbnailFile by remember(imageUrl, targetWidthPx, targetHeightPx) {
        mutableStateOf(
            (cachedEntry as? GridThumbnailResult.Displayable)?.file,
        )
    }
    var resolved by remember(imageUrl, targetWidthPx, targetHeightPx) {
        mutableStateOf(cachedEntry != null)
    }
    var reloadAttempt by remember(imageUrl, targetWidthPx, targetHeightPx) { mutableIntStateOf(0) }

    suspend fun loadThumbnail() {
        resolved = false
        thumbnailFile = loadGridThumbnail(imageUrl, targetWidthPx, targetHeightPx)
        resolved = true
    }

    LaunchedEffect(imageUrl, targetWidthPx, targetHeightPx, loadGridThumbnail, reloadAttempt) {
        if (reloadAttempt == 0 && cachedEntry is GridThumbnailResult.Displayable) return@LaunchedEffect
        loadThumbnail()
    }

    val onRetry: () -> Unit = {
        onRetryGridThumbnail(imageUrl, targetWidthPx, targetHeightPx)
        reloadAttempt++
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(if (onClick != null) 1f else 0.72f)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
    ) {
        when {
            !resolved -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            thumbnailFile != null -> {
                val file = thumbnailFile!!
                val fromMemoryCache = reloadAttempt == 0 && cachedEntry is GridThumbnailResult.Displayable
                var coilDecodeFailed by remember(file, reloadAttempt) { mutableStateOf(false) }
                val request = remember(file, reloadAttempt, fromMemoryCache) {
                    ImageRequest.Builder(context)
                        .data(file)
                        .crossfade(!fromMemoryCache)
                        .build()
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    SubcomposeAsyncImage(
                        model = request,
                        contentDescription = stringResource(R.string.content_description_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { coilDecodeFailed = false },
                        onError = { coilDecodeFailed = true },
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (fromMemoryCache) {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                    ),
                            )
                        },
                        success = {
                            SubcomposeAsyncImageContent(modifier = Modifier.fillMaxSize())
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                        },
                    )
                    if (coilDecodeFailed) {
                        ImageGridRetryCell(
                            onRetry = onRetry,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            else -> {
                ImageGridRetryCell(
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun ImageGridRetryCell(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        FilledTonalButton(
            onClick = onRetry,
            modifier = Modifier
                .padding(horizontal = Dimens.imageGridRetryContentPadding)
                .fillMaxWidth()
                .height(Dimens.imageGridRetryButtonMinHeight),
        ) {
            Text(
                text = stringResource(R.string.retry),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
