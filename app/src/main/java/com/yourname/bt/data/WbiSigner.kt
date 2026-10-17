package com.yourname.bt.data

import java.net.URLEncoder
import java.security.MessageDigest

object WbiSigner {
    private val MIXIN_KEY_ENC_TAB = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    )

    private fun getMixinKey(imgKey: String, subKey: String): String {
        val rawWbiKey = imgKey + subKey
        val sb = StringBuilder()
        for (i in 0 until 32) {
            sb.append(rawWbiKey[MIXIN_KEY_ENC_TAB[i]])
        }
        return sb.toString()
    }

    fun sign(params: Map<String, String>, imgKey: String, subKey: String): Map<String, String> {
        // 1. 剔除 w_rid 和 wts
        val filteredParams = params.filterKeys { it != "w_rid" && it != "wts" }.toMutableMap()
        val mixinKey = getMixinKey(imgKey, subKey)
        
        // 2. 排序并构建 query
        val sortedParams = filteredParams.toSortedMap()
        val sb = StringBuilder()
        for ((key, value) in sortedParams) {
            val filteredValue = value.filter { it !in "!'()*" }
            val encodedValue = URLEncoder.encode(filteredValue, "UTF-8").replace("+", "%20")
            if (sb.isNotEmpty()) sb.append("&")
            sb.append("$key=$encodedValue")
        }

        // 3. 添加当前时间戳 wts (秒级)
        val wts = (System.currentTimeMillis() / 1000).toString()
        if (sb.isNotEmpty()) sb.append("&")
        sb.append("wts=$wts")

        // 4. 计算 w_rid
        val wRid = md5(sb.toString() + mixinKey)

        val result = filteredParams.toMutableMap()
        result["wts"] = wts
        result["w_rid"] = wRid
        return result
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}