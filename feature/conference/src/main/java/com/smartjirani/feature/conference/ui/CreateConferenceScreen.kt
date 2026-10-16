package com.smartjirani.feature.conference.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateConferenceRoute(
    onNavigateBack: () -> Unit,
    onConferenceCreated: (String) -> Unit,
    viewModel: ConferenceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var title by remember { mutableStateOf("") }
    var neighborhoodId by remember { mutableStateOf("") }
    var joinId by remember { mutableStateOf("") }

    LaunchedEffect(uiState.createdConferenceId) {
        val id = uiState.createdConferenceId
        if (id != null) onConferenceCreated(id)
    }

    CreateConferenceScreen(
        errorMessage = uiState.error,
        title = title,
        neighborhoodId = neighborhoodId,
        joinId = joinId,
        onTitleChange = { title = it },
        onNeighborhoodIdChange = { neighborhoodId = it },
        onJoinIdChange = { joinId = it },
        onCreate = {
            viewModel.createConference(title.trim(), neighborhoodId.trim())
        },
        onJoin = {
            viewModel.joinConference(joinId.trim())
            onConferenceCreated(joinId.trim())
        },
        onDismissError = viewModel::dismissError,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateConferenceScreen(
    errorMessage: String?,
    title: String,
    neighborhoodId: String,
    joinId: String,
    onTitleChange: (String) -> Unit,
    onNeighborhoodIdChange: (String) -> Unit,
    onJoinIdChange: (String) -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onDismissError: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New conference") },
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
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (errorMessage != null) {
                ErrorBanner(message = errorMessage, onDismiss = onDismissError)
            }

            Text(
                text = "Start a conference",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                value = neighborhoodId,
                onValueChange = onNeighborhoodIdChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Neighborhood ID") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            SmartButton(
                text = "Start conference",
                onClick = onCreate,
                enabled = title.isNotBlank() && neighborhoodId.isNotBlank(),
            )

            Text(
                text = "Or join one",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedTextField(
                value = joinId,
                onValueChange = onJoinIdChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Conference ID") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            SmartButton(
                text = "Join conference",
                onClick = onJoin,
                enabled = joinId.isNotBlank(),
            )
        }
    }
}
