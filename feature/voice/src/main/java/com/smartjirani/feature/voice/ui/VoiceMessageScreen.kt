package com.smartjirani.feature.voice.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartEmptyState
import com.smartjirani.feature.voice.VoiceMessage
import java.util.concurrent.TimeUnit

private const val MAX_RECORDING_MS = 120_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceMessageRoute(
    peerId: String,
    onNavigateBack: () -> Unit,
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(peerId) { viewModel.loadMessages(peerId) }

    // Ask once when the user actually reaches for the mic, rather than on screen entry.
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) viewModel.startRecording()
        }

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    VoiceMessageScreen(
        uiState = uiState,
        onRequestRecord = {
            if (hasMicPermission()) {
                viewModel.startRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onSendRecording = { viewModel.stopRecordingAndSend(peerId.takeIf { it.isNotBlank() }) },
        onCancelRecording = viewModel::cancelRecording,
        onPlay = viewModel::playMessage,
        onDelete = viewModel::deleteMessage,
        onDismissError = viewModel::dismissError,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceMessageScreen(
    uiState: VoiceUiState,
    onRequestRecord: () -> Unit,
    onSendRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onPlay: (filePath: String, messageId: String) -> Unit,
    onDelete: (String) -> Unit,
    onDismissError: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    // Auto-stop at two minutes so a forgotten recording can't run up a huge file.
    LaunchedEffect(uiState.isRecording, uiState.recordingDuration) {
        if (uiState.isRecording && uiState.recordingDuration >= MAX_RECORDING_MS) {
            onSendRecording()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice notes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        bottomBar = {
            RecordBar(
                isRecording = uiState.isRecording,
                durationMs = uiState.recordingDuration,
                onStart = onRequestRecord,
                onSend = onSendRecording,
                onCancel = onCancelRecording,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (uiState.error != null) {
                ErrorBanner(message = uiState.error, onDismiss = onDismissError)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.messages.isEmpty()) {
                    SmartEmptyState(
                        title = "No voice notes",
                        message = "Hold a conversation with your neighbors using short voice notes.",
                    )
                } else {
                    LazyColumn(
                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 16.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items = uiState.messages, key = { it.id }) { message ->
                            VoiceMessageRow(
                                message = message,
                                isPlaying = uiState.playingMessageId == message.id,
                                onPlay = { onPlay(message.storagePath, message.id) },
                                onDelete = { onDelete(message.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceMessageRow(
    message: VoiceMessage,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Play",
                )
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
            ) {
                Text(
                    text = formatDuration(message.durationMs),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text =
                        TimeUnit.MILLISECONDS
                            .toMinutes(
                                message.createdAt.let {
                                    System.currentTimeMillis() - it
                                },
                            ).let { "$it min ago" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete voice note",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecordBar(
    isRecording: Boolean,
    durationMs: Long,
    onStart: () -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isRecording) {
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 16.dp),
            )
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Cancel recording",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(modifier = Modifier.size(8.dp))
            FilledIconButton(onClick = onSend) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Send voice note",
                    modifier = Modifier.size(28.dp),
                )
            }
        } else {
            FilledIconButton(onClick = onStart, modifier = Modifier.size(64.dp)) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Record voice note",
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs.coerceAtLeast(0L))
    return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
