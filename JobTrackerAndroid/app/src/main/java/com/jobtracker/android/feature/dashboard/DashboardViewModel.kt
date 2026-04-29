package com.jobtracker.android.feature.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val summary = async { repository.summary(_state.value.window) }
                val trend = async { repository.trend("week", 12) }
                val results = listOf(summary, trend).awaitAll()
                @Suppress("UNCHECKED_CAST")
                (results[0] as StatsSummary) to (results[1] as TrendResponse)
            }
                .onSuccess { (summary, trend) ->
                    _state.update { it.copy(loading = false, summary = summary, trend = trend) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Failed to load stats") }
                }
        }
    }
}
