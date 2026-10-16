package com.smartjirani.feature.civic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.feature.civic.CivicReport
import com.smartjirani.feature.civic.CivicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CivicMapUiState(
    val isLoading: Boolean = true,
    val reports: List<CivicReport> = emptyList(),
    val error: String? = null,
    val locationLabel: String = "Mtaa",
) {
    val overdue: Int get() = reports.count { it.isOverdue }
    val escalated: Int get() = reports.count { it.status == "ESCALATED" }
}

@HiltViewModel
class CivicMapViewModel
    @Inject
    constructor(
        private val civicRepository: CivicRepository,
        userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CivicMapUiState())
        val uiState: StateFlow<CivicMapUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                userPreferencesRepository.userPreferences
                    .map { it.selectedNeighborhoodName }
                    .distinctUntilChanged()
                    .collect { name ->
                        if (!name.isNullOrBlank()) {
                            _uiState.update { it.copy(locationLabel = name) }
                        }
                    }
            }
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = it.reports.isEmpty(), error = null) }
                when (val result = civicRepository.getMapReports()) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isLoading = false, reports = result.data, error = null)
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun dismissError() = _uiState.update { it.copy(error = null) }
    }
