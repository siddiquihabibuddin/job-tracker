package com.jobtracker.android.core.auth

import com.jobtracker.android.core.data.prefs.TokenStore

class AuthRepository(
    private val api: AuthApi,
    private val sessionManager: SessionManager,
) {

    suspend fun login(email: String, password: String): Result<TokenStore.Session> = runCatching {
        val response = api.login(LoginRequest(email.trim(), password))
        response.toSession().also(sessionManager::setSession)
    }

    suspend fun register(email: String, password: String, displayName: String?): Result<TokenStore.Session> = runCatching {
        val response = api.register(RegisterRequest(email.trim(), password, displayName?.takeIf { it.isNotBlank() }))
        response.toSession().also(sessionManager::setSession)
    }

    fun signOut() = sessionManager.invalidate()

    private fun AuthResponse.toSession() = TokenStore.Session(
        token = token,
        email = email,
        userId = userId,
        displayName = displayName,
    )
}
