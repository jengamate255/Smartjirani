package com.smartjirani.feature.auth

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.network.SupabaseClientProvider
import com.smartjirani.core.security.SecureTokenStorage
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
        private val preferencesRepository: UserPreferencesRepository,
        private val secureTokenStorage: SecureTokenStorage,
    ) : AuthRepository {
        override val currentUserId: String?
            get() =
                supabaseClient.auth
                    .currentSessionOrNull()
                    ?.user
                    ?.id

        override val isLoggedIn: Boolean
            get() = supabaseClient.auth.currentSessionOrNull() != null

        override suspend fun register(
            email: String,
            password: String,
        ): Resource<AuthUser> =
            safeCall {
                val result =
                    supabaseClient.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }
                val user =
                    result?.let {
                        AuthUser(
                            id = it.id,
                            email = it.email,
                        )
                    } ?: supabaseClient.auth.currentUserOrNull()?.let {
                        AuthUser(
                            id = it.id,
                            email = it.email,
                        )
                    } ?: throw Exception("Registration failed")

                if (supabaseClient.auth.currentSessionOrNull() != null) {
                    preferencesRepository.setUserId(user.id)
                    secureTokenStorage.saveUserId(user.id)
                }
                user
            }

        override suspend fun login(
            email: String,
            password: String,
        ): Resource<AuthUser> =
            safeCall {
                supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                val user = supabaseClient.auth.currentUserOrNull() ?: throw Exception("Login failed")
                val authUser =
                    AuthUser(
                        id = user.id,
                        email = user.email,
                    )
                preferencesRepository.setUserId(authUser.id)
                secureTokenStorage.saveUserId(authUser.id)
                authUser
            }

        override suspend fun logout(): Resource<Unit> =
            safeCall {
                supabaseClient.auth.signOut()
                preferencesRepository.clearAll()
                secureTokenStorage.clearAll()
            }

        override suspend fun resetPassword(email: String): Resource<Unit> =
            safeCall {
                supabaseClient.auth.resetPasswordForEmail(email)
            }

        override suspend fun restoreSession(): Resource<AuthUser?> =
            safeCall {
                awaitRestoredUser()
            }

        override suspend fun getCurrentUser(): AuthUser? = awaitRestoredUser()

        /**
         * Session storage loads asynchronously after process start; reading
         * `currentSessionOrNull()` during that window returns null and would wrongly
         * report "logged out" (bouncing the shell to Login / skipping shell
         * reconciliation). Wait until the status leaves [SessionStatus.LoadingFromStorage].
         */
        private suspend fun awaitRestoredUser(): AuthUser? {
            val status =
                kotlinx.coroutines.withTimeoutOrNull(4_000) {
                    supabaseClient.auth.sessionStatus.first { it !is SessionStatus.LoadingFromStorage }
                }
            val user =
                (status as? SessionStatus.Authenticated)?.session?.user
                    ?: supabaseClient.auth.currentSessionOrNull()?.user
                    ?: return null
            return AuthUser(id = user.id, email = user.email)
        }
    }
