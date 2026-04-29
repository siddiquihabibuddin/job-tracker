package com.jobtracker.android.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.jobtracker.android.BuildConfig
import com.jobtracker.android.core.auth.AuthApi
import com.jobtracker.android.core.auth.SessionManager
import com.jobtracker.android.feature.applications.ApplicationsApi
import com.jobtracker.android.feature.dashboard.StatsApi
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ApiModule(
    sessionManager: SessionManager,
    baseUrlProvider: BaseUrlProvider,
) {

    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        coerceInputValues = true
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(maxIdleConnections = 5, keepAliveDuration = 5, TimeUnit.MINUTES))
        .retryOnConnectionFailure(true)
        .addInterceptor(HostSelectionInterceptor(baseUrlProvider::current))
        .addInterceptor(AuthInterceptor(sessionManager))
        .addInterceptor(IdempotencyInterceptor())
        .addInterceptor(UnauthorizedInterceptor(sessionManager))
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
            }
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(SENTINEL_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val applicationsApi: ApplicationsApi = retrofit.create(ApplicationsApi::class.java)
    val statsApi: StatsApi = retrofit.create(StatsApi::class.java)

    private companion object {
        // Real host comes from HostSelectionInterceptor at runtime; only path prefix matters here.
        const val SENTINEL_BASE_URL = "http://placeholder.invalid/v1/"
    }
}
