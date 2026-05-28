package com.example.imagesobserver.domain.model

/** Manifest link URLs in grid order (invalid lines omitted). */
fun List<ManifestGridRow>.manifestLinks(): List<ImageUrl> = mapNotNull { row ->
    when (row) {
        is ManifestGridRow.Link -> row.url
        is ManifestGridRow.InvalidLine -> null
    }
}
