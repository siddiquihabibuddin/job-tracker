package com.jobtracker.android.feature.login

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.android.core.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val auth: AuthRepository,
) : ViewModel() {

    @Immutable
    data class UiState(
        val email: String = "",
        val password: String = "",
        val submitting: Boolean = false,
        val error: String? = null,
        val success: Boolean = false,
    ) {
        val canSubmit: Boolean get() = !submitting && email.isNotBlank() && password.isNotBlank()
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            auth.login(current.email, current.password)
                .onSuccess { _state.update { it.copy(submitting = false, success = true) } }
                .onFailure { e ->
                    _state.update { it.copy(submitting = false, error = e.message ?: "Login failed") }
                }
        }
    }
}
