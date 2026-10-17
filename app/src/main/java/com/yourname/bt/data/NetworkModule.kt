package com.yourname.bt.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor // 【新增】
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val API_BASE_URL = "https://api.bilibili.com/"
    private const val PASSPORT_BASE_URL = "https://passport.bilibili.com/"

    private var appContext: Context? = null
    private var wbiImgKey: String = ""
    private var wbiSubKey: String = ""

    private val baseOkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        fetchWbiKeysAsync()
    }

    private fun getGuestCookie(): String {
        return "buvid3=infoc;"
    }

    private fun getCurrentCookie(): String {
        val saved = if (appContext != null) UserPrefs.getCookie(appContext!!) else ""
        return if (saved.isNotEmpty()) saved else getGuestCookie()
    }

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
                    val imgUrl = bodyStr.substringAfter("\"img_url\":\"", "").substringBefore("\"")
                    val subUrl = bodyStr.substringAfter("\"sub_url\":\"", "").substringBefore("\"")
                    
                    if (imgUrl.isNotEmpty() && subUrl.isNotEmpty()) {
                        wbiImgKey = imgUrl.substringAfterLast("/").substringBefore(".")
                        wbiSubKey = subUrl.substringAfterLast("/").substringBefore(".")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun createInterceptor(): okhttp3.Interceptor {
        return okhttp3.Interceptor { chain ->
            var request = chain.request()
            val url = request.url

            val requestBuilder = request.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://www.bilibili.com")
                .header("Cookie", getCurrentCookie())

            // 加上这个头，有时候能过风控
            requestBuilder.header("X-Requested-With", "XMLHttpRequest")

            if (url.host.contains("api.bilibili.com") && url.encodedPath.contains("wbi")) {
                if (wbiImgKey.isEmpty()) {
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

    // 【核心修改】加入 LoggingInterceptor，并在发生非 200 错误时读取 errorBody
    private fun createClientWithInterceptor(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY // 打印请求头和响应体

        return baseOkHttpClient.newBuilder()
            .addInterceptor(logging)
            .addInterceptor(createInterceptor())
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (response.code != 200) {
                    // 如果报错（比如 412），强行把错误内容打印出来
                    val errorBody = response.peekBody(1024 * 1024).string()
                    println("B站接口报错: HTTP ${response.code} -> $errorBody")
                    // 如果是在手机上，可以用 Toast 弹出来（需要传 Context，这里为了简单用 Log）
                }
                response
            }
            .build()
    }

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