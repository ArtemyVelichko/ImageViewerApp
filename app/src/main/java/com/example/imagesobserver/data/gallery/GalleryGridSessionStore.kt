package com.example.imagesobserver.data.gallery

import com.example.imagesobserver.domain.model.ManifestGridRow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryGridSessionStore @Inject constructor() {

    private val lock = Any()
    private var session = GalleryGridSession()

    fun snapshot(): GalleryGridSession = synchronized(lock) { session }

    fun updateManifestRows(rows: List<ManifestGridRow>) {
        synchronized(lock) {
            session = session.copy(manifestRows = rows)
        }
    }

    fun updateThumbnailTarget(widthPx: Int, heightPx: Int) {
        synchronized(lock) {
            session = session.copy(
                thumbnailWidthPx = widthPx,
                thumbnailHeightPx = heightPx,
            )
        }
    }

    /** @return false when indices unchanged (caller may skip prefetch restart). */
    fun updateVisibleRowIndices(indices: IntRange?): Boolean = synchronized(lock) {
        if (session.visibleRowIndices == indices) return false
        session = session.copy(visibleRowIndices = indices)
        true
    }

    fun clear() {
        synchronized(lock) {
            session = GalleryGridSession()
        }
    }
}
