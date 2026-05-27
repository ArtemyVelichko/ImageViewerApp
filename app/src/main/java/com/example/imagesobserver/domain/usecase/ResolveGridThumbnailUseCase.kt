package com.example.imagesobserver.domain.usecase

import com.example.imagesobserver.data.cache.GridThumbnailMemoryCache
import com.example.imagesobserver.domain.model.DisplayableImageResult
import com.example.imagesobserver.domain.model.GridThumbnailResult
import com.example.imagesobserver.domain.model.ImageUrl
import com.example.imagesobserver.domain.validation.DisplayableImageValidator
import com.example.imagesobserver.domain.validation.resolveDisplayable
import javax.inject.Inject

/**
 * Memory cache → disk thumbnail load → bitmap validation for one grid cell.
 * Network failures are [GridThumbnailResult.LoadFailed]; invalid bitmaps are [GridThumbnailResult.Invalid].
 */
class ResolveGridThumbnailUseCase @Inject constructor(
    private val loadGridThumbnailUseCase: LoadGridThumbnailUseCase,
    private val displayableImageValidator: DisplayableImageValidator,
    private val gridThumbnailMemoryCache: GridThumbnailMemoryCache,
) {

    suspend operator fun invoke(
        imageUrl: ImageUrl,
        widthPx: Int,
        heightPx: Int,
    ): GridThumbnailResult {
        peek(imageUrl, widthPx, heightPx)?.let { return it }

        val file = loadGridThumbnailUseCase(imageUrl, widthPx, heightPx)
            ?: return GridThumbnailResult.LoadFailed.also {
                cache(imageUrl, widthPx, heightPx, it)
            }
        return when (val validated = displayableImageValidator.resolveDisplayable(file)) {
            is DisplayableImageResult.Displayable -> {
                GridThumbnailResult.Displayable(validated.file).also {
                    cache(imageUrl, widthPx, heightPx, it)
                }
            }
            DisplayableImageResult.NotDisplayable -> {
                GridThumbnailResult.Invalid.also {
                    cache(imageUrl, widthPx, heightPx, it)
                }
            }
        }
    }

    fun peek(imageUrl: ImageUrl, widthPx: Int, heightPx: Int): GridThumbnailResult? =
        gridThumbnailMemoryCache.get(imageUrl, widthPx, heightPx)?.toResult()

    fun evict(imageUrl: ImageUrl) {
        gridThumbnailMemoryCache.evict(imageUrl)
    }

    fun clearMemoryCache() {
        gridThumbnailMemoryCache.clear()
    }

    private fun cache(
        imageUrl: ImageUrl,
        widthPx: Int,
        heightPx: Int,
        result: GridThumbnailResult,
    ) {
        gridThumbnailMemoryCache.put(imageUrl, widthPx, heightPx, result.toEntry())
    }

    private fun GridThumbnailMemoryCache.Entry.toResult(): GridThumbnailResult = when (this) {
        is GridThumbnailMemoryCache.Entry.Displayable -> GridThumbnailResult.Displayable(file)
        GridThumbnailMemoryCache.Entry.LoadFailed -> GridThumbnailResult.LoadFailed
        GridThumbnailMemoryCache.Entry.Invalid -> GridThumbnailResult.Invalid
    }

    private fun GridThumbnailResult.toEntry(): GridThumbnailMemoryCache.Entry = when (this) {
        is GridThumbnailResult.Displayable -> GridThumbnailMemoryCache.Entry.Displayable(file)
        GridThumbnailResult.LoadFailed -> GridThumbnailMemoryCache.Entry.LoadFailed
        GridThumbnailResult.Invalid -> GridThumbnailMemoryCache.Entry.Invalid
    }
}
