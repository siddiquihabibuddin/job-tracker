package com.jobtracker.android

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.jobtracker.android.core.domain.model.ParseApplicationRequest
import com.jobtracker.android.feature.applications.create.SmartCreateApi
import com.jobtracker.android.feature.applications.create.SmartCreateRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class SmartCreateApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: SmartCreateApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        api = Retrofit.Builder()
            .baseUrl(server.url("/v1/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SmartCreateApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `parse populates non-null fields and ignores nulls`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{
                        "company": "Stripe",
                        "role": "Senior Backend Engineer",
                        "status": "APPLIED",
                        "source": "LinkedIn",
                        "salaryMin": 200000,
                        "salaryMax": 240000,
                        "currency": "USD",
                        "jobLink": "https://stripe.com/jobs/123",
                        "tags": ["backend", "remote"]
                    }""".trimIndent()
                )
        )

        val parsed = api.parse(ParseApplicationRequest("Applied to Stripe senior backend, 200-240k via LinkedIn"))

        assertEquals("Stripe", parsed.company)
        assertEquals("Senior Backend Engineer", parsed.role)
        assertEquals("APPLIED", parsed.status)
        assertEquals(200000.0, parsed.salaryMin!!, 0.01)
        assertEquals(240000.0, parsed.salaryMax!!, 0.01)
        assertEquals(listOf("backend", "remote"), parsed.tags)
        assertEquals(null, parsed.location)
        assertEquals(null, parsed.appliedAt)
    }

    @Test
    fun `repository wraps 503 as Result-failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        val repo = SmartCreateRepository(api)
        val result = repo.parse("anything")
        assertTrue(result.isFailure)
    }
}
