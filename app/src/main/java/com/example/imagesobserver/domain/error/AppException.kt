package com.example.imagesobserver.domain.error

/** Application-level failure with a user- or log-facing message from [android.content.res.Resources]. */
class AppException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
