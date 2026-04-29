package com.jobtracker.android

import com.jobtracker.android.core.network.IdempotencyInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class IdempotencyInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        client = OkHttpClient.Builder()
            .addInterceptor(IdempotencyInterceptor())
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `injects Idempotency-Key on POST applications`() {
        server.enqueue(MockResponse().setResponseCode(201))
        val req = Request.Builder()
            .url(server.url("/v1/applications"))
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().close()
        val recorded = server.takeRequest()
        val key = recorded.getHeader("Idempotency-Key")
        assertEquals(36, key?.length)
    }

    @Test
    fun `omits Idempotency-Key on GET`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val req = Request.Builder()
            .url(server.url("/v1/applications"))
            .get()
            .build()
        client.newCall(req).execute().close()
        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Idempotency-Key"))
    }

    @Test
    fun `omits Idempotency-Key on auth endpoints`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val req = Request.Builder()
            .url(server.url("/v1/auth/token"))
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().close()
        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Idempotency-Key"))
    }

    @Test
    fun `each mutation gets a distinct key`() {
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))
        val build = {
            Request.Builder()
                .url(server.url("/v1/applications/abc"))
                .patch("{}".toRequestBody("application/json".toMediaType()))
                .build()
        }
        client.newCall(build()).execute().close()
        client.newCall(build()).execute().close()
        val first = server.takeRequest().getHeader("Idempotency-Key")
        val second = server.takeRequest().getHeader("Idempotency-Key")
        assertNotEquals(first, second)
    }
}
