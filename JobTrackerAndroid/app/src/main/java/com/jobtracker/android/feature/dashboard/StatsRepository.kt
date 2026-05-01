package com.jobtracker.android.feature.dashboard

import com.jobtracker.android.core.domain.model.CompanyCount
import com.jobtracker.android.core.domain.model.Insights
import com.jobtracker.android.core.domain.model.StaleApp
import com.jobtracker.android.core.domain.model.StatsSummary
import com.jobtracker.android.core.domain.model.TrendResponse

class StatsRepository(
    private val api: StatsApi,
) {
    suspend fun summary(window: String = "30d"): StatsSummary = api.summary(window)
    suspend fun trend(period: String = "week", weeks: Int = 12): TrendResponse =
        api.trend(period, weeks)

    suspend fun insights(): Insights = api.insights()
    suspend fun companies(): List<CompanyCount> = api.companies()
    suspend fun stale(): List<StaleApp> = api.stale()
}
