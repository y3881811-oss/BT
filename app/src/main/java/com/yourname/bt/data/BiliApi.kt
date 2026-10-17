package com.yourname.bt.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call // 新增 Call 导入
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

    // 【修改】去掉 suspend，改为同步 Call，供拦截器内部调用
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
    private var lastKeyFetchTime: Long = 0

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            var request = chain.request()
            val url = request.url

            // 1. passport 域名直接放行
            if (url.host.contains("passport.bilibili.com")) {
                return@addInterceptor chain.proceed(request)
            }

            // 2. api 域名进行 WBI 签名
            if (url.host.contains("api.bilibili.com")) {
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
                    // 【修复】正确清除旧参数的方法
                    for (name in url.queryParameterNames) {
                        newUrlBuilder.removeAllQueryParameters(name)
                    }
                    // 添加签名后的参数
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

    private fun refreshWbiKeys() {
        try {
            // 【修复】新建临时 Retrofit（不带拦截器），防止在拦截器内部发起请求导致死循环
            val tempRetrofit = Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val tempApi = tempRetrofit.create(BiliApi::class.java)
            
            // 同步执行
            val response = tempApi.getNavInfo().execute()
            if (response.isSuccessful) {
                val navData = response.body()?.data
                val imgUrl = navData?.wbiImg?.imgUrl ?: ""
                val subUrl = navData?.wbiImg?.subUrl ?: ""
                if (imgUrl.isNotEmpty() && subUrl.isNotEmpty()) {
                    wbiImgKey = imgUrl.substringAfterLast("/").substringBefore(".")
                    wbiSubKey = subUrl.substringAfterLast("/").substringBefore(".")
                    lastKeyFetchTime = System.currentTimeMillis()
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