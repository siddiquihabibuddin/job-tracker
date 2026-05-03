package com.jobtracker.android.feature.applications.detail

import com.jobtracker.android.core.domain.model.ActivityItem
import com.jobtracker.android.core.domain.model.AddNoteRequest
import com.jobtracker.android.core.domain.model.Note
import com.jobtracker.android.core.domain.model.UpdateNoteRequest

class NotesAndActivityRepository(
    private val notesApi: NotesApi,
    private val activityApi: ActivityApi,
) {

    suspend fun addNote(appId: String, body: String): Note =
        notesApi.addNote(appId, AddNoteRequest(body = body.trim()))

    suspend fun updateNote(appId: String, noteId: Long, content: String): Note =
        notesApi.updateNote(appId, noteId, UpdateNoteRequest(content = content.trim()))

    suspend fun loadActivity(appId: String): List<ActivityItem> =
        activityApi.activity(appId)
}
