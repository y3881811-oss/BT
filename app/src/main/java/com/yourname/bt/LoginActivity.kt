package com.yourname.bt

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.yourname.bt.data.RetrofitClient
import com.yourname.bt.data.UserPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var qrImage: ImageView
    private lateinit var tipText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        qrImage = findViewById(R.id.qrImage)
        tipText = findViewById(R.id.tipText)

        startLoginFlow()
    }

    private fun startLoginFlow() {
        lifecycleScope.launch {
            try {
                // 1. 获取二维码
                val qrRes = RetrofitClient.api.getQrCode()
                if (qrRes.code != 0) {
                    tipText.text = "获取二维码失败"
                    return@launch
                }
                val qrUrl = qrRes.data.url
                val qrKey = qrRes.data.qrcode_key

                // 2. 生成二维码图片
                qrImage.setImageBitmap(generateQrCode(qrUrl))

                // 3. 轮询扫码状态
                while (true) {
                    delay(2000) // 每2秒查一次
                    val pollRes = RetrofitClient.api.pollQrCode(qrKey)
                    if (pollRes.code == 0) {
                        when (pollRes.data.code) {
                            0 -> { // 登录成功
                                tipText.text = "登录成功！"
                                saveCookiesFromUrl(pollRes.data.url)
                                delay(1000)
                                finish() // 关闭登录页
                                return@launch
                            }
                            86090 -> tipText.text = "已扫码，请在手机上确认"
                            86038 -> {
                                tipText.text = "二维码已过期，请重试"
                                return@launch
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                tipText.text = "网络异常: ${e.message}"
            }
        }
    }

    private fun generateQrCode(content: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 300, 300)
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.RGB_565) // 使用 RGB_565 省内存
        for (x in 0 until 300) {
            for (y in 0 until 300) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bitmap
    }

    // 从 B 站返回的跳转链接中提取 Cookie
    private fun saveCookiesFromUrl(url: String) {
        try {
            // B站返回的 url 类似于: https://passport.biligame.com/crossDomain?DedeUserID=xxx&SESSDATA=xxx&bili_jct=xxx...
            val query = url.substringAfter("?")
            val params = query.split("&")
            val cookieBuilder = StringBuilder()
            
            for (param in params) {
                if (param.startsWith("SESSDATA=") || param.startsWith("bili_jct=") || param.startsWith("DedeUserID=")) {
                    cookieBuilder.append(param).append("; ")
                }
            }
            // 必须补充一个 buvid3，不然部分接口不认
            cookieBuilder.append("buvid3=infoc;")
            
            UserPrefs.saveCookie(this, cookieBuilder.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}