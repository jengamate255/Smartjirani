package com.smartjirani.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.feature.civic.CivicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shell-scoped (activity-level) probe of the signed-in user's civic roles.
 * Drives the visibility of the Serikali tab and the Profile fallback entry;
 * actual authority is enforced server-side by RLS + RPC checks.
 */
@HiltViewModel
class CivicAccessViewModel
    @Inject
    constructor(
        private val civicRepository: CivicRepository,
    ) : ViewModel() {
        private val _isOfficial = MutableStateFlow(false)
        val isOfficial: StateFlow<Boolean> = _isOfficial.asStateFlow()

        private var loaded = false

        /** Called from the nav graph once the session is authenticated. */
        fun ensureLoaded() {
            if (loaded) return
            loaded = true
            viewModelScope.launch {
                when (val result = civicRepository.getMyRoles()) {
                    is Resource.Success -> _isOfficial.update { result.data.any { it.isOfficial } }
                    else -> _isOfficial.update { false }
                }
            }
        }

        /** Clear the cached verdict on logout so a different user re-probes. */
        fun reset() {
            loaded = false
            _isOfficial.update { false }
        }
    }
