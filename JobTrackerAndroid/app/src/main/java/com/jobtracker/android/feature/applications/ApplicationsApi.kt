package com.jobtracker.android.feature.applications

import com.jobtracker.android.core.domain.model.Application
import com.jobtracker.android.core.domain.model.CreateApplicationRequest
import com.jobtracker.android.core.domain.model.PageResponse
import com.jobtracker.android.core.domain.model.UpdateApplicationRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApplicationsApi {

    @GET("applications")
    suspend fun list(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null,
        @Query("gotCall") gotCall: Boolean? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 50,
    ): PageResponse<Application>

    @GET("applications/{id}")
    suspend fun get(@Path("id") id: String): Application

    @POST("applications")
    suspend fun create(@Body body: CreateApplicationRequest): Application

    @PATCH("applications/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdateApplicationRequest): Application

    @DELETE("applications/{id}")
    suspend fun delete(@Path("id") id: String)
}
