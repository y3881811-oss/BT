package com.yourname.bt.data

import android.content.Context
import okhttp3.HttpUrl
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

    // 注意：路径变为 wbi/playurl
    @GET("x/player/wbi/playurl")
    suspend fun getPlayUrl(
        @Query("bvid") bvid: String,
        @Query("cid") cid: Long,
        @Query("qn") qn: Int = 80,
        @Query("fnval") fnval: Int = 16
    ): PlayUrlResponse

    // 获取 WBI keys 的导航接口
    @GET("x/web-interface/nav")
    suspend fun getNavInfo(): NavResponse

    // 扫码登录接口（注意这里是相对路径，因为 BASE_URL 会切换）
    @GET("x/passport-login/web/qrcode/generate")
    suspend fun getQrCode(): QrCodeResponse

    @GET("x/passport-login/web/qrcode/poll")
    suspend fun pollQrCode(@Query("qrcode_key") key: String): QrPollResponse
}

object RetrofitClient {
    private const val API_BASE_URL = "https://api.bilibili.com/"
    private const val PASSPORT_BASE_URL = "https://passport.bilibili.com/"

    var appContext: Context? = null

    // 缓存 WBI Keys
    private var wbiImgKey: String = ""
    private var wbiSubKey: String = ""
    private var lastKeyFetchTime: Long = 0

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            var request = chain.request()
            val url = request.url

            // 1. 如果是 passport 域名，直接放行（登录接口）
            if (url.host.contains("passport.bilibili.com")) {
                return@addInterceptor chain.proceed(request)
            }

            // 2. 对于 api.bilibili.com，需要 WBI 签名
            if (url.host.contains("api.bilibili.com")) {
                // 如果 key 为空或过期（B站key每日轮换，这里简单起见每次检查）
                if (wbiImgKey.isEmpty()) {
                    refreshWbiKeys()
                }

                // 对 URL 中的 query 参数进行签名
                if (wbiImgKey.isNotEmpty()) {
                    val originalParams = mutableMapOf<String, String>()
                    for (i in 0 until url.querySize) {
                        originalParams[url.queryParameterName(i)] = url.queryParameterValue(i) ?: ""
                    }
                    
                    val signedParams = WbiSigner.sign(originalParams, wbiImgKey, wbiSubKey)
                    
                    val newUrlBuilder = url.newBuilder()
                    // 清除旧参数，加入签名后的参数
                    newUrlBuilder.query(null)
                    signedParams.forEach { (key, value) ->
                        newUrlBuilder.addQueryParameter(key, value)
                    }
                    
                    request = request.newBuilder()
                        .url(newUrlBuilder.build())
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Referer", "https://www.bilibili.com")
                        .build()
                }
            }

            val savedCookie = if (appContext != null) UserPrefs.getCookie(appContext!!) else ""
            if (savedCookie.isNotEmpty()) {
                request = request.newBuilder().header("Cookie", savedCookie).build()
            }

            chain.proceed(request)
        }
        .build()

    // 刷新 WBI Keys
    private fun refreshWbiKeys() {
        try {
            val response = api.getNavInfo().execute() // 同步执行
            if (response.isSuccessful) {
                val navData = response.body()?.data
                val imgUrl = navData?.wbiImg?.imgUrl ?: ""
                val subUrl = navData?.wbiImg?.subUrl ?: ""
                // 从 URL 中提取文件名作为 key
                wbiImgKey = imgUrl.substringAfterLast("/").substringBefore(".")
                wbiSubKey = subUrl.substringAfterLast("/").substringBefore(".")
                lastKeyFetchTime = System.currentTimeMillis()
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

    // 专用的 Passport API（登录用）
    val passportApi: BiliApi by lazy {
        Retrofit.Builder()
            .baseUrl(PASSPORT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BiliApi::class.java)
    }
}