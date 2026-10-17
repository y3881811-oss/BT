package com.yourname.bt.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
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

    @GET("x/player/wbi/playurl")
    suspend fun getPlayUrl(
        @Query("bvid") bvid: String,
        @Query("cid") cid: Long,
        @Query("qn") qn: Int = 80,
        @Query("fnval") fnval: Int = 16
    ): PlayUrlResponse

    @GET("x/web-interface/nav")
    fun getNavInfo(): Call<NavResponse>

    @GET("x/passport-login/web/qrcode/generate")
    suspend fun getQrCode(): QrCodeResponse

    @GET("x/passport-login/web/qrcode/poll")
    suspend fun pollQrCode(@Query("qrcode_key") key: String): QrPollResponse
}

object RetrofitClient {
    private const val API_BASE_URL = "https://api.bilibili.com/"
    private const val PASSPORT_BASE_URL = "https://passport.bilibili.com/"

    var appContext: Context? = null

    private var wbiImgKey: String = ""
    private var wbiSubKey: String = ""

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            var request = chain.request()
            val url = request.url
            
            // 基础请求头
            val requestBuilder = request.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://www.bilibili.com")

            // 注入 Cookie
            val savedCookie = if (appContext != null) UserPrefs.getCookie(appContext!!) else ""
            // 如果没登录，强行塞一个基础 buvid3 骗过风控
            val finalCookie = if (savedCookie.isNotEmpty()) savedCookie else "buvid3=infoc"
            requestBuilder.header("Cookie", finalCookie)

            // ================= 核心逻辑 =================
            // 只有包含 "wbi" 的接口（比如 playurl）才进行签名
            if (url.host.contains("api.bilibili.com") && url.encodedPath.contains("wbi")) {
                if (wbiImgKey.isEmpty()) {
                    refreshWbiKeys()
                }

                if (wbiImgKey.isNotEmpty()) {
                    val originalParams = mutableMapOf<String, String>()
                    for (i in 0 until url.querySize) {
                        originalParams[url.queryParameterName(i)] = url.queryParameterValue(i) ?: ""
                    }
                    
                    val signedParams = WbiSigner.sign(originalParams, wbiImgKey, wbiSubKey)
                    val newUrlBuilder = url.newBuilder()
                    for (name in url.queryParameterNames) {
                        newUrlBuilder.removeAllQueryParameters(name)
                    }
                    signedParams.forEach { (key, value) ->
                        newUrlBuilder.addQueryParameter(key, value)
                    }
                    
                    requestBuilder.url(newUrlBuilder.build())
                }
            }
            // ============================================

            chain.proceed(requestBuilder.build())
        }
        .build()

    private fun refreshWbiKeys() {
        try {
            val tempRetrofit = Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val tempApi = tempRetrofit.create(BiliApi::class.java)
            
            // 获取Key时也必须带上 UA 和 Cookie
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Cookie", "buvid3=infoc")
                        .build())
                }.build()
            
            val apiWithHeader = Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BiliApi::class.java)

            val response = apiWithHeader.getNavInfo().execute()
            if (response.isSuccessful) {
                val navData = response.body()?.data
                val imgUrl = navData?.wbiImg?.imgUrl ?: ""
                val subUrl = navData?.wbiImg?.subUrl ?: ""
                if (imgUrl.isNotEmpty() && subUrl.isNotEmpty()) {
                    wbiImgKey = imgUrl.substringAfterLast("/").substringBefore(".")
                    wbiSubKey = subUrl.substringAfterLast("/").substringBefore(".")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val api: BiliApi by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BiliApi::class.java)
    }

    val passportApi: BiliApi by lazy {
        Retrofit.Builder()
            .baseUrl(PASSPORT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BiliApi::class.java)
    }
}