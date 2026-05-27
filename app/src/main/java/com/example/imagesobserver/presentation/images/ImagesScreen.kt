package com.example.imagesobserver.presentation.images

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imagesobserver.R
import com.example.imagesobserver.domain.model.GridThumbnailResult
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.model.ManifestGridRow
import com.example.imagesobserver.presentation.images.components.ImageGridPlaceholderCell
import com.example.imagesobserver.presentation.images.components.ImageThumbnailCell
import com.example.imagesobserver.presentation.images.model.ImagesUiState
import com.example.imagesobserver.presentation.theme.Dimens
import com.example.imagesobserver.presentation.theme.ImagesObserverTheme
import java.io.File
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
fun ImagesScreen(
    onOpenImageDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImagesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val openableUrls by viewModel.openableUrls.collectAsStateWithLifecycle()
    val brokenUrls by viewModel.brokenUrls.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.syncBrokenUrlsFromRepository()
        onPauseOrDispose { }
    }

    ImagesScreenContent(
        state = state,
        openableUrls = openableUrls,
        brokenUrls = brokenUrls,
        modifier = modifier,
        onOpenImageDetail = onOpenImageDetail,
        loadGridThumbnail = viewModel::loadGridThumbnail,
        peekGridThumbnail = viewModel::peekGridThumbnail,
        onGridLayoutChanged = viewModel::updateGridLayout,
        openDetailFromGrid = viewModel::openDetailFromGrid,
        onRetryGridThumbnail = viewModel::onRetryGridThumbnail,
    )
}

@Composable
fun ImagesScreenContent(
    state: ImagesUiState,
    openableUrls: PersistentSet<String>,
    brokenUrls: PersistentSet<String>,
    onOpenImageDetail: (Int) -> Unit,
    loadGridThumbnail: suspend (ImageUrl, Int, Int) -> File?,
    peekGridThumbnail: (ImageUrl, Int, Int) -> GridThumbnailResult?,
    onGridLayoutChanged: (contentWidthDp: Float, cellSpacingDp: Float) -> Unit,
    openDetailFromGrid: (ImageUrl) -> Int?,
    onRetryGridThumbnail: (ImageUrl) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.error != null && state.items.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(Dimens.imagesListErrorScreenPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.imagesListErrorItemSpacing),
                ) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.images_list_will_retry_when_online),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            state.isLoading && state.items.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            else -> {
                val density = LocalDensity.current
                val spacingDp = Dimens.imageGridCellSpacing.value
                var gridWidthPx by remember { mutableIntStateOf(0) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.imageGridHorizontalMargin)
                        .onSizeChanged { gridWidthPx = it.width },
                ) {
                    LaunchedEffect(gridWidthPx, spacingDp, density) {
                        if (gridWidthPx > 0) {
                            val contentWidthDp = with(density) { gridWidthPx.toDp().value }
                            onGridLayoutChanged(contentWidthDp, spacingDp)
                        }
                    }
                    val columns = state.gridColumnCount.coerceAtLeast(1)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = Dimens.imageGridContentPaddingVertical),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.imageGridCellSpacing),
                        verticalArrangement = Arrangement.spacedBy(Dimens.imageGridCellSpacing),
                    ) {
                        itemsIndexed(
                            items = state.items,
                            key = { index, row ->
                                when (row) {
                                    is ManifestGridRow.Link -> "link_$index"
                                    is ManifestGridRow.InvalidLine -> "invalid_$index"
                                }
                            },
                        ) { _, row ->
                            when (row) {
                                is ManifestGridRow.Link ->
                                    ImageThumbnailCell(
                                        imageUrl = row.url,
                                        loadGridThumbnail = loadGridThumbnail,
                                        peekGridThumbnail = peekGridThumbnail,
                                        isPermanentlyBroken = row.url.url in brokenUrls,
                                        onRetryGridThumbnail = onRetryGridThumbnail,
                                        onClick = if (row.url.url in openableUrls) {
                                            {
                                                openDetailFromGrid(row.url)?.let(onOpenImageDetail)
                                            }
                                        } else {
                                            null
                                        },
                                    )

                                is ManifestGridRow.InvalidLine ->
                                    ImageGridPlaceholderCell()
                            }
                        }
                    }
                }
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(Dimens.imageGridLoadingIndicatorPadding),
                    )
                }
            }
        }
    }
}

private val previewOnRetryGridThumbnail: (ImageUrl) -> Unit = {}

private val previewLoadGridThumbnail: suspend (ImageUrl, Int, Int) -> File? = { _, _, _ -> null }

private val previewPeekGridThumbnail: (ImageUrl, Int, Int) -> GridThumbnailResult? =
    { _, _, _ -> null }

/** Preview stub: grid clicks do not open detail. */
private val previewOpenDetailFromGrid: (ImageUrl) -> Int? = { null }

private val previewGridItems = persistentListOf(
    ManifestGridRow.Link(ImageUrl("https://it-link.ru/images/1.jpg")),
    ManifestGridRow.Link(ImageUrl("https://it-link.ru/images/2.jpg")),
    ManifestGridRow.InvalidLine("not-a-url"),
    ManifestGridRow.Link(ImageUrl("https://it-link.ru/images/3.jpg")),
)

@Preview(name = "Images grid", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun ImagesScreenGridPreview() {
    ImagesObserverTheme(dynamicColor = false) {
        ImagesScreenContent(
            state = ImagesUiState(
                items = previewGridItems,
                gridColumnCount = 3,
                isLoading = false,
            ),
            openableUrls = persistentSetOf("https://it-link.ru/images/1.jpg"),
            brokenUrls = persistentSetOf(),
            onOpenImageDetail = {},
            loadGridThumbnail = previewLoadGridThumbnail,
            peekGridThumbnail = previewPeekGridThumbnail,
            onGridLayoutChanged = { _, _ -> },
            openDetailFromGrid = previewOpenDetailFromGrid,
            onRetryGridThumbnail = previewOnRetryGridThumbnail,
        )
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun ImagesScreenLoadingPreview() {
    ImagesObserverTheme(dynamicColor = false) {
        ImagesScreenContent(
            state = ImagesUiState(isLoading = true),
            openableUrls = persistentSetOf(),
            brokenUrls = persistentSetOf(),
            onOpenImageDetail = {},
            loadGridThumbnail = previewLoadGridThumbnail,
            peekGridThumbnail = previewPeekGridThumbnail,
            onGridLayoutChanged = { _, _ -> },
            openDetailFromGrid = previewOpenDetailFromGrid,
            onRetryGridThumbnail = previewOnRetryGridThumbnail,
        )
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun ImagesScreenErrorPreview() {
    ImagesObserverTheme(dynamicColor = false) {
        ImagesScreenContent(
            state = ImagesUiState(
                isLoading = false,
                error = "Images list is missing in cache and remote refresh failed",
            ),
            openableUrls = persistentSetOf(),
            brokenUrls = persistentSetOf(),
            onOpenImageDetail = {},
            loadGridThumbnail = previewLoadGridThumbnail,
            peekGridThumbnail = previewPeekGridThumbnail,
            onGridLayoutChanged = { _, _ -> },
            openDetailFromGrid = previewOpenDetailFromGrid,
            onRetryGridThumbnail = previewOnRetryGridThumbnail,
        )
    }
}
