package com.example.imagesobserver.util

import com.example.imagesobserver.constants.ProjectConstants
import com.example.imagesobserver.domain.error.AppErrorFactory

/**
 * Runs [block] up to [maxAttempts] times. On the last attempt, rethrows the failure.
 */
suspend inline fun <T> smartRetry(
    appErrorFactory: AppErrorFactory,
    maxAttempts: Int = ProjectConstants.SMART_RETRY_ATTEMPTS,
    crossinline block: suspend () -> T,
): T {
    if (maxAttempts < 1) {
        throw appErrorFactory.maxAttemptsInvalid()
    }
    var lastError: Throwable? = null
    for (attempt in 1..maxAttempts) {
        try {
            return block()
        } catch (t: Throwable) {
            lastError = t
            if (attempt == maxAttempts) throw t
        }
    }
    throw lastError ?: appErrorFactory.smartRetryNoAttempts()
}
