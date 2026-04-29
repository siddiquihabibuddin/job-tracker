package com.jobtracker.android.feature.applications.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.android.core.domain.model.AppStatus
import com.jobtracker.android.core.domain.model.Application
import com.jobtracker.android.core.domain.model.UpdateApplicationRequest
import com.jobtracker.android.feature.applications.ApplicationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ApplicationDetailViewModel(
    private val repository: ApplicationsRepository,
    private val applicationId: String,
) : ViewModel() {

    @Immutable
    data class UiState(
        val application: Application? = null,
        val refreshing: Boolean = false,
        val updating: Boolean = false,
        val deleted: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        repository.observeById(applicationId)
            .onEach { app -> _state.update { it.copy(application = app) } }
            .launchIn(viewModelScope)
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.get(applicationId) }
                .onSuccess { _state.update { it.copy(refreshing = false) } }
                .onFailure { e ->
                    _state.update { it.copy(refreshing = false, error = e.message ?: "Failed to load") }
                }
        }
    }

    fun setStatus(status: AppStatus) = patch(UpdateApplicationRequest(status = status))
    fun setGotCall(value: Boolean) = patch(UpdateApplicationRequest(gotCall = value))
    fun setRejectDate(date: String?) = patch(UpdateApplicationRequest(rejectDate = date))

    fun delete() {
        _state.update { it.copy(updating = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.delete(applicationId) }
                .onSuccess { _state.update { it.copy(updating = false, deleted = true) } }
                .onFailure { e ->
                    _state.update { it.copy(updating = false, error = e.message ?: "Delete failed") }
                }
        }
    }

    private fun patch(request: UpdateApplicationRequest) {
        _state.update { it.copy(updating = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.update(applicationId, request) }
                .onSuccess { _state.update { it.copy(updating = false) } }
                .onFailure { e ->
                    _state.update { it.copy(updating = false, error = e.message ?: "Update failed") }
                }
        }
    }
}
