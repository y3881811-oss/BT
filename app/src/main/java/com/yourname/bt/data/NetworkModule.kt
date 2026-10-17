package com.yourname.bt.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val API_BASE_URL = "https://api.bilibili.com/"
    private const val PASSPORT_BASE_URL = "https://passport.bilibili.com/"

    private var appContext: Context? = null
    private var wbiImgKey: String = ""
    private var wbiSubKey: String = ""
    private var lastKeyFetchTime: Long = 0

    // 基础 OkHttpClient（用于获取 WBI Key 和 实际请求）
    private val baseOkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // 初始化网络模块（在 Application 或 MainActivity 中调用）
    fun init(context: Context) {
        appContext = context.applicationContext
        // 预加载 WBI Key
        fetchWbiKeysAsync()
    }

    // 游客 Cookie，防止 B 站直接拒绝请求
    private fun getGuestCookie(): String {
        return "buvid3=infoc;"
    }

    // 获取当前 Cookie
    private fun getCurrentCookie(): String {
        val saved = if (appContext != null) UserPrefs.getCookie(appContext!!) else ""
        return if (saved.isNotEmpty()) saved else getGuestCookie()
    }

    // 异步获取 WBI Key（使用纯 OkHttp，避免拦截器死循环）
    private fun fetchWbiKeysAsync() {
        Thread {
            try {
                val request = Request.Builder()
                    .url("${API_BASE_URL}x/web-interface/nav")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Referer", "https://www.bilibili.com")
                    .header("Cookie", getCurrentCookie())
                    .build()

                val response = baseOkHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    // 简单粗暴地解析 JSON 提取 key，避免引入复杂依赖
                    val imgUrl = bodyStr.substringAfter("\"img_url\":\"", "").substringBefore("\"")
                    val subUrl = bodyStr.substringAfter("\"sub_url\":\"", "").substringBefore("\"")
                    
                    if (imgUrl.isNotEmpty() && subUrl.isNotEmpty()) {
                        wbiImgKey = imgUrl.substringAfterLast("/").substringBefore(".")
                        wbiSubKey = subUrl.substringAfterLast("/").substringBefore(".")
                        lastKeyFetchTime = System.currentTimeMillis()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // 构建拦截器（处理 Cookie 和 WBI 签名）
    private fun createInterceptor(): okhttp3.Interceptor {
        return okhttp3.Interceptor { chain ->
            var request = chain.request()
            val url = request.url

            // 1. 统一添加 Header 和 Cookie
            val requestBuilder = request.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://www.bilibili.com")
                .header("Cookie", getCurrentCookie())

            // 2. 只有路径中包含 "wbi" 的接口才进行加签（如 playurl）
            if (url.host.contains("api.bilibili.com") && url.encodedPath.contains("wbi")) {
                if (wbiImgKey.isEmpty()) {
                    // 如果 Key 还没准备好，同步获取一次（极少情况）
                    fetchWbiKeysAsync()
                    Thread.sleep(500) 
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

            chain.proceed(requestBuilder.build())
        }
    }

    // 构建带有拦截器的 OkHttpClient
    private fun createClientWithInterceptor(): OkHttpClient {
        return baseOkHttpClient.newBuilder()
            .addInterceptor(createInterceptor())
            .build()
    }

    // 提供 API 实例
    val api: BiliApi by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(createClientWithInterceptor())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BiliApi::class.java)
    }

    val passportApi: BiliApi by lazy {
        Retrofit.Builder()
            .baseUrl(PASSPORT_BASE_URL)
            .client(createClientWithInterceptor())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BiliApi::class.java)
    }
}