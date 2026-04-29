package com.jobtracker.android.feature.dashboard

import com.jobtracker.android.core.domain.model.StatsSummary
import com.jobtracker.android.core.domain.model.TrendResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface StatsApi {

    @GET("stats/summary")
    suspend fun summary(@Query("window") window: String = "30d"): StatsSummary

    @GET("stats/trend")
    suspend fun trend(
        @Query("period") period: String = "week",
        @Query("weeks") weeks: Int = 12,
    ): TrendResponse
}
