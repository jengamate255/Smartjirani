package com.smartjirani.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.notifications.FcmTokenManager
import com.smartjirani.feature.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    /** True once [AuthRepository.restoreSession] has answered, so splash never guesses. */
    val isSessionResolved: Boolean = false,
    val isRegistered: Boolean = false,
    val requiresEmailConfirmation: Boolean = false,
    val isPasswordResetSent: Boolean = false,
    /** One-shot: logout finished successfully; the shell observes this and routes to Login. */
    val logoutCompleted: Boolean = false,
)

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val fcmTokenManager: FcmTokenManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AuthUiState())
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

        init {
            checkAuthState()
        }

        private fun checkAuthState() {
            viewModelScope.launch {
                val result = authRepository.restoreSession()
                val user = (result as? Resource.Success)?.data
                if (user != null) {
                    _uiState.value = _uiState.value.copy(isLoggedIn = true)
                    fcmTokenManager.registerToken(user.id)
                }
                _uiState.value = _uiState.value.copy(isSessionResolved = true)
            }
        }

        fun login(
            email: String,
            password: String,
        ) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                when (val result = authRepository.login(email, password)) {
                    is Resource.Success -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                isLoggedIn = true,
                            )
                        result.data?.let { user ->
                            fcmTokenManager.registerToken(user.id)
                        }
                    }

                    is Resource.Error -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                error = result.message,
                            )
                    }

                    is Resource.Loading -> {}
                }
            }
        }

        fun register(
            email: String,
            password: String,
        ) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                when (val result = authRepository.register(email, password)) {
                    is Resource.Success -> {
                        if (authRepository.isLoggedIn) {
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    isRegistered = true,
                                    requiresEmailConfirmation = false,
                                )
                            result.data?.let { user ->
                                fcmTokenManager.registerToken(user.id)
                            }
                        } else {
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    requiresEmailConfirmation = true,
                                )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                error = result.message,
                            )
                    }

                    is Resource.Loading -> {}
                }
            }
        }

        fun resetPassword(email: String) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                when (val result = authRepository.resetPassword(email)) {
                    is Resource.Success -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                isPasswordResetSent = true,
                            )
                    }

                    is Resource.Error -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                error = result.message,
                            )
                    }

                    is Resource.Loading -> {}
                }
            }
        }

        fun logout() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                // Push token must be cleared while the session is still valid: the
                // profiles update is RLS-gated to the signed-in user.
                val userId = authRepository.currentUserId
                if (!userId.isNullOrBlank()) {
                    fcmTokenManager.clearToken(userId)
                }
                when (val result = authRepository.logout()) {
                    is Resource.Success -> {
                        // Repository already wiped DataStore + secure storage; start
                        // from a clean state so the shell never sees stale auth flags.
                        _uiState.value =
                            AuthUiState(
                                isSessionResolved = true,
                                isLoggedIn = false,
                                logoutCompleted = true,
                            )
                    }

                    is Resource.Error -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                error = result.message,
                            )
                    }

                    is Resource.Loading -> {}
                }
            }
        }

        fun consumeLogoutEvent() {
            _uiState.value = _uiState.value.copy(logoutCompleted = false)
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(error = null)
        }

        fun dismissEmailConfirmation() {
            _uiState.value = _uiState.value.copy(requiresEmailConfirmation = false)
        }
    }
