package com.jobtracker.android

import com.jobtracker.android.core.network.HostSelectionInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HostSelectionInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `rewrites scheme host port from base URL`() {
        val target = server.url("/").toString()
        val client = OkHttpClient.Builder()
            .addInterceptor(HostSelectionInterceptor { target })
            .build()
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val req = Request.Builder()
            .url("http://placeholder.invalid/v1/applications")
            .get()
            .build()
        client.newCall(req).execute().close()

        val recorded = server.takeRequest()
        assertEquals("/v1/applications", recorded.path)
    }
}
