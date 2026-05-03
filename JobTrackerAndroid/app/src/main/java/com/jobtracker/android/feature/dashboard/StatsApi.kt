package com.jobtracker.android.feature.dashboard

import com.jobtracker.android.core.domain.model.CompanyCount
import com.jobtracker.android.core.domain.model.Insights
import com.jobtracker.android.core.domain.model.StaleApp
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

    @GET("stats/insights")
    suspend fun insights(): Insights

    @GET("stats/companies")
    suspend fun companies(): List<CompanyCount>

    @GET("stats/stale")
    suspend fun stale(): List<StaleApp>
}
