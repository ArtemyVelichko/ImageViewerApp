package com.example.imagesobserver.presentation.images

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imagesobserver.R
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.presentation.detail.ImageDetailViewModel
import com.example.imagesobserver.presentation.detail.model.DetailPagerState
import com.example.imagesobserver.presentation.systemui.ImmersiveSystemUi
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ImageDetailScreen(
    startIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageDetailViewModel = hiltViewModel(),
) {
    var isChromeVisible by remember { mutableStateOf(true) }

    BackHandler {
        if (!isChromeVisible) {
            isChromeVisible = true
        } else {
            onBack()
        }
    }

    LaunchedEffect(startIndex) {
        viewModel.load(startIndex)
    }

    val pagerState by viewModel.pagerState.collectAsStateWithLifecycle()

    ImmersiveSystemUi(systemBarsVisible = isChromeVisible)

    val toggleChrome = { isChromeVisible = !isChromeVisible }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (isChromeVisible) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.image_detail_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text(stringResource(R.string.image_detail_back))
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.shareCurrentImageUrl() },
                            enabled = pagerState.pages.isNotEmpty(),
                        ) {
                            Text(stringResource(R.string.image_share))
                        }
                        TextButton(
                            onClick = { viewModel.openCurrentImageInBrowser() },
                            enabled = pagerState.pages.isNotEmpty(),
                        ) {
                            Text(stringResource(R.string.image_open_in_browser))
                        }
                    },
                )
            }
        },
    ) { scaffoldPadding ->
        val contentPadding = if (isChromeVisible) {
            scaffoldPadding
        } else {
            PaddingValues(0.dp)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .then(
                    if (pagerState.pages.isEmpty()) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = toggleChrome,
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (pagerState.pages.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                DetailPagerSection(
                    state = pagerState,
                    loadCachedOriginal = viewModel::loadCachedOriginal,
                    onToggleChrome = toggleChrome,
                    onPageSettled = viewModel::onPageSettled,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailPagerSection(
    state: DetailPagerState,
    loadCachedOriginal: suspend (ImageUrl) -> File?,
    onToggleChrome: () -> Unit,
    onPageSettled: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = state.pages
    val pagerUiState = rememberPagerState(
        initialPage = state.initialPageIndex.coerceIn(0, pages.lastIndex).index,
        pageCount = { pages.size },
    )
    val settledPageIndexState = rememberUpdatedState(state.settledPageIndex)
    val pageSignature = remember(pages) { pages.joinToString(separator = "\u0000") { it.url } }

    LaunchedEffect(pagerUiState.settledPage) {
        onPageSettled(pagerUiState.settledPage)
    }

    LaunchedEffect(pageSignature) {
        if (pages.isEmpty()) return@LaunchedEffect
        val target = settledPageIndexState.value.coerceIn(0, pages.lastIndex).index
        if (pagerUiState.settledPage != target) {
            pagerUiState.scrollToPage(target)
        }
    }

    HorizontalPager(
        state = pagerUiState,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        key = { page -> pages[page].url },
    ) { page ->
        val imageUrl = pages[page]
        val zoomState = rememberZoomPanState(imageKey = imageUrl.url)

        ZoomableFitAsyncImage(
            imageUrl = imageUrl,
            loadCachedOriginal = loadCachedOriginal,
            zoomState = zoomState,
            modifier = Modifier.fillMaxSize(),
            onSingleTap = onToggleChrome,
        )
    }
}
