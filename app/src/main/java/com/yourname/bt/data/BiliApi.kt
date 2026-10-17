package com.yourname.bt.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface BiliApi {
    @GET("x/web-interface/popular?ps=20&pn=1")
    suspend fun getPopularVideos(): BiliResponse

    @GET("x/player/pagelist")
    suspend fun getCid(@Query("bvid") bvid: String): PageListResponse

    @GET("x/player/playurl")
    suspend fun getPlayUrl(
        @Query("bvid") bvid: String,
        @Query("cid") cid: Long,
        @Query("qn") qn: Int = 80, // 【升级】登录后请求 1080P
        @Query("fnval") fnval: Int = 16
    ): PlayUrlResponse

    // 申请扫码
    @GET("x/passport-login/web/qrcode/generate")
    suspend fun getQrCode(): QrCodeResponse

    // 轮询扫码状态
    @GET("x/passport-login/web/qrcode/poll")
    suspend fun pollQrCode(@Query("qrcode_key") key: String): QrPollResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://api.bilibili.com/"
    
    // 必须全局保存 context 以读取 Cookie
    var appContext: Context? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original: Request = chain.request()
            val requestBuilder = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://www.bilibili.com")
            
            // 【核心】从本地读取登录后的 Cookie
            val savedCookie = if (appContext != null) UserPrefs.getCookie(appContext!!) else ""
            if (savedCookie.isNotEmpty()) {
                requestBuilder.header("Cookie", savedCookie)
            }
            
            chain.proceed(requestBuilder.build())
        }
        .build()

    val api: BiliApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BiliApi::class.java)
    }
}