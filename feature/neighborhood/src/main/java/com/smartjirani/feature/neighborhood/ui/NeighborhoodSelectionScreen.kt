package com.smartjirani.feature.neighborhood.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.feature.neighborhood.Neighborhood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeighborhoodSelectionScreen(
    onNeighborhoodSelected: () -> Unit,
    viewModel: NeighborhoodViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.selectedNeighborhood) {
        if (uiState.selectedNeighborhood != null) {
            onNeighborhoodSelected()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Select Your Neighborhood") })

        if (uiState.isLoading) {
            SmartLoading()
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(items = uiState.neighborhoods, key = { it.id }) { neighborhood ->
                    NeighborhoodItem(
                        neighborhood = neighborhood,
                        onClick = { viewModel.selectNeighborhood(neighborhood) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NeighborhoodItem(
    neighborhood: Neighborhood,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = neighborhood.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${neighborhood.city}, ${neighborhood.region}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
