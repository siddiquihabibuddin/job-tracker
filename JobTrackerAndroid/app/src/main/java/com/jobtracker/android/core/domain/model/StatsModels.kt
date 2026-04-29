package com.jobtracker.android.core.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class StatsSummary(
    val windowDays: Int,
    val totalApplied: Long,
    val byStatus: Map<String, Long> = emptyMap(),
    val bySource: Map<String, Long> = emptyMap(),
    val generatedAt: String? = null,
)

@Immutable
@Serializable
data class TrendPoint(
    val start: String,
    val end: String,
    val count: Long,
    val callsCount: Long = 0,
)

@Immutable
@Serializable
data class TrendResponse(
    val period: String,
    val points: List<TrendPoint> = emptyList(),
)
