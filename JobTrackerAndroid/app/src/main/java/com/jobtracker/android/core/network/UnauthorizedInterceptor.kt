package com.jobtracker.android.core.network

import android.os.Handler
import android.os.Looper
import com.jobtracker.android.core.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class UnauthorizedInterceptor(private val sessionManager: SessionManager) : Interceptor {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401 && !request.url.encodedPath.contains("/auth/")) {
            mainHandler.post { sessionManager.invalidate() }
        }
        return response
    }
}
