package com.example.imagesobserver.data.repository

import com.example.imagesobserver.data.api.ImagesRetrofit
import com.example.imagesobserver.data.local.ImagesListCacheStore
import com.example.imagesobserver.domain.error.AppErrorFactory
import com.example.imagesobserver.domain.repository.ImagesListRepository
import com.example.imagesobserver.di.IoDispatcher
import com.example.imagesobserver.util.smartRetry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagesListRepositoryImpl @Inject constructor(
    private val imagesRetrofit: ImagesRetrofit,
    private val cacheStore: ImagesListCacheStore,
    private val appErrorFactory: AppErrorFactory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ImagesListRepository {

    override suspend fun fetchRemoteList(): String = withContext(ioDispatcher) {
        delay(1000)
        smartRetry(appErrorFactory) {
            imagesRetrofit.imagesManifestApi.downloadManifestFile()
        }
    }

    override suspend fun readCachedList(): String? = withContext(ioDispatcher) {
        cacheStore.readList()
    }

    override suspend fun writeCachedList(content: String) = withContext(ioDispatcher) {
        cacheStore.writeList(content)
    }
}
