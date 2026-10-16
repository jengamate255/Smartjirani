package com.smartjirani.feature.messaging.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.realtime.RealtimeEvent
import com.smartjirani.core.realtime.RealtimeService
import com.smartjirani.feature.messaging.MessageRepository
import com.smartjirani.feature.messaging.domain.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val error: String? = null,
    val currentUserId: String? = null,
)

@HiltViewModel
class ConversationsViewModel
    @Inject
    constructor(
        private val messageRepository: MessageRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val realtimeService: RealtimeService,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ConversationsUiState())
        val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

        private var observeJob: Job? = null
        private var realtimeJob: Job? = null

        fun loadConversations() {
            observeJob?.cancel()
            observeJob =
                viewModelScope.launch {
                    val userId = userPreferencesRepository.userPreferences.first().userId
                    if (userId.isNullOrBlank()) {
                        _uiState.update { it.copy(isLoading = false, currentUserId = null) }
                        return@launch
                    }
                    _uiState.update { it.copy(isLoading = true, currentUserId = userId, error = null) }

                    messageRepository.observeConversations(userId).collect { conversations ->
                        _uiState.update { it.copy(isLoading = false, conversations = conversations) }
                    }
                }

            viewModelScope.launch {
                when (val result = messageRepository.syncConversations(userId = userPreferencesRepository.userPreferences.first().userId ?: return@launch)) {
                    is Resource.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }

                    else -> {}
                }
            }

            observeRealtimeEvents()
        }

        private fun observeRealtimeEvents() {
            realtimeJob?.cancel()
            realtimeJob =
                viewModelScope.launch {
                    realtimeService.events.collect { event ->
                        if (event is RealtimeEvent.NewMessage) {
                            val userId = _uiState.value.currentUserId ?: return@collect
                            when (val result = messageRepository.syncConversations(userId)) {
                                is Resource.Success -> {}
                                else -> {}
                            }
                        }
                    }
                }
        }

        override fun onCleared() {
            observeJob?.cancel()
            realtimeJob?.cancel()
            super.onCleared()
        }
    }
