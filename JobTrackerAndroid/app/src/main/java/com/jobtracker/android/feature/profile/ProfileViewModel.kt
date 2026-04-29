package com.jobtracker.android.feature.profile

import androidx.lifecycle.ViewModel
import com.jobtracker.android.core.auth.SessionManager
import com.jobtracker.android.core.data.prefs.TokenStore
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {

    val session: StateFlow<TokenStore.Session?> = sessionManager.session

    fun signOut() = sessionManager.invalidate()
}
