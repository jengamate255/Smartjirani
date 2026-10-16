# SmartJirani Communications Architecture

## Overview

SmartJirani provides voice messaging, 1-to-1 voice/video calls, and multi-party conferencing. The communication layer uses provider-independent interfaces, allowing the underlying WebRTC/telephony implementation to be swapped without affecting feature modules.

## Architecture

### Provider-Independent Design

```
┌─────────────────────────────────────────────────────┐
│                 Feature Modules                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │  voice   │  │  calls   │  │   conference     │  │
│  └────┬─────┘  └────┬─────┘  └────────┬─────────┘  │
│       │              │                 │             │
│  ┌────▼──────────────▼─────────────────▼─────────┐  │
│  │         Core Communication Interfaces          │  │
│  │   VoiceCallService    VideoConferenceService   │  │
│  └────────────────┬──────────────────────────────┘  │
│                   │                                 │
│  ┌────────────────▼──────────────────────────────┐  │
│  │         Provider Implementations               │  │
│  │  ┌─────────────┐  ┌─────────────────────┐     │  │
│  │  │  WebRTC     │  │  Supabase Realtime  │     │  │
│  │  │  Provider   │  │  Signaling          │     │  │
│  │  └─────────────┘  └─────────────────────┘     │  │
│  └────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### Core Interfaces

#### VoiceCallService

```kotlin
interface VoiceCallService {
    // Call lifecycle
    suspend fun initiateCall(callId: String, recipientId: String, callType: CallType): CallSession
    suspend fun acceptCall(callId: String): CallSession
    suspend fun declineCall(callId: String)
    suspend fun endCall(callId: String)

    // In-call controls
    fun muteAudio(muted: Boolean)
    fun setSpeakerphone(enabled: Boolean)
    fun holdCall(held: Boolean)

    // State observation
    fun observeCallState(): Flow<CallState>
    fun observeCallEvents(): Flow<CallEvent>
}

sealed class CallState {
    object Idle : CallState()
    object Ringing : CallState()
    object Connecting : CallState()
    object Active : CallState()
    object Hold : CallState()
    object Disconnecting : CallState()
    data class Failed(val reason: CallFailureReason) : CallState()
}

sealed class CallEvent {
    data class ParticipantJoined(val participantId: String) : CallEvent()
    data class ParticipantLeft(val participantId: String) : CallEvent()
    data class AudioMuted(val participantId: String, val muted: Boolean) : CallEvent()
    data class NetworkQualityChanged(val quality: NetworkQuality) : CallEvent()
}
```

#### VideoConferenceService

```kotlin
interface VideoConferenceService {
    // Conference lifecycle
    suspend fun createConference(conferenceId: String, neighborhoodId: String): ConferenceSession
    suspend fun joinConference(conferenceId: String): ConferenceSession
    suspend fun leaveConference(conferenceId: String)
    suspend fun endConference(conferenceId: String)

    // Participant management
    suspend fun inviteParticipant(conferenceId: String, userId: String)
    suspend fun removeParticipant(conferenceId: String, userId: String)
    fun observeParticipants(): Flow<List<Participant>>

    // Media controls
    fun toggleVideo(enabled: Boolean)
    fun toggleAudio(muted: Boolean)
    fun shareScreen(enabled: Boolean)
    fun switchCamera()

    // State observation
    fun observeConferenceState(): Flow<ConferenceState>
    fun observeConferenceEvents(): Flow<ConferenceEvent>
}

sealed class ConferenceState {
    object Idle : ConferenceState()
    object Creating : ConferenceState()
    object Joining : ConferenceState()
    object Active : ConferenceState()
    object Leaving : ConferenceState()
    data class Failed(val reason: ConferenceFailureReason) : ConferenceState()
}

data class Participant(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val isAudioMuted: Boolean,
    val isVideoEnabled: Boolean,
    val isScreenSharing: Boolean,
    val isHost: Boolean
)
```

## Voice Messaging

### Recording Flow

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  User    │───▶│  Record  │───▶│  Encode  │───▶│  Upload  │
│  Press   │    │  Audio   │    │  OGG/Opus│    │ Supabase │
│  Hold    │    │  PCM     │    │          │    │ Storage  │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
     │                                             │
     │◀──────────── Cancel ────────────────────────│
     │               (discard)                      │
```

### Audio Recording (core:audio)

```kotlin
interface AudioRecorder {
    suspend fun startRecording(config: RecordingConfig): Flow<RecordingEvent>
    suspend fun stopRecording(): AudioFile?
    fun cancelRecording()
    fun observeAmplitude(): Flow<Float>
}

data class RecordingConfig(
    val format: AudioFormat = AudioFormat.OGG_OPUS,
    val sampleRate: Int = 44_100,
    val channels: Int = 1,
    val maxDurationMs: Long = 120_000, // 2 minutes max
    val enableNoiseSuppression: Boolean = true,
    val enableEchoCancellation: Boolean = true
)
```

### Playback Flow

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  Tap     │───▶│ Download │───▶│  Decode  │───▶│  Play    │
│  Voice   │    │  from    │    │  OGG/Opus│    │  Audio   │
│  Note    │    │  CDN     │    │          │    │  Output  │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
```

## 1-to-1 Calls

### Call Flow

```
Caller                    Supabase                   Callee
  │                          │                          │
  │──── 1. Initiate Call ───▶│                          │
  │                          │──── 2. Push Notify ─────▶│
  │                          │                          │
  │                          │◀─── 3. Accept/Decline ───│
  │◀─── 4. Signal Result ────│                          │
  │                          │                          │
  │◀═══════ 5. WebRTC P2P Connection ══════════════════▶│
  │                          │                          │
  │◀═══════ 6. Media Stream (Audio/Video) ═════════════▶│
  │                          │                          │
  │──── 7. End Call ────────▶│──── 8. Notify ──────────▶│
```

### Call States

```kotlin
enum class CallType {
    VOICE,
    VIDEO
}

enum class CallStatus {
    RINGING,
    CONNECTING,
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    MISSED,
    DECLINED,
    FAILED,
    BUSY
}
```

### Incoming Call Handling

1. **FCM Push** arrives with call metadata
2. **CallService** starts foreground service
3. **Full-screen notification** shown (incoming call UI)
4. User **accepts** or **declines**
5. On accept: WebRTC session established
6. On decline: Signal sent to caller, call logged

### Call UI States

```
┌─────────────────────────────────────────┐
│              Incoming Call               │
│  ┌─────────────────────────────────┐    │
│  │     [Avatar]  Caller Name       │    │
│  │         Voice / Video           │    │
│  └─────────────────────────────────┘    │
│                                         │
│  [Decline]              [Accept]        │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│              Active Call                 │
│  ┌─────────────────────────────────┐    │
│  │     [Avatar]  Caller Name       │    │
│  │         00:03:45                │    │
│  └─────────────────────────────────┘    │
│                                         │
│  [Mute] [Speaker] [Video] [End Call]   │
└─────────────────────────────────────────┘
```

## Video Conferencing

### Conference Creation

```kotlin
// Host creates conference
conferenceService.createConference(
    conferenceId = UUID.randomUUID().toString(),
    neighborhoodId = currentNeighborhood.id
)

// Invite participants
conferenceService.inviteParticipant(conferenceId, userId1)
conferenceService.inviteParticipant(conferenceId, userId2)
```

### Conference Layout

```
┌─────────────────────────────────────────┐
│          Conference Room                 │
│  ┌─────────────────┬─────────────────┐  │
│  │                 │                 │  │
│  │    Participant 1│  Participant 2  │  │
│  │    (Speaker)    │                 │  │
│  │                 │                 │  │
│  ├─────────────────┼─────────────────┤  │
│  │                 │                 │  │
│  │    Participant 3│  Participant 4  │  │
│  │                 │    (You)        │  │
│  │                 │                 │  │
│  └─────────────────┴─────────────────┘  │
│                                         │
│  [Mute] [Video] [Share] [Participants] │
│  [Chat] [Leave] [End (Host)]           │
└─────────────────────────────────────────┘
```

### Participant Grid

- **1 participant**: Full screen
- **2 participants**: Side by side
- **3-4 participants**: 2x2 grid
- **5-9 participants**: 3x3 grid
- **10+ participants**: Scrolling grid with active speaker spotlight

### Screen Sharing

```kotlin
// Toggle screen sharing
conferenceService.shareScreen(enabled = true)

// Underlying implementation uses MediaProjection API
// Captures screen, encodes, and sends via WebRTC track
```

## Signaling

### Supabase Realtime Channels

```kotlin
// Call signaling channel
val callChannel = supabase.channel("call:$callId")

// Events
callChannel.onBroadcast<CallOffer>("offer") { offer ->
    // Handle incoming call offer
}
callChannel.onBroadcast<CallAnswer>("answer") { answer ->
    // Handle call answer
}
callChannel.onBroadcast<IceCandidate>("ice-candidate") { candidate ->
    // Handle ICE candidate exchange
}
callChannel.onBroadcast<CallEnd>("end") { _ ->
    // Handle call termination
}
```

### WebRTC Configuration

```kotlin
val rtcConfig = RTCConfiguration(
    iceServers = listOf(
        RTCIceServer(
            urls = listOf("stun:stun.supabase.com:3478"),
            username = null,
            credential = null
        ),
        RTCIceServer(
            urls = listOf("turn:turn.supabase.com:3478"),
            username = config.turnUsername,
            credential = config.turnCredential
        )
    ),
    sdpSemantics = SDPSemantics.UNIFIED_PLAN,
    iceCandidatePoolSize = 2
)
```

## Audio Processing

### Voice Quality

- **Codec**: Opus (OGG container) for voice notes
- **Sample Rate**: 48kHz for calls, 44.1kHz for voice notes
- **Bitrate**: 32kbps for voice, 128kbps for music
- **Noise Suppression**: Built-in WebRTC NS module
- **Echo Cancellation**: Built-in WebRTC AEC module
- **AGC**: Automatic Gain Control enabled by default

### Audio Focus

```kotlin
// Manage audio focus during calls
val audioFocusRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN)
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setUsage(USAGE_VOICE_COMMUNICATION)
            .setContentType(CONTENT_TYPE_SPEECH)
            .build()
    )
    .setOnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AUDIOFOCUS_LOSS -> endCall()
            AUDIOFOCUS_LOSS_TRANSIENT -> muteAudio(true)
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> lowerVolume()
        }
    }
    .build()
```

## Error Handling

### Call Failures

```kotlin
sealed class CallFailureReason {
    object NetworkUnavailable : CallFailureReason()
    object RecipientOffline : CallFailureReason()
    object RecipientBusy : CallFailureReason()
    object RecipientDeclined : CallFailureReason()
    object ConnectionTimeout : CallFailureReason()
    object IceFailed : CallFailureReason()
    object SdpFailed : CallFailureReason()
    data class Unknown(val message: String) : CallFailureReason()
}
```

### Retry Strategy

- **Network failure**: Exponential backoff (1s, 2s, 4s, max 3 retries)
- **ICE failure**: Restart ICE with updated candidates
- **Connection timeout**: 30-second timeout, then fail
- **Audio issues**: Automatic codec renegotiation

## Testing

### Unit Tests

- Mock WebRTC peer connection
- Test call state machine transitions
- Verify signaling message handling
- Test audio/video toggle logic

### Integration Tests

- End-to-end call flow with fake Supabase
- Conference creation and participant management
- Voice note recording and playback
- Error recovery scenarios

### Manual Testing

- Test on 3G/4G/WiFi networks
- Test with airplane mode toggle
- Test with device rotation during call
- Test background/foreground transitions
- Test with Bluetooth headset
