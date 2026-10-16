package com.smartjirani.feature.civic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.feature.civic.CivicReport
import com.smartjirani.feature.civic.CivicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CivicMyReportsUiState(
    val isLoading: Boolean = true,
    val reports: List<CivicReport> = emptyList(),
    val statusFilter: String? = null,
    val error: String? = null,
) {
    val visibleReports: List<CivicReport>
        get() = if (statusFilter == null) reports else reports.filter { it.status == statusFilter }

    val availableStatuses: List<String>
        get() = reports.map { it.status }.distinct()
}

@HiltViewModel
class CivicMyReportsViewModel
    @Inject
    constructor(
        private val civicRepository: CivicRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CivicMyReportsUiState())
        val uiState: StateFlow<CivicMyReportsUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = it.reports.isEmpty(), error = null) }
                when (val res = civicRepository.getMyReports()) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isLoading = false, reports = res.data, error = null)
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = res.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun setStatusFilter(status: String?) {
            _uiState.update { it.copy(statusFilter = status) }
        }

        fun dismissError() = _uiState.update { it.copy(error = null) }
    }
