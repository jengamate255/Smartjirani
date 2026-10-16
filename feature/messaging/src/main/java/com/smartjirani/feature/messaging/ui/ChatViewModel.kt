package com.smartjirani.feature.messaging.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.realtime.RealtimeService
import com.smartjirani.feature.messaging.MessageRepository
import com.smartjirani.feature.messaging.domain.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isLoading: Boolean = false,
    val messages: List<Message> = emptyList(),
    val conversationTitle: String = "",
    val currentUserId: String? = null,
    val error: String? = null,
    val isSending: Boolean = false,
)

@HiltViewModel
class ChatViewModel
    @Inject
    constructor(
        private val messageRepository: MessageRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val realtimeService: RealtimeService,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChatUiState())
        val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

        private var conversationId: String? = null
        private var observeJob: Job? = null
        private var realtimeJob: Job? = null

        fun loadChat(conversationId: String) {
            this.conversationId = conversationId
            observeJob?.cancel()
            observeJob =
                viewModelScope.launch {
                    val userId = userPreferencesRepository.userPreferences.first().userId
                    _uiState.update { it.copy(isLoading = true, currentUserId = userId, error = null) }

                    messageRepository.observeMessages(conversationId).collect { messages ->
                        _uiState.update { it.copy(isLoading = false, messages = messages) }
                    }
                }

            viewModelScope.launch {
                realtimeService.subscribeToConversation(conversationId)
                when (val result = messageRepository.syncMessages(conversationId)) {
                    is Resource.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }

                    else -> {}
                }
            }

            observeRealtimeEvents(conversationId)
        }

        private fun observeRealtimeEvents(conversationId: String) {
            realtimeJob?.cancel()
            realtimeJob =
                viewModelScope.launch {
                    realtimeService.events.collect { event ->
                        if (event is com.smartjirani.core.realtime.RealtimeEvent.NewMessage &&
                            event.conversationId == conversationId
                        ) {
                            messageRepository.syncMessages(conversationId)
                        }
                    }
                }
        }

        fun sendMessage(content: String) {
            val convId = conversationId ?: return
            val userId = _uiState.value.currentUserId ?: return
            if (content.isBlank()) return

            _uiState.update { it.copy(isSending = true) }
            viewModelScope.launch {
                when (val result = messageRepository.sendMessage(convId, userId, content.trim())) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isSending = false) }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isSending = false, error = result.message) }
                    }

                    is Resource.Loading -> {}
                }
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        override fun onCleared() {
            conversationId?.let {
                viewModelScope.launch { realtimeService.unsubscribeFromConversation(it) }
            }
            observeJob?.cancel()
            realtimeJob?.cancel()
            super.onCleared()
        }
    }
