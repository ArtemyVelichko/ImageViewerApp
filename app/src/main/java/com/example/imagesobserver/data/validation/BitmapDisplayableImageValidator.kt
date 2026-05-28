package com.example.imagesobserver.data.validation

import com.example.imagesobserver.di.IoDispatcher
import com.example.imagesobserver.domain.validation.DisplayableImageValidator
import com.example.imagesobserver.util.isBitmapValid
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class BitmapDisplayableImageValidator @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DisplayableImageValidator {

    override suspend fun isDisplayable(file: File): Boolean = withContext(ioDispatcher) {
        file.isBitmapValid()
    }

    override suspend fun isDisplayable(bytes: ByteArray): Boolean = withContext(ioDispatcher) {
        bytes.isBitmapValid()
    }
}
