package com.smartjirani.feature.civic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.feature.civic.CivicReport
import com.smartjirani.feature.civic.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicMyReportsRoute(
    onNavigateBack: () -> Unit,
    onReportClick: (String) -> Unit,
    viewModel: CivicMyReportsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    CivicMyReportsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onReportClick = onReportClick,
        onStatusFilter = viewModel::setStatusFilter,
        onRefresh = viewModel::load,
        onDismissError = viewModel::dismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicMyReportsScreen(
    uiState: CivicMyReportsUiState,
    onNavigateBack: () -> Unit,
    onReportClick: (String) -> Unit,
    onStatusFilter: (String?) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.civic_my_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.civic_back),
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
        ) {
            uiState.error?.let { message ->
                ErrorBanner(
                    message = message,
                    onDismiss = onDismissError,
                    onRetry = onRefresh,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.statusFilter == null,
                    onClick = { onStatusFilter(null) },
                    label = { Text(stringResource(R.string.civic_my_filter_all)) },
                )
                uiState.availableStatuses.forEach { status ->
                    FilterChip(
                        selected = uiState.statusFilter == status,
                        onClick = { onStatusFilter(status) },
                        label = { Text(stringResource(statusLabelRes(status))) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                uiState.isLoading -> {
                    SmartLoading()
                }

                uiState.visibleReports.isEmpty() -> {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.civic_my_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.visibleReports, key = { it.id }) { report ->
                            MyReportCard(report = report, onClick = { onReportClick(report.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyReportCard(
    report: CivicReport,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(report.status)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = report.publicReference,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (report.isOverdue) {
                    Text(
                        text = stringResource(R.string.civic_overdue_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = report.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = report.createdAt.take(10),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
