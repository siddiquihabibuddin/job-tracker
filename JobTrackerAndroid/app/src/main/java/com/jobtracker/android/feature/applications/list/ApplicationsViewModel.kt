package com.jobtracker.android.feature.applications.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.android.core.domain.model.AppStatus
import com.jobtracker.android.core.domain.model.Application
import com.jobtracker.android.feature.applications.ApplicationsRepository
import com.jobtracker.android.feature.applications.ApplicationsRepository.Filters
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ApplicationsViewModel(
    private val repository: ApplicationsRepository,
) : ViewModel() {

    @Immutable
    data class UiState(
        val items: List<Application> = emptyList(),
        val filters: Filters = Filters(),
        val refreshing: Boolean = false,
        val loadingMore: Boolean = false,
        val page: Int = 0,
        val totalPages: Int = 1,
        val totalElements: Long = 0,
        val error: String? = null,
        val primedFromCache: Boolean = false,
    ) {
        val endReached: Boolean get() = page + 1 >= totalPages
    }

    private val _filters = MutableStateFlow(Filters())
    private val _ui = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _ui.asStateFlow()

    private var refreshJob: Job? = null

    init {
        primeFromCache()

        // Debounce only the *refresh trigger*. UI state mirrors the raw filters
        // synchronously via the setters below, so the search field stays responsive.
        _filters
            .map { it.copy(search = it.search.trim()) }
            .distinctUntilChanged()
            .debounce { f -> if (f.search.isNotEmpty()) 300L else 0L }
            .onEach { refresh() }
            .launchIn(viewModelScope)
    }

    private fun primeFromCache() {
        viewModelScope.launch {
            val items = repository.observeAll().first()
            if (items.isNotEmpty()) {
                _ui.update { it.copy(items = items, primedFromCache = true) }
            }
        }
    }

    fun setStatus(status: AppStatus?) = updateFilters { it.copy(status = status) }
    fun setSearch(value: String) = updateFilters { it.copy(search = value) }
    fun setMonth(month: Int?) = updateFilters { it.copy(month = month) }
    fun setYear(year: Int?) = updateFilters { it.copy(year = year) }
    fun setGotCall(gotCall: Boolean?) = updateFilters { it.copy(gotCall = gotCall) }
    fun setSortBy(sortBy: String) = updateFilters { it.copy(sortBy = sortBy) }
    fun clearFilters() = updateFilters { Filters() }

    private inline fun updateFilters(transform: (Filters) -> Filters) {
        val next = transform(_filters.value)
        _filters.value = next
        _ui.update { it.copy(filters = next) }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _ui.update { it.copy(refreshing = true, error = null) }
            runCatching { repository.fetchPage(_filters.value, page = 0) }
                .onSuccess { page ->
                    _ui.update {
                        it.copy(
                            items = page.items,
                            refreshing = false,
                            page = page.page,
                            totalPages = page.totalPages,
                            totalElements = page.totalElements,
                        )
                    }
                }
                .onFailure { e ->
                    _ui.update { it.copy(refreshing = false, error = e.message ?: "Failed to load") }
                }
        }
    }

    fun loadMore() {
        val current = _ui.value
        if (current.loadingMore || current.refreshing || current.endReached) return
        viewModelScope.launch {
            _ui.update { it.copy(loadingMore = true, error = null) }
            runCatching { repository.fetchPage(_filters.value, page = current.page + 1) }
                .onSuccess { page ->
                    _ui.update {
                        it.copy(
                            items = it.items + page.items,
                            loadingMore = false,
                            page = page.page,
                            totalPages = page.totalPages,
                            totalElements = page.totalElements,
                        )
                    }
                }
                .onFailure { e ->
                    _ui.update { it.copy(loadingMore = false, error = e.message ?: "Failed to load more") }
                }
        }
    }
}
