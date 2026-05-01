package com.jobtracker.android.feature.applications.create

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.android.core.domain.model.AppStatus
import com.jobtracker.android.core.domain.model.CreateApplicationRequest
import com.jobtracker.android.core.domain.model.ParsedApplication
import com.jobtracker.android.feature.applications.ApplicationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewApplicationViewModel(
    private val repository: ApplicationsRepository,
    private val smartCreate: SmartCreateRepository,
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
        // Smart Create fields
        val description: String = "",
        val parsing: Boolean = false,
        val parseError: String? = null,
        val parseFilledOnce: Boolean = false,
    ) {
        val canSubmit: Boolean get() = !submitting && company.isNotBlank() && role.isNotBlank()
        val canParse: Boolean get() = !parsing && description.isNotBlank()
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onField(update: UiState.() -> UiState) = _state.update { it.update().copy(error = null) }

    fun onDescriptionChange(value: String) =
        _state.update { it.copy(description = value, parseError = null) }

    fun parseDescription() {
        val s = _state.value
        if (!s.canParse) return
        _state.update { it.copy(parsing = true, parseError = null) }
        viewModelScope.launch {
            smartCreate.parse(s.description)
                .onSuccess { parsed -> mergeParsedFields(parsed) }
                .onFailure {
                    _state.update {
                        it.copy(
                            parsing = false,
                            parseError = "Smart fill is unavailable. Please fill the form manually.",
                        )
                    }
                }
        }
    }

    private fun mergeParsedFields(parsed: ParsedApplication) {
        _state.update { current ->
            current.copy(
                parsing = false,
                parseError = null,
                parseFilledOnce = true,
                company = parsed.company?.takeIf { it.isNotBlank() } ?: current.company,
                role = parsed.role?.takeIf { it.isNotBlank() } ?: current.role,
                status = parsed.status?.let { runCatching { AppStatus.valueOf(it) }.getOrNull() } ?: current.status,
                source = parsed.source?.takeIf { it.isNotBlank() } ?: current.source,
                location = parsed.location?.takeIf { it.isNotBlank() } ?: current.location,
                appliedAt = parsed.appliedAt ?: current.appliedAt,
                salaryMin = parsed.salaryMin?.toLong()?.toString() ?: current.salaryMin,
                salaryMax = parsed.salaryMax?.toLong()?.toString() ?: current.salaryMax,
                currency = parsed.currency?.takeIf { it.isNotBlank() } ?: current.currency,
                jobLink = parsed.jobLink?.takeIf { it.isNotBlank() } ?: current.jobLink,
                notes = parsed.notes?.takeIf { it.isNotBlank() } ?: current.notes,
            )
        }
    }

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
