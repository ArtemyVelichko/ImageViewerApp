package com.example.imagesobserver.domain.error

/** Domain-facing factory for [AppException] messages (implemented in the data layer). */
interface AppErrorFactory {

    fun noImagesListData(): AppException

    fun httpDownloadFailed(code: Int, url: String): AppException

    fun emptyResponseBody(url: String): AppException

    fun decodeImageBytesFailed(): AppException

    fun smartRetryNoAttempts(): AppException

    fun maxAttemptsInvalid(): AppException
}
