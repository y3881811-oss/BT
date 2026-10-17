 package com.yourname.bt.data

import android.content.Context

object UserPrefs {
    private const val PREF_NAME = "bt_user_prefs"
    private const val KEY_COOKIE = "cookie"

    fun saveCookie(context: Context, cookie: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_COOKIE, cookie).apply()
    }

    fun getCookie(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_COOKIE, "") ?: ""
    }
}