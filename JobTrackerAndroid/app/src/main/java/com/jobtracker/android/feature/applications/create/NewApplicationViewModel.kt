package com.jobtracker.android.feature.applications.create

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.android.core.domain.model.AppStatus
import com.jobtracker.android.core.domain.model.CreateApplicationRequest
import com.jobtracker.android.feature.applications.ApplicationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewApplicationViewModel(
    private val repository: ApplicationsRepository,
) : ViewModel() {

    @Immutable
    data class UiState(
        val company: String = "",
        val role: String = "",
        val status: AppStatus = AppStatus.APPLIED,
        val source: String = "",
        val location: String = "",
        val appliedAt: String = "",
        val salaryMin: String = "",
        val salaryMax: String = "",
        val currency: String = "",
        val jobLink: String = "",
        val gotCall: Boolean = false,
        val notes: String = "",
        val submitting: Boolean = false,
        val error: String? = null,
        val created: Boolean = false,
    ) {
        val canSubmit: Boolean get() = !submitting && company.isNotBlank() && role.isNotBlank()
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onField(update: UiState.() -> UiState) = _state.update { it.update().copy(error = null) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            runCatching {
                repository.create(
                    CreateApplicationRequest(
                        company = s.company.trim(),
                        role = s.role.trim(),
                        status = s.status,
                        source = s.source.takeIf { it.isNotBlank() },
                        location = s.location.takeIf { it.isNotBlank() },
                        appliedAt = s.appliedAt.takeIf { it.isNotBlank() },
                        salaryMin = s.salaryMin.toDoubleOrNull(),
                        salaryMax = s.salaryMax.toDoubleOrNull(),
                        currency = s.currency.takeIf { it.isNotBlank() },
                        jobLink = s.jobLink.takeIf { it.isNotBlank() },
                        gotCall = s.gotCall,
                        notes = s.notes.takeIf { it.isNotBlank() },
                    )
                )
            }
                .onSuccess { _state.update { it.copy(submitting = false, created = true) } }
                .onFailure { e ->
                    _state.update { it.copy(submitting = false, error = e.message ?: "Create failed") }
                }
        }
    }
}
