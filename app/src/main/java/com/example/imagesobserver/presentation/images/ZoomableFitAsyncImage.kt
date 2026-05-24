package com.example.imagesobserver.presentation.images

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.example.imagesobserver.constants.ProjectConstants
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.presentation.detail.DetailFullscreenCoilImage
import com.example.imagesobserver.presentation.theme.Dimens
import java.io.File
import kotlin.math.absoluteValue

/**
 * Zoom and pan state for a single image on the detail screen.
 *
 * @property scale Current zoom factor; `1f` is fit-to-screen.
 * @property panOffset Translation applied after scaling (pixels).
 */
@Stable
class ZoomPanState {
    var scale by mutableFloatStateOf(1f)
        internal set

    var panOffset by mutableStateOf(Offset.Zero)
        internal set

    /** Returns to fit-to-screen with no offset. */
    fun reset() {
        scale = 1f
        panOffset = Offset.Zero
    }
}

/**
 * Remembers [ZoomPanState] and resets it when [imageKey] changes (e.g. pager page / URL).
 */
@Composable
fun rememberZoomPanState(imageKey: String): ZoomPanState {
    val state = remember { ZoomPanState() }
    LaunchedEffect(imageKey) {
        state.reset()
    }
    return state
}

/** Keeps the zoomed image from being panned beyond its visible bounds. */
private fun clampPanOffset(offset: Offset, scale: Float, widthPx: Float, heightPx: Float): Offset {
    if (scale <= 1.001f || widthPx <= 0f || heightPx <= 0f) return Offset.Zero
    val maxX = (widthPx * (scale - 1f) * 0.5f).absoluteValue
    val maxY = (heightPx * (scale - 1f) * 0.5f).absoluteValue
    return Offset(
        offset.x.coerceIn(-maxX, maxX),
        offset.y.coerceIn(-maxY, maxY),
    )
}

private enum class HorizontalDragMode {
    Undecided,
    Pan,
    Swipe,
}

/**
 * Routes one-finger horizontal drags either to image pan or to the parent [HorizontalPager].
 *
 * After touch slop, the gesture is classified once:
 * - **Swipe** — horizontal movement ≥ [Dimens.imageHorizontalPanSwipeThreshold] and mostly horizontal.
 *   Events are not consumed so the pager can flip pages.
 * - **Pan** — image is zoomed in ([ZoomPanState.scale] > 1) and the drag is shorter / not clearly horizontal.
 *   Delta is multiplied by [ProjectConstants.IMAGE_PAN_DRAG_SENSITIVITY].
 *
 * Pinch zoom is handled separately by [Modifier.transformable] with [canPan] disabled.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectPanOrPagerSwipe(
    zoomState: ZoomPanState,
    swipeThresholdPx: Float,
    widthPx: Float,
    heightPx: Float,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var accumulated = Offset.Zero
        var mode = HorizontalDragMode.Undecided
        val touchSlop = viewConfiguration.touchSlop

        do {
            val event = awaitPointerEvent(pass = PointerEventPass.Main)
            if (event.changes.count { it.pressed } > 1) {
                mode = HorizontalDragMode.Undecided
                break
            }

            val change = event.changes.firstOrNull { it.id == down.id } ?: continue
            if (!change.pressed) break

            val delta = change.positionChange()
            if (delta == Offset.Zero) continue

            accumulated += delta

            if (mode == HorizontalDragMode.Undecided &&
                accumulated.getDistance() >= touchSlop
            ) {
                val horizontal = accumulated.x.absoluteValue
                val vertical = accumulated.y.absoluteValue
                mode = when {
                    horizontal >= swipeThresholdPx && horizontal > vertical ->
                        HorizontalDragMode.Swipe

                    zoomState.scale > 1.001f ->
                        HorizontalDragMode.Pan

                    else ->
                        HorizontalDragMode.Swipe
                }
            }

            when (mode) {
                HorizontalDragMode.Pan -> {
                    val panDelta = delta * ProjectConstants.IMAGE_PAN_DRAG_SENSITIVITY
                    zoomState.panOffset = clampPanOffset(
                        zoomState.panOffset + panDelta,
                        zoomState.scale,
                        widthPx,
                        heightPx,
                    )
                    change.consume()
                }

                HorizontalDragMode.Swipe -> Unit

                HorizontalDragMode.Undecided -> Unit
            }
        } while (event.changes.any { it.pressed })
    }
}

/**
 * Full-screen image with [ContentScale.Fit], pinch-to-zoom, and pan when zoomed in.
 *
 * Designed to sit inside a [HorizontalPager] on the detail screen:
 * - **Pinch** — zoom via [Modifier.transformable] (pan disabled there on purpose).
 * - **Short drag while zoomed** — pans the image ([detectPanOrPagerSwipe]).
 * - **Long horizontal drag** — left to the pager (next/previous image).
 * - **Single tap** — [onSingleTap] (e.g. toggle fullscreen UI).
 * - **Double tap** — [ZoomPanState.reset].
 *
 * Modifier order matters: [combinedClickable] is outermost so [transformable] can still
 * receive pinch first; tap detectors that consume down events too early would block paging.
 *
 * @param zoomState Per-page state from [rememberZoomPanState].
 * @param maxScale Upper zoom limit (default 8×).
 * @param onSingleTap Called on a single tap on the image or letterbox area.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomableFitAsyncImage(
    imageUrl: ImageUrl,
    loadCachedOriginal: suspend (ImageUrl) -> File?,
    zoomState: ZoomPanState,
    modifier: Modifier = Modifier,
    maxScale: Float = 8f,
    onSingleTap: () -> Unit = {},
) {
    var contentSize by remember { mutableStateOf(IntSize(1, 1)) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { Dimens.imageHorizontalPanSwipeThreshold.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RectangleShape)
            .onSizeChanged { contentSize = it }
            // Outermost tap target: image and letterbox (empty space around Fit).
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSingleTap,
                onDoubleClick = { zoomState.reset() },
            ),
    ) {
        val wPx = contentSize.width.toFloat().coerceAtLeast(1f)
        val hPx = contentSize.height.toFloat().coerceAtLeast(1f)

        val wPxState = rememberUpdatedState(wPx)
        val hPxState = rememberUpdatedState(hPx)
        val maxScaleState = rememberUpdatedState(maxScale)
        val transformState = rememberTransformableState { zoomChange, _, rotationChange ->
            if (rotationChange != 0f) return@rememberTransformableState
            val w = wPxState.value
            val h = hPxState.value
            val ceiling = maxScaleState.value

            val oldScale = zoomState.scale
            val newScale = (oldScale * zoomChange).coerceIn(1f, ceiling)
            if (newScale <= 1.001f) {
                zoomState.scale = 1f
                zoomState.panOffset = Offset.Zero
                return@rememberTransformableState
            }
            zoomState.scale = newScale
            zoomState.panOffset = clampPanOffset(
                zoomState.panOffset,
                zoomState.scale,
                w,
                h,
            )
        }

        val imageGestureModifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = zoomState.scale,
                scaleY = zoomState.scale,
                translationX = zoomState.panOffset.x,
                translationY = zoomState.panOffset.y,
                transformOrigin = TransformOrigin.Center,
            )
            .pointerInput(zoomState.scale, wPx, hPx, swipeThresholdPx) {
                detectPanOrPagerSwipe(
                    zoomState = zoomState,
                    swipeThresholdPx = swipeThresholdPx,
                    widthPx = wPx,
                    heightPx = hPx,
                )
            }
            .transformable(
                state = transformState,
                canPan = { false },
                lockRotationOnZoomPan = true,
            )

        DetailFullscreenCoilImage(
            imageUrl = imageUrl,
            loadCachedOriginal = loadCachedOriginal,
            modifier = Modifier.fillMaxSize(),
            imageModifier = imageGestureModifier,
            contentScale = ContentScale.Fit,
        )
    }
}
