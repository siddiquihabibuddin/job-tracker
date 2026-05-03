package com.jobtracker.android.core.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Mirrors `ParsedApplicationDto` from ai-service. Every field is nullable
 * because the model returns null for anything it could not extract from the
 * description. Status is a String here (not AppStatus) so we tolerate
 * unrecognized values from the AI without blowing up parsing.
 */
@Immutable
@Serializable
data class ParsedApplication(
    val company: String? = null,
    val role: String? = null,
    val status: String? = null,
    val source: String? = null,
    val location: String? = null,
    val salaryMin: Double? = null,
    val salaryMax: Double? = null,
    val currency: String? = null,
    val appliedAt: String? = null,
    val nextFollowUpOn: String? = null,
    val jobLink: String? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
)

@Serializable
data class ParseApplicationRequest(val description: String)
