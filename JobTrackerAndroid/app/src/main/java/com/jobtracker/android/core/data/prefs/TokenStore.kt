package com.jobtracker.android.core.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val appCtx = context.applicationContext
        val masterKey = MasterKey.Builder(appCtx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appCtx,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun read(): Session? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return Session(
            token = token,
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
            userId = prefs.getString(KEY_USER_ID, "").orEmpty(),
            displayName = prefs.getString(KEY_DISPLAY_NAME, null),
        )
    }

    fun write(session: Session) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    data class Session(
        val token: String,
        val email: String,
        val userId: String,
        val displayName: String?,
    )

    private companion object {
        const val FILE = "jt_secure_prefs"
        const val KEY_TOKEN = "token"
        const val KEY_EMAIL = "email"
        const val KEY_USER_ID = "user_id"
        const val KEY_DISPLAY_NAME = "display_name"
    }
}
