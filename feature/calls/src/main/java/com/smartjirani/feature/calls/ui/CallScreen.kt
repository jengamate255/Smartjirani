package com.smartjirani.feature.calls.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.UserAvatar
import com.smartjirani.core.video.CallStatus
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallRoute(
    onNavigateBack: () -> Unit,
    viewModel: CallsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    CallScreen(
        uiState = uiState,
        onAccept = viewModel::acceptCall,
        onReject = viewModel::rejectCall,
        onEnd = viewModel::endCall,
        onToggleMute = viewModel::toggleMute,
        onToggleSpeaker = viewModel::toggleSpeaker,
        onDismissError = viewModel::dismissError,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallScreen(
    uiState: CallsUiState,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onEnd: (String) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onDismissError: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val call = uiState.callState
    val isRinging = call.status == CallStatus.RINGING
    val isFinished =
        call.status == CallStatus.ENDED ||
            call.status == CallStatus.MISSED ||
            call.status == CallStatus.DECLINED

    // Leaving while a call is live would orphan the media session, so swallow back until it ends.
    BackHandler(enabled = !isFinished) { }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = isFinished) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.error != null) {
                ErrorBanner(message = uiState.error, onDismiss = onDismissError)
            }

            Spacer(modifier = Modifier.height(48.dp))

            UserAvatar(
                avatarUrl = call.remoteParticipantAvatar,
                contentDescription = call.remoteParticipantName.ifBlank { null },
                size = 120,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = call.remoteParticipantName.ifBlank { "Unknown" },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = callStatusLabel(call.status, call.duration),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (isRinging) {
                IncomingCallControls(
                    onAccept = { onAccept(call.callId) },
                    onReject = { onReject(call.callId) },
                )
            } else if (!isFinished) {
                ActiveCallControls(
                    isMuted = call.isMuted,
                    isSpeakerOn = call.isSpeakerOn,
                    onToggleMute = onToggleMute,
                    onToggleSpeaker = onToggleSpeaker,
                    onEnd = { onEnd(call.callId) },
                )
            }
        }
    }
}

@Composable
private fun IncomingCallControls(
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledIconButton(
                onClick = onReject,
                modifier =
                    Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape),
            ) {
                Icon(Icons.Filled.CallEnd, contentDescription = "Decline")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Decline", style = MaterialTheme.typography.labelMedium)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledIconButton(
                onClick = onAccept,
                modifier =
                    Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
            ) {
                Icon(Icons.Filled.Call, contentDescription = "Accept")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Answer", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ActiveCallControls(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEnd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CallControlButton(
            icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            label = if (isMuted) "Muted" else "Mute",
            contentDescription = if (isMuted) "Unmute" else "Mute",
            isActive = isMuted,
            onClick = onToggleMute,
        )
        CallControlButton(
            icon = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
            label = "Speaker",
            contentDescription = if (isSpeakerOn) "Speaker off" else "Speaker on",
            isActive = isSpeakerOn,
            onClick = onToggleSpeaker,
        )
        CallControlButton(
            icon = Icons.Filled.CallEnd,
            label = "End",
            contentDescription = "End call",
            containerColor = MaterialTheme.colorScheme.error,
            onClick = onEnd,
        )
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier =
                Modifier
                    .size(56.dp)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary else containerColor,
                        CircleShape,
                    ),
        ) {
            Icon(icon, contentDescription = contentDescription)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun callStatusLabel(
    status: CallStatus,
    durationMs: Long,
): String =
    when (status) {
        CallStatus.IDLE -> "Ready to call"
        CallStatus.RINGING -> "Ringing…"
        CallStatus.ACTIVE -> formatCallDuration(durationMs)
        CallStatus.ENDED -> "Call ended"
        CallStatus.MISSED -> "Missed call"
        CallStatus.DECLINED -> "Call declined"
    }

private fun formatCallDuration(durationMs: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs.coerceAtLeast(0L))
    return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}
