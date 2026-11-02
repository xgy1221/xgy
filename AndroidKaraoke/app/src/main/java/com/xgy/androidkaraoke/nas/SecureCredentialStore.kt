package com.xgy.androidkaraoke.nas

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * NAS 用户名/密码仅存加密 SharedPreferences。
 * 禁止写入普通 prefs / 源码 / 文档。
 */
class SecureCredentialStore(context: Context) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (_: Exception) {
        // Fallback only for broken keystore on some TV firmwares; still private mode.
        context.getSharedPreferences("${FILE_NAME}_fallback", Context.MODE_PRIVATE)
    }

    fun username(): String = prefs.getString(KEY_USER, "") ?: ""
    fun password(): String = prefs.getString(KEY_PASS, "") ?: ""

    fun save(username: String, password: String) {
        prefs.edit()
            .putString(KEY_USER, username)
            .putString(KEY_PASS, password)
            .apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_USER).remove(KEY_PASS).apply()
    }

    companion object {
        private const val FILE_NAME = "nas_secure_creds"
        private const val KEY_USER = "username"
        private const val KEY_PASS = "password"
    }
}
