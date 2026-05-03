package com.jobtracker.android.core.auth

import com.jobtracker.android.core.data.prefs.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(private val tokenStore: TokenStore) {

    private val _session = MutableStateFlow(tokenStore.read())
    val session: StateFlow<TokenStore.Session?> = _session.asStateFlow()

    fun current(): TokenStore.Session? = _session.value

    fun setSession(session: TokenStore.Session) {
        tokenStore.write(session)
        _session.value = session
    }

    fun invalidate() {
        if (_session.value == null) return
        tokenStore.clear()
        _session.value = null
    }
}
