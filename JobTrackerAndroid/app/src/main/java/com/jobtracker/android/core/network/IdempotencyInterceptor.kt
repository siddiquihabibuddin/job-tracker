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
        // Match `/v1/applications` or `/v1/applications/...` exactly — avoid matching
        // unrelated paths like `/v1/ai/applications/parse` that just happen to contain
        // the substring "/applications".
        return APPLICATIONS_PATH.containsMatchIn(path)
    }

    private companion object {
        const val HEADER = "Idempotency-Key"
        // Match `/v1/applications` exactly or `/v1/applications/...` — excludes
        // unrelated paths like `/v1/ai/applications/parse`.
        val APPLICATIONS_PATH = Regex("/v1/applications(/|$)")
    }
}
