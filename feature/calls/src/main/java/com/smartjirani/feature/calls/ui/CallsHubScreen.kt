package com.smartjirani.feature.calls.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.UserAvatar
import com.smartjirani.core.video.CallStatus
import com.smartjirani.core.video.CallType

/**
 * Entry point for communication: starts 1-to-1 calls and hands off to the conference flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsHubRoute(
    onNavigateBack: () -> Unit,
    onCallStarted: (String) -> Unit,
    onNavigateToCreateConference: () -> Unit,
    onNavigateToVoiceMessage: (String) -> Unit,
    viewModel: CallsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var calleeId by remember { mutableStateOf("") }

    LaunchedEffect(uiState.startedCallId) {
        val id = uiState.startedCallId
        if (id != null) onCallStarted(id)
    }

    CallsHubScreen(
        uiState = uiState,
        calleeId = calleeId,
        onCalleeIdChange = { calleeId = it },
        onStartVoiceCall = {
            viewModel.startCall(calleeId.trim(), CallType.ONE_TO_ONE)
        },
        onDismissError = viewModel::dismissError,
        onNavigateToCreateConference = onNavigateToCreateConference,
        onSendVoiceNote = { onNavigateToVoiceMessage(calleeId.trim()) },
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsHubScreen(
    uiState: CallsUiState,
    calleeId: String,
    onCalleeIdChange: (String) -> Unit,
    onStartVoiceCall: () -> Unit,
    onDismissError: () -> Unit,
    onNavigateToCreateConference: () -> Unit,
    onSendVoiceNote: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val canStart = calleeId.isNotBlank()
    val call = uiState.callState
    val isCallLive = call.status == CallStatus.RINGING || call.status == CallStatus.ACTIVE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calls") },
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
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.error != null) {
                ErrorBanner(message = uiState.error, onDismiss = onDismissError)
            }

            if (isCallLive) {
                LiveCallBanner(
                    remoteName = call.remoteParticipantName,
                    status = call.status,
                )
            }

            Text(
                text = "Start a call",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Enter the user ID of a neighbor to reach them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = calleeId,
                onValueChange = onCalleeIdChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("User ID") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onStartVoiceCall,
                    enabled = canStart,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Call", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onSendVoiceNote,
                    enabled = canStart,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Voice note", style = MaterialTheme.typography.labelLarge)
                }
            }

            Text(
                text = "Group",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Card(
                onClick = onNavigateToCreateConference,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                    ) {
                        Text("Video conference", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Start a group call with your neighborhood",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Filled.Videocam, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun LiveCallBanner(
    remoteName: String,
    status: CallStatus,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                avatarUrl = null,
                contentDescription = remoteName.ifBlank { null },
                size = 40,
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = remoteName.ifBlank { "Ongoing call" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (status == CallStatus.RINGING) "Ringing…" else "In progress",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
