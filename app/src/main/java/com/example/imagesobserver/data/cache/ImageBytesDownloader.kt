package com.example.imagesobserver.data.cache

import com.example.imagesobserver.data.api.ImagesRetrofit
import com.example.imagesobserver.di.IoDispatcher
import com.example.imagesobserver.domain.error.AppErrorFactory
import com.example.imagesobserver.domain.model.ImageUrl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

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
            imagesRetrofit.imageDownloadApi.downloadBytes(url).use { body ->
                body.bytes().takeIf { it.isNotEmpty() }
                    ?: throw appErrorFactory.emptyResponseBody(url)
            }
        } catch (e: HttpException) {
            throw appErrorFactory.httpDownloadFailed(e.code(), url)
        }
    }
}
