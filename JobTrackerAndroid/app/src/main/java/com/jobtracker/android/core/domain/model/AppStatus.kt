package com.jobtracker.android.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppStatus {
    APPLIED, PHONE, ONSITE, OFFER, REJECTED, ACCEPTED, WITHDRAWN;

    val displayName: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}
