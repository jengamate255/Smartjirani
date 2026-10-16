package com.smartjirani.core.video

import kotlinx.coroutines.flow.StateFlow

enum class CallStatus {
    IDLE,
    RINGING,
    ACTIVE,
    ENDED,
    MISSED,
    DECLINED,
}

enum class CallType {
    ONE_TO_ONE,
    GROUP,
}

data class CallState(
    val callId: String = "",
    val callType: CallType = CallType.ONE_TO_ONE,
    val status: CallStatus = CallStatus.IDLE,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val remoteParticipantName: String = "",
    val remoteParticipantAvatar: String? = null,
    val duration: Long = 0L,
)

interface VoiceCallService {
    val callState: StateFlow<CallState>

    suspend fun startCall(
        calleeId: String,
        callType: CallType,
    ): Result<String>

    suspend fun acceptCall(callId: String): Result<Unit>

    suspend fun rejectCall(callId: String): Result<Unit>

    suspend fun endCall(callId: String): Result<Unit>

    fun mute()

    fun unmute()

    fun speakerOn()

    fun speakerOff()
}

data class ConferenceState(
    val conferenceId: String = "",
    val title: String = "",
    val status: ConferenceStatus = ConferenceStatus.IDLE,
    val isMuted: Boolean = false,
    val isCameraEnabled: Boolean = true,
    val isScreenSharing: Boolean = false,
    val participantCount: Int = 0,
)

enum class ConferenceStatus {
    IDLE,
    WAITING,
    ACTIVE,
    ENDED,
}

interface VideoConferenceService {
    val conferenceState: StateFlow<ConferenceState>

    suspend fun createConference(
        title: String,
        neighborhoodId: String,
    ): Result<String>

    suspend fun joinConference(conferenceId: String): Result<Unit>

    suspend fun leaveConference(conferenceId: String): Result<Unit>

    fun muteMicrophone()

    fun unmuteMicrophone()

    fun enableCamera()

    fun disableCamera()

    fun switchCamera()

    fun toggleScreenShare()

    fun endConference()
}
