package com.smartjirani.feature.auth

import com.smartjirani.core.common.result.Resource

data class AuthUser(
    val id: String,
    val email: String? = null,
    val phone: String? = null,
)

interface AuthRepository {
    val currentUserId: String?
    val isLoggedIn: Boolean

    suspend fun register(
        email: String,
        password: String,
    ): Resource<AuthUser>

    suspend fun login(
        email: String,
        password: String,
    ): Resource<AuthUser>

    suspend fun logout(): Resource<Unit>

    suspend fun resetPassword(email: String): Resource<Unit>

    suspend fun restoreSession(): Resource<AuthUser?>

    suspend fun getCurrentUser(): AuthUser?
}
