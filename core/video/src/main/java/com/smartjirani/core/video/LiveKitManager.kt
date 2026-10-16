package com.smartjirani.core.video

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class LiveKitEvent {
    data class ParticipantConnected(
        val participant: RemoteParticipant,
    ) : LiveKitEvent()

    data class ParticipantDisconnected(
        val participant: RemoteParticipant,
    ) : LiveKitEvent()

    data class TrackSubscribed(
        val participant: RemoteParticipant,
        val track: VideoTrack,
    ) : LiveKitEvent()

    data class TrackUnsubscribed(
        val participant: RemoteParticipant,
        val track: VideoTrack,
    ) : LiveKitEvent()

    data class ActiveSpeakersChanged(
        val speakers: List<RemoteParticipant>,
    ) : LiveKitEvent()

    data object Disconnected : LiveKitEvent()

    data class Error(
        val error: Throwable,
    ) : LiveKitEvent()
}

@Singleton
class LiveKitManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private var room: Room? = null
        private var eventJob: Job? = null
        private val scope = CoroutineScope(Dispatchers.Main + Job())

        private val _events = MutableSharedFlow<LiveKitEvent>(extraBufferCapacity = 64)
        val events: SharedFlow<LiveKitEvent> = _events.asSharedFlow()

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
        val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

        private val _remoteVideoTracks = MutableStateFlow<Map<String, VideoTrack>>(emptyMap())
        val remoteVideoTracks: StateFlow<Map<String, VideoTrack>> = _remoteVideoTracks.asStateFlow()

        suspend fun connectToRoom(
            url: String,
            token: String,
            roomName: String,
        ) {
            disconnect()

            try {
                val newRoom = LiveKit.create(context)

                newRoom.connect(url, token)
                room = newRoom
                _isConnected.value = true

                eventJob =
                    scope.launch {
                        newRoom.events.collect { event ->
                            handleRoomEvent(event)
                        }
                    }
            } catch (e: Exception) {
                _events.emit(LiveKitEvent.Error(e))
                throw e
            }
        }

        private suspend fun handleRoomEvent(event: RoomEvent) {
            when (event) {
                is RoomEvent.ParticipantConnected -> {
                    _events.emit(LiveKitEvent.ParticipantConnected(event.participant))
                }

                is RoomEvent.ParticipantDisconnected -> {
                    val participantSid = event.participant.sid.toString()
                    _remoteVideoTracks.value = _remoteVideoTracks.value - participantSid
                    _events.emit(LiveKitEvent.ParticipantDisconnected(event.participant))
                }

                is RoomEvent.TrackSubscribed -> {
                    val track = event.track
                    val participantSid = event.participant.sid.toString()
                    if (track is VideoTrack) {
                        _remoteVideoTracks.value = _remoteVideoTracks.value + (participantSid to track)
                        _events.emit(LiveKitEvent.TrackSubscribed(event.participant, track))
                    }
                }

                is RoomEvent.TrackUnsubscribed -> {
                    val track = event.track
                    val participantSid = event.participant.sid.toString()
                    if (track is VideoTrack) {
                        _remoteVideoTracks.value = _remoteVideoTracks.value - participantSid
                        _events.emit(LiveKitEvent.TrackUnsubscribed(event.participant, track))
                    }
                }

                is RoomEvent.ActiveSpeakersChanged -> {
                    val remoteSpeakers = event.speakers.filterIsInstance<RemoteParticipant>()
                    _events.emit(LiveKitEvent.ActiveSpeakersChanged(remoteSpeakers))
                }

                is RoomEvent.Disconnected -> {
                    _isConnected.value = false
                    _localVideoTrack.value = null
                    _remoteVideoTracks.value = emptyMap()
                    _events.emit(LiveKitEvent.Disconnected)
                }

                is RoomEvent.FailedToConnect -> {
                    _events.emit(LiveKitEvent.Error(Exception("Failed to connect to room")))
                }

                else -> {}
            }
        }

        fun setMicrophoneEnabled(enabled: Boolean) {
            scope.launch {
                room?.localParticipant?.setMicrophoneEnabled(enabled)
            }
        }

        fun setCameraEnabled(enabled: Boolean) {
            scope.launch {
                val localParticipant = room?.localParticipant ?: return@launch
                localParticipant.setCameraEnabled(enabled)
                if (enabled) {
                    val videoTrack = localParticipant.getTrackPublication(io.livekit.android.room.track.Track.Source.CAMERA)?.track
                    _localVideoTrack.value = videoTrack as? VideoTrack
                } else {
                    _localVideoTrack.value = null
                }
            }
        }

        fun switchCamera() {
            // Camera switching not implemented in this version
        }

        suspend fun disconnect() {
            eventJob?.cancel()
            room?.disconnect()
            room = null
            _isConnected.value = false
            _localVideoTrack.value = null
            _remoteVideoTracks.value = emptyMap()
        }

        fun getRemoteParticipants(): List<RemoteParticipant> = room?.remoteParticipants?.values?.toList() ?: emptyList()
    }
