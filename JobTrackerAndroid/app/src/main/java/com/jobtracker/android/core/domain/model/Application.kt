package com.jobtracker.android.core.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Application(
    val id: String,
    val company: String,
    val role: String,
    val status: AppStatus,
    val source: String? = null,
    val location: String? = null,
    val salaryMin: Double? = null,
    val salaryMax: Double? = null,
    val currency: String? = null,
    val nextFollowUpOn: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val appliedAt: String? = null,
    val jobLink: String? = null,
    val resumeUploaded: String? = null,
    val gotCall: Boolean = false,
    val rejectDate: String? = null,
    val loginDetails: String? = null,
)

@Serializable
data class PageResponse<T>(
    val items: List<T>,
    val limit: Int,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
)

@Serializable
data class CreateApplicationRequest(
    val company: String,
    val role: String,
    val status: AppStatus,
    val source: String? = null,
    val location: String? = null,
    val salaryMin: Double? = null,
    val salaryMax: Double? = null,
    val currency: String? = null,
    val nextFollowUpOn: String? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val appliedAt: String? = null,
    val jobLink: String? = null,
    val resumeUploaded: String? = null,
    val gotCall: Boolean = false,
    val rejectDate: String? = null,
    val loginDetails: String? = null,
)

@Serializable
data class UpdateApplicationRequest(
    val company: String? = null,
    val role: String? = null,
    val status: AppStatus? = null,
    val source: String? = null,
    val location: String? = null,
    val salaryMin: Double? = null,
    val salaryMax: Double? = null,
    val currency: String? = null,
    val nextFollowUpOn: String? = null,
    val tags: List<String>? = null,
    val appliedAt: String? = null,
    val jobLink: String? = null,
    val resumeUploaded: String? = null,
    val gotCall: Boolean? = null,
    val rejectDate: String? = null,
    val loginDetails: String? = null,
)

@Serializable
data class ApiError(
    val error: String? = null,
    @SerialName("details") val details: List<String>? = null,
)
