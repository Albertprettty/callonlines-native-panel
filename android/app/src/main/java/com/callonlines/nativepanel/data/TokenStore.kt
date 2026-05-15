package com.callonlines.nativepanel.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class TokenStore(context: Context) {

    private val app = context.applicationContext
    private val prefs: SharedPreferences by lazy { createPrefs() }

    private val _sessionEnded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionEnded: Flow<Unit> = _sessionEnded.asSharedFlow()

    init {
        migratePlaintextIfNeeded()
    }

    private fun createPrefs(): SharedPreferences = try {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_NAME_SECURE,
            masterKey,
            app,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        Log.w(TAG, "EncryptedSharedPreferences no disponible; usando almacenamiento clásico", e)
        app.getSharedPreferences(PREFS_NAME_LEGACY, Context.MODE_PRIVATE)
    }

    /** Si ya hay JWT cifrado, no hace nada. Si no, copia desde prefs legadas y borra la copia en claro. */
    private fun migratePlaintextIfNeeded() {
        if (prefs.getString(KEY_JWT, null) != null) return
        val legacy = app.getSharedPreferences(PREFS_NAME_LEGACY, Context.MODE_PRIVATE)
        val token = legacy.getString(KEY_JWT, null) ?: return
        prefs.edit().putString(KEY_JWT, token).apply()
        legacy.edit().remove(KEY_JWT).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_JWT, null)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_JWT, token).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_JWT).apply()
    }

    /** Llamado cuando la API responde 401: borra token y avisa para volver al login. */
    fun endSessionDueToUnauthorized() {
        if (getToken() == null) return
        clear()
        _sessionEnded.tryEmit(Unit)
    }

    companion object {
        private const val TAG = "TokenStore"
        private const val KEY_JWT = "jwt"
        private const val PREFS_NAME_SECURE = "callonlines_panel_secure"
        private const val PREFS_NAME_LEGACY = "callonlines_panel"
    }
}
