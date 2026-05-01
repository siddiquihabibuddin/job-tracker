package com.jobtracker.android.feature.applications.detail

import com.jobtracker.android.core.domain.model.ActivityItem
import retrofit2.http.GET
import retrofit2.http.Path

interface ActivityApi {

    @GET("stats/activity/{appId}")
    suspend fun activity(@Path("appId") appId: String): List<ActivityItem>
}
