package com.jobtracker.android.feature.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.android.core.domain.model.CompanyCount
import com.jobtracker.android.core.domain.model.Insights
import com.jobtracker.android.core.domain.model.StaleApp
import com.jobtracker.android.core.domain.model.StatsSummary
import com.jobtracker.android.core.domain.model.TrendResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: StatsRepository,
) : ViewModel() {

    @Immutable
    data class UiState(
        val summary: StatsSummary? = null,
        val trend: TrendResponse? = null,
        val insights: Insights? = null,
        val companies: List<CompanyCount> = emptyList(),
        val staleApps: List<StaleApp> = emptyList(),
        val window: String = "30d",
        val loading: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { refresh() }

    fun setWindow(window: String) {
        if (_state.value.window == window) return
        _state.update { it.copy(window = window) }
        // Only summary is window-scoped; trend/insights/companies/stale are not.
        refreshSummaryOnly()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val summary = async { repository.summary(_state.value.window) }
                val trend = async { repository.trend("week", 12) }
                val insights = async { runCatching { repository.insights() }.getOrNull() }
                val companies = async { runCatching { repository.companies() }.getOrDefault(emptyList()) }
                val stale = async { runCatching { repository.stale() }.getOrDefault(emptyList()) }
                listOf(summary, trend, insights, companies, stale).awaitAll()
                Quintet(
                    summary.await(),
                    trend.await(),
                    insights.await(),
                    companies.await(),
                    stale.await(),
                )
            }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            loading = false,
                            summary = result.summary,
                            trend = result.trend,
                            insights = result.insights,
                            companies = result.companies,
                            staleApps = result.staleApps,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Failed to load stats") }
                }
        }
    }

    private fun refreshSummaryOnly() {
        viewModelScope.launch {
            runCatching { repository.summary(_state.value.window) }
                .onSuccess { summary -> _state.update { it.copy(summary = summary) } }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message ?: "Failed to refresh summary") }
                }
        }
    }

    private data class Quintet(
        val summary: StatsSummary,
        val trend: TrendResponse,
        val insights: Insights?,
        val companies: List<CompanyCount>,
        val staleApps: List<StaleApp>,
    )
}
