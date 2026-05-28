package com.example.imagesobserver.data.cache

import com.example.imagesobserver.constants.ProjectConstants
import com.example.imagesobserver.data.api.ImagesRetrofit
import com.example.imagesobserver.di.IoDispatcher
import com.example.imagesobserver.domain.error.AppErrorFactory
import com.example.imagesobserver.domain.model.ImageUrl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Downloads raw image bytes for a manifest URL. */
@Singleton
class ImageBytesDownloader @Inject constructor(
    private val imagesRetrofit: ImagesRetrofit,
    private val appErrorFactory: AppErrorFactory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun download(imageUrl: ImageUrl): ByteArray = withContext(ioDispatcher) {
        val url = imageUrl.url
        try {
            withTimeout(ProjectConstants.IMAGE_DOWNLOAD_TIMEOUT_SEC * 1000) {
                imagesRetrofit.imageDownloadApi.downloadBytes(url).use { body ->
                    body.bytes().takeIf { it.isNotEmpty() }
                        ?: throw appErrorFactory.emptyResponseBody(url)
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw IOException(
                "Image download timed out after ${ProjectConstants.IMAGE_DOWNLOAD_TIMEOUT_SEC}s: $url",
                e,
            )
        } catch (e: HttpException) {
            throw appErrorFactory.httpDownloadFailed(e.code(), url)
        }
    }
}
