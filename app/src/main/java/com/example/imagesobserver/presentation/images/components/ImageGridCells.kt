package com.example.imagesobserver.presentation.images.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.example.imagesobserver.domain.model.ImageGalleryUrlStatus
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.presentation.images.model.GridThumbnailCallbacks
import com.example.imagesobserver.presentation.theme.Dimens

/** Stub cell for invalid manifest lines and for valid URLs that are not displayable as images. */
@Composable
fun ImageGridPlaceholderCell(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.imageThumbnailMaxHeight)
            .clip(RoundedCornerShape(Dimens.imageThumbnailCorner))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
    }
}

/** Thumbnail slot while the image is downloading or Coil is decoding the cached file. */
@Composable
fun ImageGridLoadingCell(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.circularProgressIndicatorSmall),
            strokeWidth = Dimens.circularProgressIndicatorStroke,
        )
    }
}

@Composable
fun ImageThumbnailCell(
    imageUrl: ImageUrl,
    callbacks: GridThumbnailCallbacks,
    urlStatus: ImageGalleryUrlStatus,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var cellSize by remember { mutableStateOf(IntSize(1, 1)) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.imageThumbnailMaxHeight)
            .clip(RoundedCornerShape(Dimens.imageThumbnailCorner))
            .onSizeChanged { cellSize = it },
    ) {
        val wPx = cellSize.width.coerceAtLeast(1)
        val hPx = cellSize.height.coerceAtLeast(1)
        GridThumbnailCoilImage(
            imageUrl = imageUrl,
            callbacks = callbacks,
            targetWidthPx = wPx,
            targetHeightPx = hPx,
            urlStatus = urlStatus,
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
