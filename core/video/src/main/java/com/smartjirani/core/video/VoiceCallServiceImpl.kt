package com.smartjirani.core.video

import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CallRequestInsertDto(
    val caller_id: String,
    val callee_id: String,
    val call_type: String,
    val room_name: String,
    val status: String = "ringing",
)

@Singleton
class VoiceCallServiceImpl
    @Inject
    constructor(
        private val supabaseClientProvider: SupabaseClientProvider,
        private val liveKitManager: LiveKitManager,
    ) : VoiceCallService {
        private val scope = CoroutineScope(Dispatchers.Main + Job())
        private var signalingJob: Job? = null
        private var currentCallId: String? = null

        private val _callState = MutableStateFlow(CallState())
        override val callState: StateFlow<CallState> = _callState.asStateFlow()

        private fun currentUserId(): String =
            supabaseClientProvider.auth
                .currentUserOrNull()
                ?.id
                ?.toString() ?: ""

        override suspend fun startCall(
            calleeId: String,
            callType: CallType,
        ): Result<String> =
            try {
                val userId = currentUserId()
                val roomName = "call_${System.currentTimeMillis()}"

                _callState.value =
                    CallState(
                        callId = roomName,
                        callType = callType,
                        status = CallStatus.RINGING,
                        remoteParticipantName = calleeId,
                    )

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
                liveKitManager.setMicrophoneEnabled(true)
                if (callType == CallType.ONE_TO_ONE) {
                    liveKitManager.setCameraEnabled(true)
                }

                val callTypeStr =
                    when (callType) {
                        CallType.ONE_TO_ONE -> if (_callState.value.callType == CallType.ONE_TO_ONE) "voice" else "video"
                        CallType.GROUP -> "conference"
                    }
                val typeStr =
                    if (callType == CallType.GROUP) {
                        "conference"
                    } else if (liveKitManager.localVideoTrack.value != null) {
                        "video"
                    } else {
                        "voice"
                    }

                supabaseClientProvider.postgrest.from("call_requests").insert(
                    CallRequestInsertDto(
                        caller_id = userId,
                        callee_id = calleeId,
                        call_type = typeStr,
                        room_name = roomName,
                    ),
                )

                currentCallId = roomName
                _callState.value = _callState.value.copy(status = CallStatus.ACTIVE)

                observeCallSignaling(roomName)
                observeLiveKitEvents()

                Result.success(roomName)
            } catch (e: Exception) {
                _callState.value = CallState(status = CallStatus.ENDED)
                Result.failure(e)
            }

        override suspend fun acceptCall(callId: String): Result<Unit> =
            try {
                val userId = currentUserId()

                val tokenData =
                    fetchLiveKitToken(
                        supabaseClientProvider = supabaseClientProvider,
                        roomName = callId,
                        participantName = userId,
                        participantId = userId,
                    )

                liveKitManager.connectToRoom(
                    url = tokenData.url,
                    token = tokenData.token,
                    roomName = callId,
                )
                liveKitManager.setMicrophoneEnabled(true)

                currentCallId = callId
                _callState.value =
                    _callState.value.copy(
                        callId = callId,
                        status = CallStatus.ACTIVE,
                    )

                supabaseClientProvider.postgrest
                    .from("call_requests")
                    .update({
                        set("status", "active")
                        set(
                            "accepted_at",
                            java.time.Instant
                                .now()
                                .toString(),
                        )
                    }) {
                        filter { eq("room_name", callId) }
                    }

                observeLiveKitEvents()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun rejectCall(callId: String): Result<Unit> =
            try {
                supabaseClientProvider.postgrest
                    .from("call_requests")
                    .update({ set("status", "declined") }) {
                        filter { eq("room_name", callId) }
                    }
                _callState.value = _callState.value.copy(status = CallStatus.DECLINED)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun endCall(callId: String): Result<Unit> =
            try {
                liveKitManager.disconnect()
                signalingJob?.cancel()

                supabaseClientProvider.postgrest
                    .from("call_requests")
                    .update({ set("status", "ended") }) {
                        filter { eq("room_name", callId) }
                    }

                _callState.value = CallState(status = CallStatus.ENDED)
                currentCallId = null
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override fun mute() {
            liveKitManager.setMicrophoneEnabled(false)
            _callState.value = _callState.value.copy(isMuted = true)
        }

        override fun unmute() {
            liveKitManager.setMicrophoneEnabled(true)
            _callState.value = _callState.value.copy(isMuted = false)
        }

        override fun speakerOn() {
            _callState.value = _callState.value.copy(isSpeakerOn = true)
        }

        override fun speakerOff() {
            _callState.value = _callState.value.copy(isSpeakerOn = false)
        }

        private fun observeLiveKitEvents() {
            scope.launch {
                liveKitManager.events.collect { event ->
                    when (event) {
                        is LiveKitEvent.Disconnected -> {
                            _callState.value = CallState(status = CallStatus.ENDED)
                            currentCallId = null
                        }

                        is LiveKitEvent.Error -> {
                            _callState.value = _callState.value.copy(status = CallStatus.ENDED)
                        }

                        else -> {}
                    }
                }
            }
        }

        private fun observeCallSignaling(roomName: String) {
            signalingJob?.cancel()
            signalingJob =
                scope.launch {
                    val channel = supabaseClientProvider.realtime.channel("calls:$roomName")
                    channel
                        .postgresChangeFlow<PostgresAction>(schema = "public") {
                            this.table = "call_requests"
                            filter("room_name", FilterOperator.EQ, roomName)
                        }.collect { action ->
                            val status =
                                when (action) {
                                    is PostgresAction.Update -> {
                                        (action.record["status"] as? JsonPrimitive)?.contentOrNull
                                    }

                                    else -> {
                                        null
                                    }
                                }
                            when (status) {
                                "active" -> {
                                    _callState.value = _callState.value.copy(status = CallStatus.ACTIVE)
                                }

                                "declined" -> {
                                    _callState.value = _callState.value.copy(status = CallStatus.DECLINED)
                                    liveKitManager.disconnect()
                                }

                                "ended" -> {
                                    _callState.value = CallState(status = CallStatus.ENDED)
                                    liveKitManager.disconnect()
                                }
                            }
                        }
                    channel.subscribe()
                }
        }
    }
