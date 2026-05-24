package com.example.imagesobserver.domain.model

/**
 * One manifest line in the images grid: a loadable link or a placeholder row.
 */
sealed interface ManifestGridRow {

    data class Link(val url: ImageUrl) : ManifestGridRow

    /** Non-empty line that is not an HTTP(S) URL — grid shows a stub cell. */
    data class InvalidLine(val raw: String) : ManifestGridRow
}
