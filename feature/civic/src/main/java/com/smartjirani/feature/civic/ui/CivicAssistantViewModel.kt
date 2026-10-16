package com.smartjirani.feature.civic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.feature.civic.CivicAiProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CivicAssistantMessage(
    val text: String,
    val isUser: Boolean,
    val isMock: Boolean = false,
)

data class CivicAssistantUiState(
    val messages: List<CivicAssistantMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CivicAssistantViewModel
    @Inject
    constructor(
        private val civicAiProvider: CivicAiProvider,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                CivicAssistantUiState(
                    messages =
                        listOf(
                            CivicAssistantMessage(text = GREETING, isUser = false),
                        ),
                ),
            )
        val uiState: StateFlow<CivicAssistantUiState> = _uiState.asStateFlow()

        fun onInputChange(value: String) = _uiState.update { it.copy(input = value) }

        fun send() {
            val state = _uiState.value
            val message = state.input.trim()
            if (message.isEmpty() || state.isSending) return

            _uiState.update {
                it.copy(
                    input = "",
                    isSending = true,
                    error = null,
                    messages = it.messages + CivicAssistantMessage(text = message, isUser = true),
                )
            }
            viewModelScope.launch {
                when (val res = civicAiProvider.sendMessage(message)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                error = null,
                                messages =
                                    it.messages +
                                        CivicAssistantMessage(
                                            text = res.data.reply,
                                            isUser = false,
                                            isMock = res.data.mock,
                                        ),
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isSending = false, error = res.message) }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun dismissError() = _uiState.update { it.copy(error = null) }

        private companion object {
            const val GREETING =
                "Karibu! Mimi ni Msaidizi wa AI wa SmartJirani. Naweza kukusaidia: " +
                    "jinsi ya kuripoti tatizo, hali ya ripoti zako, au maswali kuhusu huduma za serikali."
        }
    }
