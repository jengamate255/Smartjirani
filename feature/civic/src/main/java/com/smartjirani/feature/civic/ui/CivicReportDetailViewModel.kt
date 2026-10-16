package com.smartjirani.feature.civic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.feature.civic.CivicAction
import com.smartjirani.feature.civic.CivicActions
import com.smartjirani.feature.civic.CivicAiAnalysisResult
import com.smartjirani.feature.civic.CivicAiProvider
import com.smartjirani.feature.civic.CivicAiRoutingSuggestion
import com.smartjirani.feature.civic.CivicReportDetail
import com.smartjirani.feature.civic.CivicRepository
import com.smartjirani.feature.civic.CivicRole
import com.smartjirani.feature.civic.CivicUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CivicDetailUiState(
    val isLoading: Boolean = true,
    val busy: Boolean = false,
    val detail: CivicReportDetail? = null,
    val roles: List<CivicRole> = emptyList(),
    val units: List<CivicUnit> = emptyList(),
    val error: String? = null,
    val routingSuggestion: CivicAiRoutingSuggestion? = null,
    val isLoadingRouting: Boolean = false,
    val analysisResult: CivicAiAnalysisResult? = null,
    val isAnalyzing: Boolean = false,
) {
    val actions: List<CivicAction>
        get() =
            detail?.let { d ->
                val official = CivicActions.actionsFor(d.report.status, roles)
                val reporter = if (d.isReporter) CivicActions.reporterActionsFor(d.report.status) else emptyList()
                (official + reporter).distinctBy { it.kind to it.toStatus }
            } ?: emptyList()
}

@HiltViewModel
class CivicReportDetailViewModel
    @Inject
    constructor(
        private val civicRepository: CivicRepository,
        private val civicAiProvider: CivicAiProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CivicDetailUiState())
        val uiState: StateFlow<CivicDetailUiState> = _uiState.asStateFlow()

        private var reportId: String = ""

        fun load(reportId: String) {
            this.reportId = reportId
            refresh()
        }

        fun refresh() {
            if (reportId.isBlank()) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = it.detail == null, error = null) }

                // Units power the assign dialog; roles gate the action buttons.
                val unitsResult = civicRepository.getQueue()
                _uiState.update {
                    it.copy(
                        roles = (unitsResult as? Resource.Success)?.data?.officialRoles ?: it.roles,
                        units = (unitsResult as? Resource.Success)?.data?.units ?: it.units,
                    )
                }

                when (val result = civicRepository.getReportDetail(reportId)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isLoading = false, detail = result.data, error = null)
                        }
                        // Auto-fetch routing suggestion for officials when report is in triaged/verified states
                        val isOfficial = _uiState.value.roles.any { it.isOfficial }
                        val status = result.data.report.status
                        if (isOfficial && status in listOf("AI_TRIAGED", "VERIFIED", "ESCALATED")) {
                            fetchRoutingSuggestion()
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

        fun fetchRoutingSuggestion() {
            if (reportId.isBlank()) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingRouting = true) }
                when (val result = civicAiProvider.routeReport(reportId)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isLoadingRouting = false, routingSuggestion = result.data, error = null)
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoadingRouting = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun performAction(
            action: CivicAction,
            reason: String?,
        ) {
            val toStatus = action.toStatus ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(busy = true) }
                when (val result = civicRepository.transitionReport(reportId, toStatus, reason)) {
                    is Resource.Success -> refresh()
                    is Resource.Error -> _uiState.update { it.copy(busy = false, error = result.message) }
                    is Resource.Loading -> Unit
                }
            }
        }

        fun assign(
            unitId: String,
            note: String?,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(busy = true) }
                when (val result = civicRepository.assignReport(reportId, unitId, null, note)) {
                    is Resource.Success -> refresh()
                    is Resource.Error -> _uiState.update { it.copy(busy = false, error = result.message) }
                    is Resource.Loading -> Unit
                }
            }
        }

        fun markDuplicate(reference: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(busy = true, error = null) }
                when (val resolved = civicRepository.findReportByRef(reference)) {
                    is Resource.Success -> {
                        val canonical = resolved.data
                        if (canonical == null) {
                            _uiState.update {
                                it.copy(busy = false, error = "Ripoti haijapatikana: $reference")
                            }
                            return@launch
                        }
                        when (val result = civicRepository.markDuplicate(reportId, canonical.id)) {
                            is Resource.Success -> {
                                refresh()
                            }

                            is Resource.Error -> {
                                _uiState.update {
                                    it.copy(busy = false, error = result.message)
                                }
                            }

                            is Resource.Loading -> {
                                Unit
                            }
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(busy = false, error = resolved.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun dismissError() = _uiState.update { it.copy(error = null) }

        fun clearRoutingSuggestion() = _uiState.update { it.copy(routingSuggestion = null) }

        fun analyzeReport() {
            if (reportId.isBlank()) return
            viewModelScope.launch {
                _uiState.update { it.copy(isAnalyzing = true, error = null) }
                when (val result = civicAiProvider.analyzeReport(reportId)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isAnalyzing = false, analysisResult = result.data, error = null)
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isAnalyzing = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun clearAnalysis() = _uiState.update { it.copy(analysisResult = null) }
    }
