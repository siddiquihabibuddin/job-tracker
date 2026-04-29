package com.jobtracker.android.feature.profile

import androidx.lifecycle.ViewModel
import com.jobtracker.android.core.network.BaseUrlProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DebugOverrideViewModel(
    private val baseUrlProvider: BaseUrlProvider,
) : ViewModel() {

    data class UiState(
        val current: String = "",
        val draft: String = "",
        val saved: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState(current = baseUrlProvider.current(), draft = baseUrlProvider.current()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onDraft(value: String) = _state.update { it.copy(draft = value, saved = false) }

    fun save() {
        baseUrlProvider.setOverride(_state.value.draft)
        _state.update { it.copy(current = baseUrlProvider.current(), saved = true) }
    }

    fun reset() {
        baseUrlProvider.setOverride(null)
        val current = baseUrlProvider.current()
        _state.update { it.copy(current = current, draft = current, saved = true) }
    }
}
