package com.example.imagesobserver.data.local.provider

import androidx.annotation.StringRes
import com.example.imagesobserver.domain.error.AppException

fun ResourceProvider.appException(
    @StringRes messageResId: Int,
    vararg formatArgs: Any,
    cause: Throwable? = null,
): AppException = AppException(getString(messageResId, *formatArgs), cause)
