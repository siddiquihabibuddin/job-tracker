package com.jobtracker.android.core.auth

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/token")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse
}

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val email: String, val password: String, val displayName: String? = null)

@Serializable
data class AuthResponse(
    val token: String,
    val email: String,
    val userId: String,
    val displayName: String? = null,
)
