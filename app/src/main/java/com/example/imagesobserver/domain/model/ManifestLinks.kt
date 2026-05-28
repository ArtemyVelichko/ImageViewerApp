package com.example.imagesobserver.domain.model

/** Manifest link URLs in grid order (invalid lines omitted). */
fun List<ManifestGridRow>.manifestLinks(): List<ImageUrl> = mapNotNull { row ->
    when (row) {
        is ManifestGridRow.Link -> row.url
        is ManifestGridRow.InvalidLine -> null
    }
}

/** Detail pager: only URLs with a loaded thumbnail ([ImageGalleryUrlStatus.Openable]). */
fun List<ImageUrl>.detailPagerUrls(loadState: ImageGalleryLoadState): List<ImageUrl> =
    filter { loadState.isOpenable(it.url) }
