package com.jobtracker.android.feature.applications.create

import com.jobtracker.android.core.domain.model.ParseApplicationRequest
import com.jobtracker.android.core.domain.model.ParsedApplication

class SmartCreateRepository(
    private val api: SmartCreateApi,
) {
    suspend fun parse(description: String): Result<ParsedApplication> = runCatching {
        api.parse(ParseApplicationRequest(description = description.trim()))
    }
}
