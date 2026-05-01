package com.jobtracker.android.feature.applications.detail

import com.jobtracker.android.core.domain.model.AddNoteRequest
import com.jobtracker.android.core.domain.model.Note
import com.jobtracker.android.core.domain.model.UpdateNoteRequest
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface NotesApi {

    @POST("applications/{appId}/notes")
    suspend fun addNote(
        @Path("appId") appId: String,
        @Body body: AddNoteRequest,
    ): Note

    @PATCH("applications/{appId}/notes/{noteId}")
    suspend fun updateNote(
        @Path("appId") appId: String,
        @Path("noteId") noteId: Long,
        @Body body: UpdateNoteRequest,
    ): Note
}
