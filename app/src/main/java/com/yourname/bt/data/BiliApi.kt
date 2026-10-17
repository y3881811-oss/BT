package com.yourname.bt.data

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface BiliApi {
    @GET("x/web-interface/popular?ps=20&pn=1")
    suspend fun getPopularVideos(): BiliResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://api.bilibili.com/"

    // 核心：通过拦截器伪装请求头，绕过 -352 风控
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original: Request = chain.request()
            val requestBuilder = original.newBuilder()
                // 伪装成高版本的 Chrome 浏览器 (版本需 >= 115)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://www.bilibili.com/")
                .header("Origin", "https://www.bilibili.com")
                .header("Accept", "application/json, text/plain, */*")
            chain.proceed(requestBuilder.build())
        }
        .build()

    val api: BiliApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 把配好请求头的 client 交给 Retrofit
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BiliApi::class.java)
    }
}