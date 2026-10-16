package com.smartjirani.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.security.BiometricAuthUseCase
import com.smartjirani.feature.profile.ProfileExtras
import com.smartjirani.feature.profile.ProfileExtrasRepository
import com.smartjirani.feature.profile.ProfileRepository
import com.smartjirani.feature.profile.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isExtrasLoading: Boolean = false,
    val profile: UserProfile? = null,
    val extras: ProfileExtras = ProfileExtras(),
    /** Label for the shell's location chip. */
    val locationLabel: String = "Mtaa",
    val toast: String? = null,
    val error: String? = null,
    val isSaved: Boolean = false,
    /** Opt-in biometric app lock; toggle hidden when the device can't do it. */
    val appLockEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
)

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
        private val profileExtrasRepository: ProfileExtrasRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val biometricAuthUseCase: BiometricAuthUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProfileUiState())
        val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

        init {
            observeLocation()
            loadProfile()
            observeAppLock()
        }

        private fun observeAppLock() {
            _uiState.update { it.copy(isBiometricAvailable = biometricAuthUseCase.isBiometricAvailable()) }
            viewModelScope.launch {
                userPreferencesRepository.userPreferences
                    .map { it.appLockEnabled }
                    .distinctUntilChanged()
                    .collect { enabled -> _uiState.update { it.copy(appLockEnabled = enabled) } }
            }
        }

        fun onAppLockToggle(enabled: Boolean) {
            _uiState.update { it.copy(appLockEnabled = enabled) }
            viewModelScope.launch {
                userPreferencesRepository.setAppLockEnabled(enabled)
                _uiState.update {
                    it.copy(toast = if (enabled) "Linda kwa vidole imewashwa" else "Linda kwa vidole imezimwa")
                }
            }
        }

        private fun observeLocation() {
            viewModelScope.launch {
                userPreferencesRepository.userPreferences
                    .map { it.selectedNeighborhoodName ?: "Mtaa" }
                    .distinctUntilChanged()
                    .collect { label -> _uiState.update { it.copy(locationLabel = label) } }
            }
        }

        fun loadProfile() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                when (val result = profileRepository.getCurrentProfile()) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false, profile = result.data, isSaved = false) }
                        loadExtras(result.data.id)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }

                    is Resource.Loading -> {}
                }
            }
        }

        private fun loadExtras(userId: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isExtrasLoading = true) }
                when (val result = profileExtrasRepository.getExtras(userId)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isExtrasLoading = false, extras = result.data)
                        }
                    }

                    // The profile itself still renders without karma/locker data.
                    is Resource.Error -> {
                        _uiState.update { it.copy(isExtrasLoading = false) }
                    }

                    is Resource.Loading -> {}
                }
            }
        }

        fun updateProfile(
            displayName: String,
            username: String,
            bio: String?,
        ) {
            viewModelScope.launch {
                val current = _uiState.value.profile ?: return@launch
                _uiState.update { it.copy(isLoading = true) }
                val updated =
                    current.copy(
                        displayName = displayName,
                        username = username,
                        bio = bio,
                        updatedAt = System.currentTimeMillis(),
                    )
                when (val result = profileRepository.updateProfile(updated)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isLoading = false, profile = result.data, isSaved = true, toast = "Wasifu umehifadhiwa")
                        }
                        loadExtras(result.data.id)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }

                    is Resource.Loading -> {}
                }
            }
        }

        /** Optimistic SOS flip: the switch moves now, rollback if the write fails. */
        fun onSosToggle(enabled: Boolean) {
            val circle = _uiState.value.extras.securityCircle ?: return
            _uiState.update {
                it.copy(extras = it.extras.copy(securityCircle = circle.copy(sosEnabled = enabled)))
            }
            viewModelScope.launch {
                when (val result = profileExtrasRepository.setSosEnabled(enabled)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(toast = if (enabled) "SOS imewashwa" else "SOS imezimwa")
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { state ->
                            state.copy(
                                extras = state.extras.copy(securityCircle = circle),
                                error = result.message,
                            )
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun onHouseNumberToggle(visible: Boolean) {
            val privacy = _uiState.value.extras.privacy ?: return
            _uiState.update {
                it.copy(extras = it.extras.copy(privacy = privacy.copy(showHouseNumber = visible)))
            }
            viewModelScope.launch {
                when (val result = profileExtrasRepository.setHouseNumberVisible(visible)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(toast = if (visible) "Namba ya nyumba inaonekana" else "Namba ya nyumba imefichwa")
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { state ->
                            state.copy(
                                extras = state.extras.copy(privacy = privacy),
                                error = result.message,
                            )
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun refresh() {
            loadProfile()
        }

        fun dismissToast() {
            _uiState.update { it.copy(toast = null) }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        fun clearSaved() {
            _uiState.update { it.copy(isSaved = false) }
        }
    }
