package com.smartjirani.feature.civic.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.theme.SmartJiraniTheme
import com.smartjirani.feature.civic.*
import com.smartjirani.feature.civic.R
import com.smartjirani.feature.civic.official.CivicOfficialFilter
import com.smartjirani.feature.civic.official.CivicOfficialFilterChips

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DepartmentOfficerConsoleRoute(
    onReportClick: (String) -> Unit,
    viewModel: DepartmentOfficerConsoleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    DepartmentOfficerConsoleScreen(
        uiState = uiState,
        onFilterChange = viewModel::setFilter,
        onReportClick = onReportClick,
        onBatchAssign = { viewModel.batchAssign(uiState.selectedReportIds) },
        onBatchVerify = { viewModel.batchVerify(uiState.selectedReportIds) },
        onSelectionChange = viewModel::updateSelection,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DepartmentOfficerConsoleScreen(
    uiState: DepartmentOfficerConsoleUiState,
    onFilterChange: (CivicOfficialFilter) -> Unit,
    onReportClick: (String) -> Unit,
    onBatchAssign: () -> Unit,
    onBatchVerify: () -> Unit,
    onSelectionChange: (Set<String>) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.department_officer_console)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                KpiStrip(
                    open = uiState.analytics.active,
                    overdue = uiState.analytics.overdue,
                    slaAtRisk = uiState.analytics.slaMissed,
                    resolvedToday = uiState.analytics.resolvedTotal,
                )
            }

            item {
                CivicOfficialFilterChips(
                    current = uiState.filter,
                    onSelect = onFilterChange,
                )
            }

            if (uiState.selectedReportIds.isNotEmpty()) {
                item {
                    BatchActionBar(
                        selectedCount = uiState.selectedReportIds.size,
                        onAssign = onBatchAssign,
                        onVerify = onBatchVerify,
                    )
                }
            }

            items(uiState.reports, key = { it.id }) { report ->
                DepartmentReportRow(
                    report = report,
                    isSelected = report.id in uiState.selectedReportIds,
                    onClick = { onReportClick(report.id) },
                    onLongClick = {
                        val newSelection = uiState.selectedReportIds.toMutableSet()
                        if (report.id in newSelection) {
                            newSelection.remove(report.id)
                        } else {
                            newSelection.add(report.id)
                        }
                        onSelectionChange(newSelection)
                    },
                )
            }

            if (uiState.reports.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_reports_in_queue),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiStrip(
    open: Int,
    overdue: Int,
    slaAtRisk: Int,
    resolvedToday: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KpiCard(
            label = stringResource(id = R.string.kpi_open),
            value = open,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        KpiCard(
            label = stringResource(id = R.string.kpi_overdue),
            value = overdue,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        KpiCard(
            label = stringResource(id = R.string.kpi_sla_at_risk),
            value = slaAtRisk,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
        KpiCard(
            label = stringResource(id = R.string.kpi_resolved_today),
            value = resolvedToday,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BatchActionBar(
    selectedCount: Int,
    onAssign: () -> Unit,
    onVerify: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$selectedCount ${stringResource(id = R.string.selected)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAssign) {
                    Text(stringResource(id = R.string.batch_assign))
                }
                TextButton(onClick = onVerify) {
                    Text(stringResource(id = R.string.batch_verify))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DepartmentReportRow(
    report: CivicReport,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.publicReference ?: report.id.take(8),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (report.categoryName != null) {
                    Text(
                        text = report.categoryName!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val timeStatus =
                    report.dueAt?.let { dueAt ->
                        val now = java.time.Instant.now()
                        val due = java.time.Instant.parse(dueAt)
                        val hoursLeft =
                            java.time.Duration
                                .between(now, due)
                                .toHours()
                        val leftText = stringResource(id = R.string.status_left)
                        when {
                            hoursLeft < 0 -> stringResource(id = R.string.status_overdue) to MaterialTheme.colorScheme.error
                            hoursLeft < 3 -> "${hoursLeft}h $leftText" to MaterialTheme.colorScheme.tertiary
                            else -> "${hoursLeft}h $leftText" to MaterialTheme.colorScheme.secondary
                        }
                    }
                timeStatus?.let { (text, color) ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                }
                Text(
                    text = report.severity,
                    style = MaterialTheme.typography.labelSmall,
                    color = civicStatusColor(report.status),
                )
            }
        }
    }
}
