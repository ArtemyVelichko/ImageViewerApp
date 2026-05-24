package com.example.imagesobserver.data.api

import retrofit2.http.GET

/** Downloads the remote [images.txt] manifest (one image URL per line). */
interface ImagesManifestApiService {

    @GET("images.txt")
    suspend fun downloadManifestFile(): String
}
