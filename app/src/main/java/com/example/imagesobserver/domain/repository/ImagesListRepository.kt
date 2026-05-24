package com.example.imagesobserver.domain.repository

/**
 * Remote [images.txt] list and its on-disk cache (one URL per line, no parsing here).
 */
interface ImagesListRepository {

    suspend fun fetchRemoteList(): String

    suspend fun readCachedList(): String?

    suspend fun writeCachedList(content: String)
}
