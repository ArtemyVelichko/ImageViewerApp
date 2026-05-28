package com.example.imagesobserver.presentation.images.model

import com.example.imagesobserver.domain.model.GridThumbnailResult
import com.example.imagesobserver.domain.model.ImageUrl
import java.io.File

data class GridThumbnailCallbacks(
    val load: suspend (ImageUrl, Int, Int) -> File?,
    val peek: (ImageUrl, Int, Int) -> GridThumbnailResult?,
    val retry: (ImageUrl, Int, Int) -> Unit,
)
