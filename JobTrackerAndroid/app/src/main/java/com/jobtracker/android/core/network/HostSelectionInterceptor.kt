package com.jobtracker.android.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites every request's scheme/host/port to whatever `baseUrl()` returns.
 * Lets us hot-swap the API endpoint at runtime (debug override) without rebuilding Retrofit.
 * Path is preserved from the request, so the Retrofit base URL still defines the prefix.
 */
class HostSelectionInterceptor(
    private val baseUrl: () -> String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val target = baseUrl().toHttpUrlOrNull() ?: return chain.proceed(request)
        val newUrl = request.url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .build()
        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
