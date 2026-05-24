package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.model.ManifestGridRow
import javax.inject.Inject

/**
 * Infallible pure transform: HTTP(S) lines become [ManifestGridRow.Link], other non-empty lines
 * become [ManifestGridRow.InvalidLine] (placeholder in UI). I/O errors live in list-load use cases.
 */
class ParseManifestLinesToGridRowsUseCase @Inject constructor() {

    operator fun invoke(text: String): List<ManifestGridRow> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                if (line.startsWith("http://", ignoreCase = true) ||
                    line.startsWith("https://", ignoreCase = true)
                ) {
                    ManifestGridRow.Link(ImageUrl(line))
                } else {
                    ManifestGridRow.InvalidLine(line)
                }
            }
            .toList()
}
