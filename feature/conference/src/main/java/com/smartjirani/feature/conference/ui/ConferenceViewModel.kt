package com.smartjirani.feature.conference.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.video.ConferenceState
import com.smartjirani.core.video.LiveKitManager
import com.smartjirani.core.video.VideoConferenceService
import dagger.hilt.android.lifecycle.HiltViewModel
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConferenceUiState(
    val conferenceState: ConferenceState = ConferenceState(),
    val localVideoTrack: VideoTrack? = null,
    val remoteVideoTracks: Map<String, VideoTrack> = emptyMap(),
    val error: String? = null,
    val createdConferenceId: String? = null,
)

@HiltViewModel
class ConferenceViewModel
    @Inject
    constructor(
        private val conferenceService: VideoConferenceService,
        private val liveKitManager: LiveKitManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ConferenceUiState())
        val uiState: StateFlow<ConferenceUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                conferenceService.conferenceState.collect { state ->
                    _uiState.value = _uiState.value.copy(conferenceState = state)
                }
            }
            viewModelScope.launch {
                liveKitManager.localVideoTrack.collect { track ->
                    _uiState.value = _uiState.value.copy(localVideoTrack = track)
                }
            }
            viewModelScope.launch {
                liveKitManager.remoteVideoTracks.collect { tracks ->
                    _uiState.value = _uiState.value.copy(remoteVideoTracks = tracks)
                }
            }
        }

        fun createConference(
            title: String,
            neighborhoodId: String,
        ) {
            viewModelScope.launch {
                conferenceService
                    .createConference(title, neighborhoodId)
                    .onSuccess { conferenceId ->
                        _uiState.value = _uiState.value.copy(createdConferenceId = conferenceId)
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
            }
        }

        fun joinConference(conferenceId: String) {
            viewModelScope.launch { conferenceService.joinConference(conferenceId) }
        }

        fun leaveConference(conferenceId: String) {
            viewModelScope.launch { conferenceService.leaveConference(conferenceId) }
        }

        fun toggleMute() {
            if (_uiState.value.conferenceState.isMuted) {
                conferenceService.unmuteMicrophone()
            } else {
                conferenceService.muteMicrophone()
            }
        }

        fun toggleCamera() {
            if (_uiState.value.conferenceState.isCameraEnabled) {
                conferenceService.disableCamera()
            } else {
                conferenceService.enableCamera()
            }
        }

        fun switchCamera() {
            conferenceService.switchCamera()
        }

        fun toggleScreenShare() {
            conferenceService.toggleScreenShare()
        }

        fun endConference() {
            conferenceService.endConference()
        }

        fun dismissError() {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }
