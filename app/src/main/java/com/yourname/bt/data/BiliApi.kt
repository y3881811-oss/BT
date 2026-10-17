package com.yourname.bt.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface BiliApi {
    @GET("x/web-interface/popular?ps=20&pn=1")
    suspend fun getPopularVideos(): BiliResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://api.bilibili.com/"

    val api: BiliApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BiliApi::class.java)
    }
}