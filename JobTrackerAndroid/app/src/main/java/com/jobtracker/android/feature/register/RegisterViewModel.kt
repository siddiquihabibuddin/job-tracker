package com.jobtracker.android.feature.register

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.android.core.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val auth: AuthRepository,
) : ViewModel() {

    @Immutable
    data class UiState(
        val email: String = "",
        val password: String = "",
        val displayName: String = "",
        val submitting: Boolean = false,
        val error: String? = null,
        val success: Boolean = false,
    ) {
        val canSubmit: Boolean get() = !submitting &&
            email.isNotBlank() &&
            password.length in 8..72
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onEmailChange(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onDisplayNameChange(v: String) = _state.update { it.copy(displayName = v, error = null) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            auth.register(s.email, s.password, s.displayName)
                .onSuccess { _state.update { it.copy(submitting = false, success = true) } }
                .onFailure { e ->
                    _state.update { it.copy(submitting = false, error = e.message ?: "Registration failed") }
                }
        }
    }
}
