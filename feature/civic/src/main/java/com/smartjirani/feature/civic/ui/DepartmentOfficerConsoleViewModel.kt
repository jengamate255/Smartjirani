package com.smartjirani.feature.civic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.feature.civic.CivicAnalytics
import com.smartjirani.feature.civic.CivicReport
import com.smartjirani.feature.civic.CivicRepository
import com.smartjirani.feature.civic.official.CivicOfficialFeed
import com.smartjirani.feature.civic.official.CivicOfficialFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DepartmentOfficerConsoleUiState(
    val reports: List<CivicReport> = emptyList(),
    val analytics: CivicAnalytics = CivicAnalytics(),
    val filter: CivicOfficialFilter = CivicOfficialFilter.ALL,
    val selectedReportIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DepartmentOfficerConsoleViewModel
    @Inject
    constructor(
        private val civicRepository: CivicRepository,
        private val officialFeed: CivicOfficialFeed,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DepartmentOfficerConsoleUiState())
        val uiState: StateFlow<DepartmentOfficerConsoleUiState> = _uiState.asStateFlow()

        init {
            loadData()
        }

        private fun loadData() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                loadAnalytics()
                loadFeed()
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        private suspend fun loadAnalytics() {
            when (val result = civicRepository.getAnalytics()) {
                is Resource.Success -> _uiState.update { it.copy(analytics = result.data) }
                is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                else -> Unit
            }
        }

        private suspend fun loadFeed() {
            val filter = _uiState.value.filter
            val result = runCatching { officialFeed.list(filter) }
            result.onSuccess { rows ->
                val reports =
                    rows.map { row ->
                        val dueAt =
                            if (row.due_at_minutes > 0) {
                                java.time.Instant
                                    .now()
                                    .plusSeconds(row.due_at_minutes.toLong() * 60)
                                    .toString()
                            } else {
                                null
                            }
                        CivicReport(
                            id = row.report_id,
                            publicReference = row.ref,
                            title = row.title,
                            description = "",
                            status = row.status,
                            severity = row.severity,
                            priority = row.severity,
                            categoryName = row.category_sw,
                            dueAt = dueAt,
                            createdAt = row.created_at,
                            updatedAt = row.created_at,
                        )
                    }
                _uiState.update { it.copy(reports = reports) }
            }
        }

        fun setFilter(filter: CivicOfficialFilter) {
            _uiState.update { it.copy(filter = filter) }
            viewModelScope.launch { loadFeed() }
        }

        fun updateSelection(selectedIds: Set<String>) {
            _uiState.update { it.copy(selectedReportIds = selectedIds) }
        }

        fun batchAssign(reportIds: Set<String>) {
            viewModelScope.launch {
                reportIds.forEach { reportId ->
                    civicRepository.transitionReport(reportId, "ASSIGN", null)
                }
                _uiState.update { it.copy(selectedReportIds = emptySet()) }
                loadFeed()
                loadAnalytics()
            }
        }

        fun batchVerify(reportIds: Set<String>) {
            viewModelScope.launch {
                reportIds.forEach { reportId ->
                    civicRepository.transitionReport(reportId, "CONFIRM", null)
                }
                _uiState.update { it.copy(selectedReportIds = emptySet()) }
                loadFeed()
                loadAnalytics()
            }
        }
    }
