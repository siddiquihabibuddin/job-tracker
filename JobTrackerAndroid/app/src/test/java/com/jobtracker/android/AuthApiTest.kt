package com.jobtracker.android

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.jobtracker.android.core.auth.AuthApi
import com.jobtracker.android.core.auth.LoginRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class AuthApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AuthApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        api = Retrofit.Builder()
            .baseUrl(server.url("/v1/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login parses response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"token":"abc","email":"u@x.com","userId":"00000000-0000-0000-0000-000000000001","displayName":"U"}"""
            )
        )

        val response = api.login(LoginRequest("u@x.com", "secretpw"))

        assertEquals("abc", response.token)
        assertEquals("u@x.com", response.email)
        assertEquals("00000000-0000-0000-0000-000000000001", response.userId)
    }
}
