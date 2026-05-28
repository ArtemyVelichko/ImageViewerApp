package com.example.imagesobserver.data.gallery

import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.model.ManifestGridRow

object GridThumbnailPrefetchOrder {

    fun orderedUrls(
        rows: List<ManifestGridRow>,
        visibleRowIndices: IntRange?,
    ): List<ImageUrl> {
        val linksByRowIndex = rows.mapIndexedNotNull { index, row ->
            when (row) {
                is ManifestGridRow.Link -> index to row.url
                is ManifestGridRow.InvalidLine -> null
            }
        }
        if (linksByRowIndex.isEmpty()) return emptyList()
        if (visibleRowIndices == null) {
            return linksByRowIndex.map { it.second }
        }
        val visibleRows = visibleRowIndices.filter { it in rows.indices }.toSet()
        val priority = linksByRowIndex
            .filter { (rowIndex, _) -> rowIndex in visibleRows }
            .sortedBy { it.first }
            .map { it.second }
        val priorityUrls = priority.map { it.url }.toSet()
        val rest = linksByRowIndex
            .filter { (_, url) -> url.url !in priorityUrls }
            .sortedBy { it.first }
            .map { it.second }
        return priority + rest
    }
}
