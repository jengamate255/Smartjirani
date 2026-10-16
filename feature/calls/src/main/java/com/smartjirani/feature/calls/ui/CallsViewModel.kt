package com.smartjirani.feature.calls.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.video.CallState
import com.smartjirani.core.video.CallType
import com.smartjirani.core.video.VoiceCallService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallsUiState(
    val callState: CallState = CallState(),
    val startedCallId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class CallsViewModel
    @Inject
    constructor(
        private val voiceCallService: VoiceCallService,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CallsUiState())
        val uiState: StateFlow<CallsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                voiceCallService.callState.collect { state ->
                    _uiState.value = _uiState.value.copy(callState = state)
                }
            }
        }

        fun startCall(
            calleeId: String,
            callType: CallType = CallType.ONE_TO_ONE,
        ) {
            viewModelScope.launch {
                voiceCallService
                    .startCall(calleeId, callType)
                    .onSuccess { callId ->
                        _uiState.value = _uiState.value.copy(startedCallId = callId)
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
            }
        }

        fun acceptCall(callId: String) {
            viewModelScope.launch { voiceCallService.acceptCall(callId) }
        }

        fun rejectCall(callId: String) {
            viewModelScope.launch { voiceCallService.rejectCall(callId) }
        }

        fun endCall(callId: String) {
            viewModelScope.launch { voiceCallService.endCall(callId) }
        }

        fun toggleMute() {
            if (_uiState.value.callState.isMuted) {
                voiceCallService.unmute()
            } else {
                voiceCallService.mute()
            }
        }

        fun toggleSpeaker() {
            if (_uiState.value.callState.isSpeakerOn) {
                voiceCallService.speakerOff()
            } else {
                voiceCallService.speakerOn()
            }
        }

        fun dismissError() {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }
