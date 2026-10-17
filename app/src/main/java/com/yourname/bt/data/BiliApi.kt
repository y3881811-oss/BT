package com.yourname.bt.data

import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

interface BiliApi {
    @GET("x/web-interface/popular?ps=20&pn=1")
    suspend fun getPopularVideos(): BiliResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://api.bilibili.com/"
    private val json = Json { ignoreUnknownKeys = true }

    val api: BiliApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BiliApi::class.java)
    }
}