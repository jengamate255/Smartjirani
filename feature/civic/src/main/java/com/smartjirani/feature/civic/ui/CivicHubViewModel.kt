package com.smartjirani.feature.civic.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.feature.civic.CivicAnalytics
import com.smartjirani.feature.civic.CivicAnnouncement
import com.smartjirani.feature.civic.CivicQueueData
import com.smartjirani.feature.civic.CivicRepository
import com.smartjirani.feature.civic.R
import com.smartjirani.feature.civic.official.CivicOfficialFeed
import com.smartjirani.feature.civic.official.CivicOfficialFeedRow
import com.smartjirani.feature.civic.official.CivicOfficialFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CivicHubUiState(
    val isLoading: Boolean = true,
    val data: CivicQueueData = CivicQueueData(emptyList(), emptyList(), emptyList(), emptyList()),
    val statusFilter: String? = null,
    val officialFilter: CivicOfficialFilter = CivicOfficialFilter.ALL,
    val officialFeed: List<com.smartjirani.feature.civic.CivicReport> = emptyList(),
    val announcements: List<CivicAnnouncement> = emptyList(),
    /** Officials only; null while loading or when the analytics read fails (fail soft). */
    val analytics: CivicAnalytics? = null,
    val isPublishing: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val locationLabel: String = "Mtaa",
) {
    val visibleReports: List<com.smartjirani.feature.civic.CivicReport>
        get() =
            if (officialFeed.isNotEmpty()) {
                officialFeed
            } else if (statusFilter == null) {
                data.reports
            } else {
                data.reports.filter { it.status == statusFilter }
            }

    val total: Int get() = data.reports.size
    val overdue: Int get() = data.reports.count { it.isOverdue }
    val active: Int get() =
        data.reports.count {
            it.status !in com.smartjirani.feature.civic.CivicReport.TERMINAL_STATUSES
        }
    val duplicatesFolded: Int get() = data.reports.sumOf { (it.reportCount - 1).coerceAtLeast(0) }

    /** COUNCIL_ADMIN / SYSTEM_ADMIN only — shows the government config entry point. */
    val isAdmin: Boolean get() =
        data.roles.any {
            it.role == com.smartjirani.feature.civic.CivicRole.ROLE_COUNCIL_ADMIN ||
                it.role == com.smartjirani.feature.civic.CivicRole.ROLE_SYSTEM_ADMIN
        }

    /** DEPARTMENT_OFFICER — shows the department console entry point. */
    val isDepartmentOfficer: Boolean get() =
        data.roles.any {
            it.role == com.smartjirani.feature.civic.CivicRole.ROLE_DEPARTMENT_OFFICER
        }
}

@HiltViewModel
class CivicHubViewModel
    @Inject
    constructor(
        private val civicRepository: CivicRepository,
        private val officialFeed: CivicOfficialFeed,
        userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CivicHubUiState())
        val uiState: StateFlow<CivicHubUiState> = _uiState.asStateFlow()

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
            refresh()
        }

        fun refresh() {
            viewModelScope.launch { loadQueue() }
            viewModelScope.launch { loadAnnouncements() }
            viewModelScope.launch { loadAnalytics() }
            viewModelScope.launch { loadOfficialFeed() }
        }

        private suspend fun loadQueue() {
            _uiState.update { it.copy(isLoading = it.data.reports.isEmpty(), error = null) }
            when (val result = civicRepository.getQueue()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, data = result.data, error = null)
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

        private suspend fun loadAnnouncements() {
            // Announcements are the resident-facing half of this screen; a read failure must
            // not mask the queue, so they fail soft to an empty list.
            when (val result = civicRepository.getAnnouncements()) {
                is Resource.Success -> _uiState.update { it.copy(announcements = result.data) }
                else -> Unit
            }
        }

        private suspend fun loadAnalytics() {
            // Charts are additive to the queue; a failed read keeps the last data on screen.
            when (val result = civicRepository.getAnalytics()) {
                is Resource.Success -> _uiState.update { it.copy(analytics = result.data) }
                else -> Unit
            }
        }

        private suspend fun loadOfficialFeed() {
            val filter = _uiState.value.officialFilter
            val result = runCatching { officialFeed.list(filter) }
            result.onSuccess { rows ->
                val reports = rows.map { it.toCivicReport() }
                _uiState.update { it.copy(officialFeed = reports) }
            }
        }

        fun setOfficialFilter(filter: CivicOfficialFilter) {
            _uiState.update { it.copy(officialFilter = filter) }
            viewModelScope.launch { loadOfficialFeed() }
        }

        fun publishAnnouncement(
            title: String,
            body: String,
            category: String,
            pinned: Boolean,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isPublishing = true, error = null) }
                when (
                    val result =
                        civicRepository.publishAnnouncement(
                            title = title.trim(),
                            body = body.trim(),
                            category = category,
                            pinned = pinned,
                        )
                ) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isPublishing = false, toast = "Tangazo limetangazwa kwa wakazi")
                        }
                        loadAnnouncements()
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isPublishing = false, error = result.message)
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

        fun dismissToast() = _uiState.update { it.copy(toast = null) }
    }

@StringRes
fun statusLabelRes(status: String): Int =
    when (status) {
        "NEW" -> R.string.civic_status_new
        "AI_TRIAGED" -> R.string.civic_status_ai_triaged
        "AWAITING_VERIFICATION" -> R.string.civic_status_awaiting_verification
        "VERIFIED" -> R.string.civic_status_verified
        "ASSIGNED" -> R.string.civic_status_assigned
        "IN_PROGRESS" -> R.string.civic_status_in_progress
        "WAITING_EXTERNAL_ACTION" -> R.string.civic_status_waiting_external
        "AWAITING_RESIDENT_CONFIRMATION" -> R.string.civic_status_awaiting_resident
        "RESOLVED" -> R.string.civic_status_resolved
        "CLOSED" -> R.string.civic_status_closed
        "REJECTED" -> R.string.civic_status_rejected
        "ESCALATED" -> R.string.civic_status_escalated
        else -> R.string.civic_status_new
    }

private fun CivicOfficialFeedRow.toCivicReport(): com.smartjirani.feature.civic.CivicReport {
    val dueAt =
        if (due_at_minutes > 0) {
            java.time.Instant
                .now()
                .plusSeconds(due_at_minutes.toLong() * 60)
                .toString()
        } else {
            null
        }

    return com.smartjirani.feature.civic.CivicReport(
        id = report_id,
        publicReference = ref,
        title = title,
        description = "",
        status = status,
        severity = severity,
        priority = severity,
        categoryName = category_sw,
        dueAt = dueAt,
        createdAt = created_at,
        updatedAt = created_at,
    )
}
