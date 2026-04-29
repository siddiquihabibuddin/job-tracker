package com.jobtracker.android.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

class IdempotencyInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!shouldInject(request.method, request.url.encodedPath)) return chain.proceed(request)
        if (request.header(HEADER) != null) return chain.proceed(request)
        val withKey = request.newBuilder()
            .header(HEADER, UUID.randomUUID().toString())
            .build()
        return chain.proceed(withKey)
    }

    private fun shouldInject(method: String, path: String): Boolean {
        if (method != "POST" && method != "PATCH") return false
        return path.contains("/applications")
    }

    private companion object {
        const val HEADER = "Idempotency-Key"
    }
}
