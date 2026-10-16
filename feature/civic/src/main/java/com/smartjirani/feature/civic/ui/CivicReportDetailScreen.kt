package com.smartjirani.feature.civic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.feature.civic.CivicAction
import com.smartjirani.feature.civic.CivicActionKind
import com.smartjirani.feature.civic.CivicAssignment
import com.smartjirani.feature.civic.CivicHistoryEntry
import com.smartjirani.feature.civic.CivicUnit
import com.smartjirani.feature.civic.R

private enum class DialogState { NONE, REASON, ASSIGN, DUPLICATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicReportDetailRoute(
    reportId: String,
    onNavigateBack: () -> Unit,
    viewModel: CivicReportDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(reportId) { viewModel.load(reportId) }

    CivicReportDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRefresh = viewModel::refresh,
        onDismissError = viewModel::dismissError,
        onAction = viewModel::performAction,
        onAssign = viewModel::assign,
        onMarkDuplicate = viewModel::markDuplicate,
        onClearRoutingSuggestion = viewModel::clearRoutingSuggestion,
        onAnalyzeReport = viewModel::analyzeReport,
        onClearAnalysis = viewModel::clearAnalysis,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicReportDetailScreen(
    uiState: CivicDetailUiState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onAction: (CivicAction, String?) -> Unit,
    onAssign: (String, String?) -> Unit,
    onMarkDuplicate: (String) -> Unit,
    onClearRoutingSuggestion: () -> Unit,
    onAnalyzeReport: () -> Unit,
    onClearAnalysis: () -> Unit,
) {
    var dialog by remember { mutableStateOf(DialogState.NONE) }
    var pendingAction by remember { mutableStateOf<CivicAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.detail?.report?.publicReference ?: "")
                },
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val detail = uiState.detail
            when {
                uiState.isLoading && detail == null -> {
                    SmartLoading()
                }

                detail == null -> {
                    Unit
                }

                else -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
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

                        ReportHeader(detail, uiState, onAnalyzeReport)

                        if (uiState.routingSuggestion != null || uiState.isLoadingRouting) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val suggestion = uiState.routingSuggestion
                            AiRoutingSection(
                                suggestion = suggestion,
                                isLoading = uiState.isLoadingRouting,
                                onAccept = {
                                    if (suggestion != null) {
                                        val suggestedUnit =
                                            uiState.units.firstOrNull {
                                                it.name.equals(suggestion.suggestedDepartment, ignoreCase = true)
                                            }
                                        if (suggestedUnit != null) {
                                            onAssign(suggestedUnit.id, suggestion.routingReason)
                                        }
                                    }
                                },
                                onDismiss = onClearRoutingSuggestion,
                            )
                        }

                        if (uiState.analysisResult != null || uiState.isAnalyzing) {
                            Spacer(modifier = Modifier.height(16.dp))
                            AnalysisSection(
                                analysis = uiState.analysisResult,
                                isLoading = uiState.isAnalyzing,
                                onDismiss = onClearAnalysis,
                            )
                        }

                        ActionSection(
                            actions = uiState.actions,
                            busy = uiState.busy,
                            showDuplicate = uiState.roles.any { it.isOfficial },
                            onAction = { action ->
                                pendingAction = action
                                dialog =
                                    when {
                                        action.kind == CivicActionKind.ASSIGN -> DialogState.ASSIGN
                                        action.needsReason -> DialogState.REASON
                                        else -> DialogState.NONE
                                    }
                                if (action.kind != CivicActionKind.ASSIGN && !action.needsReason) {
                                    onAction(action, null)
                                }
                            },
                            onOpenDuplicateDialog = { dialog = DialogState.DUPLICATE },
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        TimelineSection(detail.history)

                        if (detail.assignments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            AssignmentsSection(detail.assignments, uiState.units)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            if (uiState.busy) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }

    when (dialog) {
        DialogState.REASON -> {
            ReasonDialog(
                onConfirm = { reason ->
                    pendingAction?.let { onAction(it, reason.ifBlank { null }) }
                    dialog = DialogState.NONE
                },
                onDismiss = { dialog = DialogState.NONE },
            )
        }

        DialogState.ASSIGN -> {
            AssignDialog(
                units = uiState.units,
                onConfirm = { unitId, note ->
                    onAssign(unitId, note.ifBlank { null })
                    dialog = DialogState.NONE
                },
                onDismiss = { dialog = DialogState.NONE },
            )
        }

        DialogState.DUPLICATE -> {
            DuplicateDialog(
                onConfirm = { reference ->
                    onMarkDuplicate(reference)
                    dialog = DialogState.NONE
                },
                onDismiss = { dialog = DialogState.NONE },
            )
        }

        DialogState.NONE -> {
            Unit
        }
    }
}

@Composable
private fun ReportHeader(
    detail: com.smartjirani.feature.civic.CivicReportDetail,
    uiState: CivicDetailUiState,
    onAnalyzeReport: () -> Unit,
) {
    val report = detail.report
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(report.status)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = report.categoryName ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${report.severity} / ${report.priority}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = report.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (report.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            report.imageUrl?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = url,
                    contentDescription = stringResource(R.string.civic_detail_photo),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAnalyzeReport,
                    enabled = !uiState.isAnalyzing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.civic_detail_analyzing))
                    } else {
                        Text(stringResource(R.string.civic_detail_analyze_image))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            report.locationDescription?.let { location ->
                Text(
                    text = stringResource(R.string.civic_detail_location) + ": " + location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            report.dueAt?.let { due ->
                Text(
                    text = stringResource(R.string.civic_detail_due) + ": " + due.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (report.isOverdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            report.resolutionDescription?.let { resolution ->
                Text(
                    text = stringResource(R.string.civic_detail_resolution) + ": " + resolution,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun AiRoutingSection(
    suggestion: com.smartjirani.feature.civic.CivicAiRoutingSuggestion?,
    isLoading: Boolean,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.civic_ai_routing_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (suggestion != null) {
                    Text(
                        text = if (suggestion.mock) "Mock" else suggestion.provider,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    )
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp).width(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.civic_ai_routing_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            } else if (suggestion != null) {
                Spacer(modifier = Modifier.height(12.dp))

                RoutingRow(
                    label = stringResource(R.string.civic_ai_routing_department),
                    value = suggestion.suggestedDepartment,
                )
                Spacer(modifier = Modifier.height(6.dp))
                RoutingRow(
                    label = stringResource(R.string.civic_ai_routing_reason),
                    value = suggestion.routingReason,
                )
                Spacer(modifier = Modifier.height(6.dp))
                RoutingRow(
                    label = stringResource(R.string.civic_ai_routing_priority),
                    value = suggestion.priority,
                )
                Spacer(modifier = Modifier.height(6.dp))
                RoutingRow(
                    label = stringResource(R.string.civic_ai_routing_sla),
                    value = suggestion.slaHours.toString(),
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.civic_ai_routing_dismiss))
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.civic_ai_routing_accept))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutingRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label + ": ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AnalysisSection(
    analysis: com.smartjirani.feature.civic.CivicAiAnalysisResult?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.civic_ai_analysis_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (analysis != null) {
                    Text(
                        text = if (analysis.mock) "Mock" else analysis.provider,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    )
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp).width(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.civic_ai_analysis_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            } else if (analysis != null) {
                Spacer(modifier = Modifier.height(12.dp))

                AnalysisRow(
                    label = stringResource(R.string.civic_ai_analysis_area),
                    value = analysis.affectedArea,
                )
                Spacer(modifier = Modifier.height(6.dp))
                AnalysisRow(
                    label = stringResource(R.string.civic_ai_analysis_population),
                    value = analysis.populationImpact,
                )
                Spacer(modifier = Modifier.height(6.dp))
                AnalysisRow(
                    label = stringResource(R.string.civic_ai_analysis_infrastructure),
                    value = analysis.infrastructureType,
                )
                Spacer(modifier = Modifier.height(6.dp))
                AnalysisRow(
                    label = stringResource(R.string.civic_ai_analysis_severity),
                    value = analysis.severityEstimate,
                )

                if (analysis.suggestedActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.civic_ai_analysis_actions),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    analysis.suggestedActions.forEach { action ->
                        Text(
                            text = "• $action",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                if (analysis.urgencyFactors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.civic_ai_analysis_urgency),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    analysis.urgencyFactors.forEach { factor ->
                        Text(
                            text = "• $factor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.civic_ai_analysis_dismiss))
                }
            }
        }
    }
}

@Composable
private fun AnalysisRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label + ": ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionSection(
    actions: List<CivicAction>,
    busy: Boolean,
    showDuplicate: Boolean,
    onAction: (CivicAction) -> Unit,
    onOpenDuplicateDialog: () -> Unit,
) {
    Spacer(modifier = Modifier.height(16.dp))
    if (actions.isEmpty() && !showDuplicate) {
        Text(
            text = stringResource(R.string.civic_detail_no_actions),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { action ->
            Button(
                enabled = !busy,
                onClick = { onAction(action) },
            ) {
                Text(text = stringResource(actionLabelRes(action)))
            }
        }
        if (showDuplicate) {
            TextButton(enabled = !busy, onClick = onOpenDuplicateDialog) {
                Text(text = stringResource(R.string.civic_action_mark_duplicate))
            }
        }
    }
}

private fun actionLabelRes(action: CivicAction): Int =
    when {
        action.kind == CivicActionKind.ASSIGN -> R.string.civic_action_assign
        action.kind == CivicActionKind.CONFIRM -> R.string.civic_action_confirm_resolution
        action.kind == CivicActionKind.REOPEN -> R.string.civic_action_reopen
        action.kind == CivicActionKind.APPEAL -> R.string.civic_action_appeal
        action.toStatus == "AWAITING_VERIFICATION" -> R.string.civic_action_send_verification
        action.toStatus == "AI_TRIAGED" -> R.string.civic_action_ai_triage
        action.toStatus == "VERIFIED" -> R.string.civic_action_verify
        action.toStatus == "REJECTED" -> R.string.civic_action_reject
        action.toStatus == "ESCALATED" -> R.string.civic_action_escalate
        action.toStatus == "IN_PROGRESS" -> R.string.civic_action_start_work
        action.toStatus == "WAITING_EXTERNAL_ACTION" -> R.string.civic_action_external
        action.toStatus == "AWAITING_RESIDENT_CONFIRMATION" -> R.string.civic_action_request_confirm
        action.toStatus == "CLOSED" -> R.string.civic_action_close
        else -> R.string.civic_action_continue_work
    }

@Composable
private fun ReasonDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.civic_dialog_reason_title)) },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text(stringResource(R.string.civic_dialog_reason_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason) }) {
                Text(stringResource(R.string.civic_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.civic_common_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignDialog(
    units: List<com.smartjirani.feature.civic.CivicUnit>,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedUnit by remember { mutableStateOf<com.smartjirani.feature.civic.CivicUnit?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var showUnitError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.civic_dialog_assign_title)) },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedUnit?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.civic_action_assign)) },
                        isError = showUnitError && selectedUnit == null,
                        trailingIcon = { Text(if (expanded) "▲" else "▼") },
                        modifier =
                            Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = {
                                    selectedUnit = unit
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                if (showUnitError && selectedUnit == null) {
                    Text(
                        text = stringResource(R.string.civic_error_select_unit),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(stringResource(R.string.civic_dialog_assign_note_hint)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val unit = selectedUnit
                    if (unit == null) {
                        showUnitError = true
                    } else {
                        onConfirm(unit.id, note)
                    }
                },
            ) {
                Text(stringResource(R.string.civic_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.civic_common_cancel))
            }
        },
    )
}

@Composable
private fun DuplicateDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reference by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.civic_dialog_dup_title)) },
        text = {
            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                placeholder = { Text(stringResource(R.string.civic_dialog_dup_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = reference.isNotBlank(),
                onClick = { onConfirm(reference) },
            ) {
                Text(stringResource(R.string.civic_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.civic_common_cancel))
            }
        },
    )
}

@Composable
private fun TimelineSection(history: List<CivicHistoryEntry>) {
    Text(
        text = stringResource(R.string.civic_detail_timeline),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            history.forEachIndexed { index, entry ->
                Row {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier =
                                Modifier
                                    .height(10.dp)
                                    .width(10.dp)
                                    .background(
                                        civicStatusColor(entry.toStatus),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                    ),
                        )
                        if (index < history.size - 1) {
                            Box(
                                modifier =
                                    Modifier
                                        .width(2.dp)
                                        .height(28.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(entry.toStatus)
                            entry.changedByRole?.let { role ->
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = role,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        entry.reason?.let { reason ->
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            text = entry.createdAt.take(16).replace('T', ' '),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < history.size - 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun AssignmentsSection(
    assignments: List<CivicAssignment>,
    units: List<CivicUnit>,
) {
    Text(
        text = stringResource(R.string.civic_detail_assignments),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    assignments.forEach { assignment ->
        val unitName = assignment.orgUnitId?.let { unitId -> units.firstOrNull { it.id == unitId }?.name }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text =
                        unitName
                            ?: stringResource(R.string.civic_history_unspecified),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                assignment.note?.let { note ->
                    Text(text = note, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = assignment.assignedAt.take(16).replace('T', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}
