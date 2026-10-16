package com.smartjirani.feature.conference.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.video.ConferenceStatus
import com.smartjirani.core.video.ui.VideoRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConferenceRoute(
    conferenceId: String,
    onNavigateBack: () -> Unit,
    viewModel: ConferenceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(conferenceId) {
        if (conferenceId.isNotBlank() && uiState.conferenceState.status == ConferenceStatus.IDLE) {
            viewModel.joinConference(conferenceId)
        }
    }

    ConferenceScreen(
        uiState = uiState,
        onToggleMute = viewModel::toggleMute,
        onToggleCamera = viewModel::toggleCamera,
        onSwitchCamera = viewModel::switchCamera,
        onToggleScreenShare = viewModel::toggleScreenShare,
        onLeave = {
            viewModel.leaveConference(conferenceId)
            onNavigateBack()
        },
        onDismissError = viewModel::dismissError,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConferenceScreen(
    uiState: ConferenceUiState,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onLeave: () -> Unit,
    onDismissError: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val conference = uiState.conferenceState
    val isEnded = conference.status == ConferenceStatus.ENDED

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(conference.title.ifBlank { "Conference" })
                        Text(
                            text = conferenceStatusLabel(conference.status, conference.participantCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = isEnded) {
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

            Spacer(modifier = Modifier.height(24.dp))

            // The real media pipeline is not wired up yet, so this tiles the local camera
            // surface and the participant count rather than pretending to stream video.
            LocalVideoTile(
                videoTrack = uiState.localVideoTrack,
                isCameraEnabled = conference.isCameraEnabled,
                isScreenSharing = conference.isScreenSharing,
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (conference.isScreenSharing) {
                Text(
                    text = "You are sharing your screen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "Conference ID: ${conference.conferenceId.ifBlank { "—" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isEnded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ConferenceControl(
                        icon = if (conference.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        label = if (conference.isMuted) "Unmute" else "Mute",
                        isActive = conference.isMuted,
                        onClick = onToggleMute,
                    )
                    ConferenceControl(
                        icon = if (conference.isCameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                        label = if (conference.isCameraEnabled) "Stop video" else "Start video",
                        isActive = conference.isCameraEnabled,
                        onClick = onToggleCamera,
                    )
                    ConferenceControl(
                        icon = Icons.Filled.Cameraswitch,
                        label = "Flip",
                        isActive = false,
                        onClick = onSwitchCamera,
                    )
                    ConferenceControl(
                        icon = Icons.Filled.ScreenShare,
                        label = "Share",
                        isActive = conference.isScreenSharing,
                        onClick = onToggleScreenShare,
                    )
                    ConferenceControl(
                        icon = Icons.Filled.CallEnd,
                        label = "Leave",
                        isActive = false,
                        containerColor = MaterialTheme.colorScheme.error,
                        onClick = onLeave,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalVideoTile(
    videoTrack: io.livekit.android.room.track.VideoTrack?,
    isCameraEnabled: Boolean,
    isScreenSharing: Boolean,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(240.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (videoTrack != null && isCameraEnabled) {
                VideoRenderer(
                    videoTrack = videoTrack,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (!isCameraEnabled && !isScreenSharing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.VideocamOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                    Text(
                        text = "Camera is off",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = if (isScreenSharing) "Sharing screen" else "You",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun ConferenceControl(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier =
                Modifier
                    .size(48.dp)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary else containerColor,
                        CircleShape,
                    ),
        ) {
            Icon(icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun conferenceStatusLabel(
    status: ConferenceStatus,
    participants: Int,
): String =
    when (status) {
        ConferenceStatus.IDLE -> "Not started"
        ConferenceStatus.WAITING -> "Waiting for others to join"
        ConferenceStatus.ACTIVE -> "$participants in the call"
        ConferenceStatus.ENDED -> "Conference ended"
    }
