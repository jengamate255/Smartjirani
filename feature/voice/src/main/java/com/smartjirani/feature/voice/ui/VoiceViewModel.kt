package com.smartjirani.feature.voice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.audio.AudioPlayer
import com.smartjirani.core.audio.AudioRecorder
import com.smartjirani.core.common.result.Resource
import com.smartjirani.feature.voice.VoiceMessage
import com.smartjirani.feature.voice.VoiceMessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceUiState(
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0L,
    val isPlaying: Boolean = false,
    val playingMessageId: String? = null,
    val messages: List<VoiceMessage> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class VoiceViewModel
    @Inject
    constructor(
        private val audioRecorder: AudioRecorder,
        private val audioPlayer: AudioPlayer,
        private val voiceMessageRepository: VoiceMessageRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(VoiceUiState())
        val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

        init {
            // Mirror the recorder's own clock so the UI timer stays in sync with the actual capture.
            viewModelScope.launch {
                audioRecorder.recordingDuration.collect { duration ->
                    if (_uiState.value.isRecording) {
                        _uiState.update { it.copy(recordingDuration = duration) }
                    }
                }
            }
            viewModelScope.launch {
                audioPlayer.isPlaying.collect { playing ->
                    if (!playing && _uiState.value.isPlaying) {
                        _uiState.update { it.copy(isPlaying = false, playingMessageId = null) }
                    }
                }
            }
        }

        /** Loads the voice-note thread between the signed-in user and [peerId]. */
        fun loadMessages(peerId: String) {
            if (peerId.isBlank()) return
            viewModelScope.launch {
                when (val result = voiceMessageRepository.getVoiceMessages(peerId)) {
                    is Resource.Success -> _uiState.update { it.copy(messages = result.data, error = null) }
                    is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                    is Resource.Loading -> Unit
                }
            }
        }

        fun startRecording() {
            viewModelScope.launch {
                audioRecorder.startRecording()
                _uiState.update { it.copy(isRecording = true, recordingDuration = 0L, error = null) }
            }
        }

        fun stopRecordingAndSend(receiverId: String?) {
            viewModelScope.launch {
                val result = audioRecorder.stopRecording()
                _uiState.update { it.copy(isRecording = false, recordingDuration = 0L) }
                if (result == null) return@launch
                val message =
                    VoiceMessage(
                        id = "",
                        senderId = "",
                        receiverId = receiverId,
                        storagePath = result.filePath,
                        durationMs = result.durationMs,
                    )
                when (val sendResult = voiceMessageRepository.sendVoiceMessage(message)) {
                    is Resource.Success -> {
                        _uiState.update {
                            if (it.messages.any { existing -> existing.id == sendResult.data.id }) {
                                it
                            } else {
                                it.copy(messages = it.messages + sendResult.data, error = null)
                            }
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(error = sendResult.message) }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun cancelRecording() {
            viewModelScope.launch {
                audioRecorder.cancelRecording()
                _uiState.update { it.copy(isRecording = false, recordingDuration = 0L) }
            }
        }

        fun playMessage(
            filePath: String,
            messageId: String,
        ) {
            // Tapping the message that is already playing acts as a stop control.
            if (_uiState.value.playingMessageId == messageId) {
                stopPlayback()
                return
            }
            audioPlayer.play(filePath)
            _uiState.update { it.copy(isPlaying = true, playingMessageId = messageId) }
        }

        fun stopPlayback() {
            audioPlayer.stop()
            _uiState.update { it.copy(isPlaying = false, playingMessageId = null) }
        }

        fun deleteMessage(messageId: String) {
            viewModelScope.launch {
                when (val result = voiceMessageRepository.deleteVoiceMessage(messageId)) {
                    is Resource.Success -> {
                        _uiState.update { state ->
                            state.copy(messages = state.messages.filterNot { it.id == messageId })
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun dismissError() {
            _uiState.update { it.copy(error = null) }
        }
    }
