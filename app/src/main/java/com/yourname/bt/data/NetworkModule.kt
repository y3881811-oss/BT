package com.yourname.bt.data

import android.content.Context
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val API_BASE_URL = "https://api.bilibili.com/"
    private const val PASSPORT_BASE_URL = "https://passport.bilibili.com/"
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private var appContext: Context? = null
    private var wbiImgKey: String = ""
    private var wbiSubKey: String = ""
    private var realBuvid3: String = ""
    private var realBuvid4: String = ""

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun init(context: Context) {
        appContext = context.applicationContext
        fetchBuvid()
        fetchWbiKeys()
    }

    private fun fetchBuvid() {
        Thread {
            try {
                val request = Request.Builder()
                    .url("${API_BASE_URL}x/frontend/finger/spi")
                    .header("User-Agent", UA)
                    .build()
                baseClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "")
                        if (json.optInt("code") == 0) {
                            val data = json.getJSONObject("data")
                            realBuvid3 = data.optString("b_3")
                            realBuvid4 = data.optString("b_4")
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }

    private fun fetchWbiKeys() {
        Thread {
            try {
                val request = Request.Builder()
                    .url("${API_BASE_URL}x/web-interface/nav")
                    .header("User-Agent", UA)
                    .header("Referer", "https://www.bilibili.com")
                    .header("Cookie", getCurrentCookie())
                    .build()
                baseClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "")
                        if (json.optInt("code") == 0) {
                            val wbiImg = json.getJSONObject("data").getJSONObject("wbi_img")
                            wbiImgKey = wbiImg.getString("img_url").substringAfterLast("/").substringBefore(".")
                            wbiSubKey = wbiImg.getString("sub_url").substringAfterLast("/").substringBefore(".")
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }

    private fun getCurrentCookie(): String {
        val saved = if (appContext != null) UserPrefs.getCookie(appContext!!) else ""
        return if (saved.isNotEmpty()) saved else getGuestCookie()
    }

    private fun getGuestCookie(): String {
        val b3 = realBuvid3.ifEmpty { "infoc" }
        return "buvid3=$b3; buvid4=$realBuvid4; b_nut=${System.currentTimeMillis() / 1000};"
    }

    private fun createInterceptor(): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()
            val url = request.url
            val builder = request.newBuilder()
                .header("User-Agent", UA)
                .header("Referer", "https://www.bilibili.com")
                .header("Cookie", getCurrentCookie())
                .header("Origin", "https://www.bilibili.com")
                .header("X-Requested-With", "XMLHttpRequest")

            if (url.host.contains("api.bilibili.com") && url.encodedPath.contains("wbi")) {
                if (wbiImgKey.isEmpty()) {
                    fetchWbiKeys()
                    Thread.sleep(800)
                }
                if (wbiImgKey.isNotEmpty()) {
                    val params = mutableMapOf<String, String>()
                    for (i in 0 until url.querySize) {
                        params[url.queryParameterName(i)] = url.queryParameterValue(i) ?: ""
                    }
                    val signed = WbiSigner.sign(params, wbiImgKey, wbiSubKey)
                    val newUrl = url.newBuilder().apply {
                        url.queryParameterNames.forEach { removeAllQueryParameters(it) }
                        signed.forEach { (k, v) -> addQueryParameter(k, v) }
                    }.build()
                    builder.url(newUrl)
                }
            }
            chain.proceed(builder.build())
        }
    }

    private fun createClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        return baseClient.newBuilder()
            .addInterceptor(logging)
            .addInterceptor(createInterceptor())
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (response.code != 200) {
                    println("B站报错: ${response.code} -> ${response.peekBody(1024 * 1024).string()}")
                }
                response
            }
            .build()
    }

    val api: BiliApi by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(createClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BiliApi::class.java)
    }

    val passportApi: BiliApi by lazy {
        Retrofit.Builder()
            .baseUrl(PASSPORT_BASE_URL)
            .client(createClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BiliApi::class.java)
    }
}