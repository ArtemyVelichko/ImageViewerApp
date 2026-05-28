package com.example.imagesobserver.data.gallery

import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.model.ManifestGridRow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade for grid thumbnail loading: session state, background prefetch, and per-cell load/retry.
 *
 * Grid reports cell size + visible rows; [GridThumbnailPrefetchWorker] runs ordered prefetch.
 */
@Singleton
class ImageGalleryLoader @Inject constructor(
    private val sessionStore: GalleryGridSessionStore,
    private val prefetchWorker: GridThumbnailPrefetchWorker,
    private val thumbnailLoader: GridThumbnailLoader,
) {

    fun onManifestRowsChanged(rows: List<ManifestGridRow>) {
        sessionStore.updateManifestRows(rows)
        restartPrefetchIfReady()
    }

    fun onGridThumbnailTargetChanged(widthPx: Int, heightPx: Int) {
        sessionStore.updateThumbnailTarget(widthPx, heightPx)
        restartPrefetchIfReady()
    }

    fun onVisibleRowsChanged(firstVisibleIndex: Int, lastVisibleIndex: Int) {
        if (firstVisibleIndex > lastVisibleIndex) return
        val changed = sessionStore.updateVisibleRowIndices(firstVisibleIndex..lastVisibleIndex)
        if (changed) {
            restartPrefetchIfReady()
        }
    }

    suspend fun loadThumbnail(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): File? =
        thumbnailLoader.load(imageUrl, widthPx, heightPx)

    fun retry(imageUrl: ImageUrl, widthPx: Int, heightPx: Int) {
        thumbnailLoader.retry(imageUrl, widthPx, heightPx)
    }

    fun getGridThumbnailTargetPx(): Pair<Int, Int>? = sessionStore.snapshot().thumbnailTargetPx()

    fun cancelAll() {
        prefetchWorker.cancel()
        sessionStore.clear()
    }

    private fun restartPrefetchIfReady() {
        prefetchWorker.restart(sessionStore.snapshot())
    }
}
