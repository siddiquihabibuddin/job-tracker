package com.jobtracker.android.feature.applications.create

import com.jobtracker.android.core.domain.model.ParseApplicationRequest
import com.jobtracker.android.core.domain.model.ParsedApplication
import retrofit2.http.Body
import retrofit2.http.POST

interface SmartCreateApi {

    @POST("ai/applications/parse")
    suspend fun parse(@Body body: ParseApplicationRequest): ParsedApplication
}
