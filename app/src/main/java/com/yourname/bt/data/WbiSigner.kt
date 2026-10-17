package com.yourname.bt.data

import java.net.URLEncoder
import java.security.MessageDigest

object WbiSigner {
    // B站固定的64位混淆重排表
    private val MIXIN_KEY_ENC_TAB = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    )

    // 根据 img_key 和 sub_key 生成 mixinKey
    private fun getMixinKey(imgKey: String, subKey: String): String {
        val rawWbiKey = imgKey + subKey
        val sb = StringBuilder()
        // 按混淆表重排，取前32位
        for (i in 0 until 32) {
            sb.append(rawWbiKey[MIXIN_KEY_ENC_TAB[i]])
        }
        return sb.toString()
    }

    // 对参数进行排序、过滤、加时间戳并计算签名
    fun sign(params: Map<String, String>, imgKey: String, subKey: String): Map<String, String> {
        val mixinKey = getMixinKey(imgKey, subKey)
        val sortedParams = params.toSortedMap() // 按键名升序排序
        val sb = StringBuilder()

        for ((key, value) in sortedParams) {
            // 过滤 value 中的特殊字符 !'()*
            val filteredValue = value.filter { it !in "!'()*" }
            // 百分号编码（注意：需要将空格编码为 %20 而非 +）
            val encodedValue = URLEncoder.encode(filteredValue, "UTF-8").replace("+", "%20")
            if (sb.isNotEmpty()) sb.append("&")
            sb.append("$key=$encodedValue")
        }

        // 加上时间戳 wts
        val wts = (System.currentTimeMillis() / 1000).toString()
        if (sb.isNotEmpty()) sb.append("&")
        sb.append("wts=$wts")

        // MD5计算 w_rid
        val wRid = md5(sb.toString() + mixinKey)

        val result = params.toMutableMap()
        result["wts"] = wts
        result["w_rid"] = wRid
        return result
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}