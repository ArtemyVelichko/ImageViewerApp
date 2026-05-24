package com.example.imagesobserver.data.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

/** Downloads image bytes from arbitrary absolute URLs in the manifest. */
interface ImageDownloadApiService {

    @GET
    suspend fun downloadBytes(@Url url: String): ResponseBody
}
