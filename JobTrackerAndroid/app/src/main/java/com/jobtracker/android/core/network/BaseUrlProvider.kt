package com.jobtracker.android.core.network

import android.content.Context
import com.jobtracker.android.BuildConfig

class BaseUrlProvider(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun current(): String {
        if (!BuildConfig.DEBUG) return BuildConfig.API_BASE_URL
        val override = prefs.getString(KEY_OVERRIDE, null)?.trim().orEmpty()
        return if (override.isNotEmpty()) ensureTrailingSlash(override) else BuildConfig.API_BASE_URL
    }

    fun setOverride(url: String?) {
        prefs.edit().apply {
            if (url.isNullOrBlank()) remove(KEY_OVERRIDE) else putString(KEY_OVERRIDE, url.trim())
        }.apply()
    }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    private companion object {
        const val FILE = "jt_base_url"
        const val KEY_OVERRIDE = "override"
    }
}
