package com.smartjirani.feature.notifications.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.database.dao.NotificationDao
import com.smartjirani.core.database.entity.NotificationEntity
import com.smartjirani.core.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationEntity> = emptyList(),
    val error: String? = null,
    val isSignedIn: Boolean = false,
) {
    val unreadCount: Int get() = notifications.count { !it.isRead }
}

@HiltViewModel
class NotificationsViewModel
    @Inject
    constructor(
        private val notificationDao: NotificationDao,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NotificationsUiState())
        val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

        private var observeJob: Job? = null

        /**
         * The signed-in user is resolved from preferences rather than passed in by the nav graph,
         * so the list cannot end up bound to a stale or empty id.
         */
        fun loadNotifications() {
            observeJob?.cancel()
            observeJob =
                viewModelScope.launch {
                    val userId = userPreferencesRepository.userPreferences.first().userId
                    if (userId.isNullOrBlank()) {
                        _uiState.update { it.copy(isLoading = false, isSignedIn = false, notifications = emptyList()) }
                        return@launch
                    }
                    _uiState.update { it.copy(isLoading = true, isSignedIn = true, error = null) }
                    notificationDao.getNotifications(userId).collect { notifications ->
                        _uiState.update { it.copy(isLoading = false, notifications = notifications) }
                    }
                }
        }

        fun markAsRead(notificationId: String) {
            viewModelScope.launch { notificationDao.markAsRead(notificationId) }
        }

        fun markAllAsRead() {
            viewModelScope.launch {
                val userId = userPreferencesRepository.userPreferences.first().userId ?: return@launch
                notificationDao.markAllAsRead(userId)
            }
        }

        fun dismiss(notificationId: String) {
            viewModelScope.launch { notificationDao.deleteNotification(notificationId) }
        }
    }
