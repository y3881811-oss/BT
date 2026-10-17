package com.yourname.bt

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.yourname.bt.data.NetworkModule
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
                val qrRes = NetworkModule.passportApi.getQrCode()
                if (qrRes.code != 0) {
                    tipText.text = "获取二维码失败"
                    return@launch
                }
                val qrUrl = qrRes.data.url
                val qrKey = qrRes.data.qrcode_key

                qrImage.setImageBitmap(generateQrCode(qrUrl))

                while (true) {
                    delay(2000)
                    val pollRes = NetworkModule.passportApi.pollQrCode(qrKey)
                    if (pollRes.code == 0) {
                        when (pollRes.data.code) {
                            0 -> { // 成功
                                tipText.text = "登录成功！"
                                saveCookiesFromUrl(pollRes.data.url)
                                delay(1000)
                                finish()
                                return@launch
                            }
                            86090 -> tipText.text = "已扫码，请在手机上确认"
                            86038 -> {
                                tipText.text = "二维码已过期，请重新打开"
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
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.RGB_565)
        for (x in 0 until 300) {
            for (y in 0 until 300) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bitmap
    }

    private fun saveCookiesFromUrl(url: String) {
        try {
            val query = url.substringAfter("?")
            val params = query.split("&")
            val cookieBuilder = StringBuilder()
            for (param in params) {
                if (param.startsWith("SESSDATA=") || param.startsWith("bili_jct=") || param.startsWith("DedeUserID=")) {
                    cookieBuilder.append(param).append("; ")
                }
            }
            cookieBuilder.append("buvid3=infoc;")
            UserPrefs.saveCookie(this, cookieBuilder.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}