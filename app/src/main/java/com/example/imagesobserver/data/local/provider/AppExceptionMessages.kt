package com.example.imagesobserver.data.local.provider

import com.example.imagesobserver.R
import com.example.imagesobserver.domain.error.AppErrorFactory
import com.example.imagesobserver.domain.error.AppException
import javax.inject.Inject
import javax.inject.Singleton

/** [AppErrorFactory] backed by [strings.xml][com.example.imagesobserver.R.string]. */
@Singleton
class AppExceptionMessages @Inject constructor(
    private val resourceProvider: ResourceProvider,
) : AppErrorFactory {

    override fun noImagesListData(): AppException =
        resourceProvider.appException(R.string.error_no_images_list_data)

    override fun httpDownloadFailed(code: Int, url: String): AppException =
        resourceProvider.appException(R.string.error_http_download_failed, code, url)

    override fun emptyResponseBody(url: String): AppException =
        resourceProvider.appException(R.string.error_empty_response_body, url)

    override fun decodeImageBytesFailed(): AppException =
        resourceProvider.appException(R.string.error_decode_image_bytes)

    override fun smartRetryNoAttempts(): AppException =
        resourceProvider.appException(R.string.error_smart_retry_no_attempts)

    override fun maxAttemptsInvalid(): AppException =
        resourceProvider.appException(R.string.error_max_attempts_invalid)
}
