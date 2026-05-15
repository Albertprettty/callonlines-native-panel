package com.callonlines.nativepanel.data

import android.content.Context

class TokenStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("callonlines_panel", Context.MODE_PRIVATE)

    fun getToken(): String? = prefs.getString(KEY_JWT, null)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_JWT, token).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_JWT).apply()
    }

    companion object {
        private const val KEY_JWT = "jwt"
    }
}
