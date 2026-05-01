package com.jobtracker.android

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.jobtracker.android.core.domain.model.AddNoteRequest
import com.jobtracker.android.core.domain.model.UpdateNoteRequest
import com.jobtracker.android.core.network.IdempotencyInterceptor
import com.jobtracker.android.feature.applications.detail.NotesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class NotesApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: NotesApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        api = Retrofit.Builder()
            .baseUrl(server.url("/v1/"))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(IdempotencyInterceptor())
                    .build()
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NotesApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `addNote sends body and parses NoteDto`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"id":42,"body":"Followed up via email","createdAt":"2026-04-29T10:00:00Z"}""")
        )

        val response = api.addNote("aaa-bbb", AddNoteRequest("Followed up via email"))

        assertEquals(42L, response.id)
        assertEquals("Followed up via email", response.body)
        assertEquals("2026-04-29T10:00:00Z", response.createdAt)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/applications/aaa-bbb/notes", recorded.path)
        assertNotNull(recorded.getHeader("Idempotency-Key"))
    }

    @Test
    fun `updateNote sends content body to PATCH endpoint`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"id":42,"body":"Edited","createdAt":"2026-04-29T10:00:00Z"}""")
        )

        val response = api.updateNote("aaa-bbb", 42L, UpdateNoteRequest("Edited"))

        assertEquals("Edited", response.body)
        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/v1/applications/aaa-bbb/notes/42", recorded.path)
        assertNotNull(recorded.getHeader("Idempotency-Key"))
    }
}
