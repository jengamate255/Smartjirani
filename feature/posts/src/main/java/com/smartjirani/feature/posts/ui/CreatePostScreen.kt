package com.smartjirani.feature.posts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.SmartButton
import com.smartjirani.core.designsystem.component.SmartTextField
import com.smartjirani.feature.posts.PostType
import com.smartjirani.feature.posts.label

private const val MAX_CONTENT_LENGTH = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit,
    viewModel: PostsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val neighborhoodId by viewModel.neighborhoodId.collectAsState()

    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PostType.GENERAL) }

    // Publishing succeeded: return to the feed, then clear the one-shot flag so returning
    // to the composer later does not immediately pop back out.
    LaunchedEffect(uiState.isPostCreated) {
        if (uiState.isPostCreated) {
            viewModel.clearCreated()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tengeneza Posti") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Rudi",
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Aina ya posti",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(PostType.entries) { type ->
                    FilterChip(
                        selected = type == selectedType,
                        onClick = { selectedType = type },
                        label = { Text(type.label()) },
                    )
                }
            }

            SmartTextField(
                value = content,
                onValueChange = { if (it.length <= MAX_CONTENT_LENGTH) content = it },
                label = "Unachoma nini?",
                singleLine = false,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp),
            )

            Text(
                text = "${content.length} / $MAX_CONTENT_LENGTH",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
            )

            val errorMessage = uiState.error
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            SmartButton(
                text = "Chapisha",
                onClick = { viewModel.createPost(content = content, postType = selectedType) },
                enabled = content.isNotBlank() && neighborhoodId != null,
                isLoading = uiState.isLoading,
                modifier = Modifier.padding(top = 16.dp),
            )

            if (neighborhoodId == null) {
                Text(
                    text = "Chagua mtaa wako ili kuweza kuchapisha posti.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                )
            }
        }
    }
}
