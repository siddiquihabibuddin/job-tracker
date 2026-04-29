package com.jobtracker.android.feature.dashboard

import com.jobtracker.android.core.domain.model.StatsSummary
import com.jobtracker.android.core.domain.model.TrendResponse

class StatsRepository(
    private val api: StatsApi,
) {
    suspend fun summary(window: String = "30d"): StatsSummary = api.summary(window)
    suspend fun trend(period: String = "week", weeks: Int = 12): TrendResponse =
        api.trend(period, weeks)
}
