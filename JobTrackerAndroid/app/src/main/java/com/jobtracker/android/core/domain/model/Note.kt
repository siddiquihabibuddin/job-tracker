package com.jobtracker.android.core.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Note(
    val id: Long,
    val body: String,
    val createdAt: String,
)

@Serializable
data class AddNoteRequest(val body: String)

@Serializable
data class UpdateNoteRequest(val content: String)
