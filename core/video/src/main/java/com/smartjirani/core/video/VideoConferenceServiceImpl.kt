package com.smartjirani.core.video

import com.smartjirani.core.network.SupabaseClientProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoConferenceServiceImpl
    @Inject
    constructor(
        private val supabaseClientProvider: SupabaseClientProvider,
        private val liveKitManager: LiveKitManager,
    ) : VideoConferenceService {
        private val scope = CoroutineScope(Dispatchers.Main + Job())

        private val _conferenceState = MutableStateFlow(ConferenceState())
        override val conferenceState: StateFlow<ConferenceState> = _conferenceState.asStateFlow()

        private fun currentUserId(): String =
            supabaseClientProvider.auth
                .currentUserOrNull()
                ?.id
                ?.toString() ?: ""

        override suspend fun createConference(
            title: String,
            neighborhoodId: String,
        ): Result<String> =
            try {
                val userId = currentUserId()
                val roomName = "conf_${System.currentTimeMillis()}"

                val tokenData =
                    fetchLiveKitToken(
                        supabaseClientProvider = supabaseClientProvider,
                        roomName = roomName,
                        participantName = userId,
                        participantId = userId,
                    )

                liveKitManager.connectToRoom(
                    url = tokenData.url,
                    token = tokenData.token,
                    roomName = roomName,
                )

                _conferenceState.value =
                    ConferenceState(
                        conferenceId = roomName,
                        title = title,
                        status = ConferenceStatus.ACTIVE,
                        isMuted = false,
                        isCameraEnabled = false,
                        participantCount = 1,
                    )

                observeParticipants()

                Result.success(roomName)
            } catch (e: Exception) {
                _conferenceState.value = ConferenceState(status = ConferenceStatus.ENDED)
                Result.failure(e)
            }

        override suspend fun joinConference(conferenceId: String): Result<Unit> =
            try {
                val userId = currentUserId()

                val tokenData =
                    fetchLiveKitToken(
                        supabaseClientProvider = supabaseClientProvider,
                        roomName = conferenceId,
                        participantName = userId,
                        participantId = userId,
                    )

                liveKitManager.connectToRoom(
                    url = tokenData.url,
                    token = tokenData.token,
                    roomName = conferenceId,
                )

                _conferenceState.value =
                    ConferenceState(
                        conferenceId = conferenceId,
                        status = ConferenceStatus.ACTIVE,
                        isMuted = false,
                        isCameraEnabled = false,
                        participantCount = liveKitManager.getRemoteParticipants().size + 1,
                    )

                observeParticipants()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun leaveConference(conferenceId: String): Result<Unit> =
            try {
                liveKitManager.disconnect()
                _conferenceState.value = ConferenceState(status = ConferenceStatus.ENDED)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override fun muteMicrophone() {
            liveKitManager.setMicrophoneEnabled(false)
            _conferenceState.value = _conferenceState.value.copy(isMuted = true)
        }

        override fun unmuteMicrophone() {
            liveKitManager.setMicrophoneEnabled(true)
            _conferenceState.value = _conferenceState.value.copy(isMuted = false)
        }

        override fun enableCamera() {
            liveKitManager.setCameraEnabled(true)
            _conferenceState.value = _conferenceState.value.copy(isCameraEnabled = true)
        }

        override fun disableCamera() {
            liveKitManager.setCameraEnabled(false)
            _conferenceState.value = _conferenceState.value.copy(isCameraEnabled = false)
        }

        override fun switchCamera() {
            liveKitManager.switchCamera()
        }

        override fun toggleScreenShare() {
            _conferenceState.value =
                _conferenceState.value.copy(
                    isScreenSharing = !_conferenceState.value.isScreenSharing,
                )
        }

        override fun endConference() {
            scope.launch {
                liveKitManager.disconnect()
                _conferenceState.value = ConferenceState(status = ConferenceStatus.ENDED)
            }
        }

        private fun observeParticipants() {
            scope.launch {
                liveKitManager.events.collect { event ->
                    when (event) {
                        is LiveKitEvent.ParticipantConnected -> {
                            _conferenceState.value =
                                _conferenceState.value.copy(
                                    participantCount = liveKitManager.getRemoteParticipants().size + 1,
                                    status = ConferenceStatus.ACTIVE,
                                )
                        }

                        is LiveKitEvent.ParticipantDisconnected -> {
                            _conferenceState.value =
                                _conferenceState.value.copy(
                                    participantCount = liveKitManager.getRemoteParticipants().size + 1,
                                )
                        }

                        is LiveKitEvent.Disconnected -> {
                            _conferenceState.value = ConferenceState(status = ConferenceStatus.ENDED)
                        }

                        else -> {}
                    }
                }
            }
        }
    }
