package com.jobtracker.android.core.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ActivityItem(
    val id: String,
    val eventType: String,
    val message: String,
    val occurredAt: String,
)
