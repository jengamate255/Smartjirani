package com.smartjirani.feature.civic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartEmptyState
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.core.designsystem.component.SmartScaffold
import com.smartjirani.feature.civic.CivicAnalytics
import com.smartjirani.feature.civic.CivicAnnouncement
import com.smartjirani.feature.civic.CivicCategoryCount
import com.smartjirani.feature.civic.CivicDayCount
import com.smartjirani.feature.civic.CivicReport
import com.smartjirani.feature.civic.R
import com.smartjirani.feature.civic.official.CivicOfficialFilter
import com.smartjirani.feature.civic.official.CivicOfficialFilterChips
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

/** Status chip colors — shared by queue cards, detail and the map pins. */
fun civicStatusColor(status: String): Color =
    when (status) {
        "NEW" -> Color(0xFF7F8C9B)
        "AI_TRIAGED" -> Color(0xFF7B61FF)
        "AWAITING_VERIFICATION" -> Color(0xFFF2A93B)
        "VERIFIED" -> Color(0xFF2D9CDB)
        "ASSIGNED" -> Color(0xFF27AE60)
        "IN_PROGRESS" -> Color(0xFF2980B9)
        "WAITING_EXTERNAL_ACTION" -> Color(0xFF8E44AD)
        "AWAITING_RESIDENT_CONFIRMATION" -> Color(0xFFF2994A)
        "RESOLVED" -> Color(0xFF1E8E5A)
        "CLOSED" -> Color(0xFF4F4F4F)
        "REJECTED" -> Color(0xFFBDBDBD)
        "ESCALATED" -> Color(0xFFEB5757)
        else -> Color(0xFF7F8C9B)
    }

private val AnnouncementCategories =
    listOf(
        "NOTICE" to R.string.civic_category_notice,
        "HEALTH" to R.string.civic_category_health,
        "SAFETY" to R.string.civic_category_safety,
        "EVENT" to R.string.civic_category_event,
        "UTILITY" to R.string.civic_category_utility,
    )

private fun categoryLabelRes(category: String): Int = AnnouncementCategories.firstOrNull { it.first == category }?.second ?: R.string.civic_category_notice

@Composable
fun CivicHubRoute(
    onReportClick: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenAdmin: () -> Unit = {},
    onOpenDepartmentConsole: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    viewModel: CivicHubViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.toast) {
        if (uiState.toast != null) {
            delay(2_500)
            viewModel.dismissToast()
        }
    }

    CivicHubScreen(
        uiState = uiState,
        onReportClick = onReportClick,
        onOpenMap = onOpenMap,
        onOpenAdmin = onOpenAdmin,
        onOpenDepartmentConsole = onOpenDepartmentConsole,
        onStatusFilter = viewModel::setStatusFilter,
        onOfficialFilter = viewModel::setOfficialFilter,
        onRefresh = viewModel::refresh,
        onDismissError = viewModel::dismissError,
        onNotificationsClick = onNotificationsClick,
        onAvatarClick = onAvatarClick,
        onPublishAnnouncement = viewModel::publishAnnouncement,
        onDismissToast = viewModel::dismissToast,
        bottomBar = bottomBar,
    )
}

@Composable
fun CivicHubScreen(
    uiState: CivicHubUiState,
    onReportClick: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenAdmin: () -> Unit = {},
    onOpenDepartmentConsole: () -> Unit = {},
    onStatusFilter: (String?) -> Unit,
    onOfficialFilter: (CivicOfficialFilter) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onPublishAnnouncement: (String, String, String, Boolean) -> Unit = { _, _, _, _ -> },
    onDismissToast: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showPublishDialog by remember { mutableStateOf(false) }

    SmartScaffold(
        screenSuffix = stringResource(R.string.civic_title),
        locationLabel = uiState.locationLabel,
        onNotificationsClick = onNotificationsClick,
        onAvatarClick = onAvatarClick,
        bottomBar = bottomBar,
        toastMessage = uiState.toast,
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    SmartLoading()
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        uiState.error?.let { message ->
                            ErrorBanner(
                                message = message,
                                onDismiss = onDismissError,
                                onRetry = onRefresh,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item(key = "announcements-header") {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.civic_announcements_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (uiState.isAdmin) {
                                        IconButton(onClick = onOpenAdmin) {
                                            Icon(
                                                imageVector = Icons.Filled.Settings,
                                                contentDescription = stringResource(R.string.civic_admin_title),
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                    if (uiState.isDepartmentOfficer) {
                                        TextButton(onClick = onOpenDepartmentConsole) {
                                            Icon(
                                                imageVector = Icons.Filled.Build,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.department_officer_console))
                                        }
                                    }
                                    if (uiState.data.isOfficial) {
                                        TextButton(onClick = { showPublishDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Filled.Campaign,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.civic_publish))
                                        }
                                    }
                                }
                            }

                            if (uiState.announcements.isEmpty()) {
                                item(key = "announcements-empty") {
                                    Text(
                                        text = stringResource(R.string.civic_announcement_empty),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                }
                            } else {
                                items(uiState.announcements, key = { "ann-${it.id}" }) { announcement ->
                                    AnnouncementCard(
                                        announcement = announcement,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                }
                            }

                            if (uiState.data.isOfficial) {
                                item(key = "queue-stats") { StatsRow(uiState) }
                                item(key = "analytics") { uiState.analytics?.let { AnalyticsSection(it) } }
                                item(key = "queue-filter") {
                                    CivicOfficialFilterChips(
                                        current = uiState.officialFilter,
                                        onSelect = onOfficialFilter,
                                    )
                                }

                                if (uiState.visibleReports.isEmpty()) {
                                    item(key = "queue-empty") {
                                        SmartEmptyState(
                                            title = stringResource(R.string.civic_title),
                                            message = stringResource(R.string.civic_queue_empty),
                                        )
                                    }
                                } else {
                                    items(uiState.visibleReports, key = { it.id }) { report ->
                                        ReportCard(
                                            report = report,
                                            onClick = { onReportClick(report.id) },
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!uiState.isLoading && uiState.data.isOfficial) {
                IconButton(
                    onClick = onOpenMap,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp),
                            ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = stringResource(R.string.civic_map),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }

    if (showPublishDialog) {
        PublishDialog(
            isPublishing = uiState.isPublishing,
            onPublish = { title, body, category, pinned ->
                showPublishDialog = false
                onPublishAnnouncement(title, body, category, pinned)
            },
            onDismiss = { showPublishDialog = false },
        )
    }
}

@Composable
private fun AnnouncementCard(
    announcement: CivicAnnouncement,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (announcement.pinned) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.extraSmall,
                ) {
                    Text(
                        text = stringResource(categoryLabelRes(announcement.category)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                if (announcement.pinned) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = stringResource(R.string.civic_announcement_pinned),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = publishedAtLabel(announcement.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = announcement.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            announcement.publishedByName?.let { publisher ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "— $publisher",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PublishDialog(
    isPublishing: Boolean,
    onPublish: (String, String, String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("NOTICE") }
    var pinned by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isPublishing) onDismiss() },
        title = { Text(stringResource(R.string.civic_publish_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 200) title = it },
                    label = { Text(stringResource(R.string.civic_publish_title_hint)) },
                    singleLine = true,
                    enabled = !isPublishing,
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= 5000) body = it },
                    label = { Text(stringResource(R.string.civic_publish_body_hint)) },
                    minLines = 3,
                    maxLines = 6,
                    enabled = !isPublishing,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AnnouncementCategories.forEach { (key, labelRes) ->
                        FilterChip(
                            selected = category == key,
                            onClick = { category = key },
                            enabled = !isPublishing,
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = pinned,
                        onCheckedChange = { pinned = it },
                        enabled = !isPublishing,
                    )
                    Text(
                        text = stringResource(R.string.civic_publish_pinned),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onPublish(title, body, category, pinned) },
                enabled = !isPublishing && title.trim().length >= 3 && body.trim().length >= 10,
            ) {
                Text(stringResource(R.string.civic_publish_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isPublishing) {
                Text(stringResource(R.string.civic_common_cancel))
            }
        },
    )
}

/** "2026-10-05T09:30:00.12+00:00" → "5 Okt 2026"; falls back to the raw date part. */
private fun publishedAtLabel(iso: String): String =
    try {
        val parsed =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .parse(iso.substringBefore('.'))
        if (parsed != null) {
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(parsed)
        } else {
            iso.take(10)
        }
    } catch (_: Exception) {
        iso.take(10)
    }

@Composable
private fun StatsRow(uiState: CivicHubUiState) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatCard(stringResource(R.string.civic_stats_total), uiState.total.toString(), Modifier.weight(1f))
        StatCard(stringResource(R.string.civic_stats_active), uiState.active.toString(), Modifier.weight(1f))
        StatCard(
            stringResource(R.string.civic_stats_overdue),
            uiState.overdue.toString(),
            Modifier.weight(1f),
            highlight = uiState.overdue > 0,
        )
        StatCard(
            stringResource(R.string.civic_stats_duplicates),
            uiState.duplicatesFolded.toString(),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (highlight) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color =
                    if (highlight) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color =
                    if (highlight) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

@Composable
private fun ReportCard(
    report: CivicReport,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(report.status)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = report.publicReference,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (report.reportCount > 1) {
                    Text(
                        text = stringResource(R.string.civic_dup_badge_format, report.reportCount),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = report.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                report.categoryName?.let { category ->
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = report.priority,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (report.isOverdue) {
                    Text(
                        text = stringResource(R.string.civic_overdue_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (report.escalationLevel > 0) {
                    Text(
                        text = stringResource(R.string.civic_escalation_badge_format, report.escalationLevel),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    status: String,
    modifier: Modifier = Modifier,
) {
    val color = civicStatusColor(status)
    Box(
        modifier =
            modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = stringResource(statusLabelRes(status)),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun AnalyticsSection(analytics: CivicAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.civic_analytics_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(
                    stringResource(R.string.civic_stats_escalated),
                    analytics.escalated.toString(),
                    Modifier.weight(1f),
                    highlight = analytics.escalated > 0,
                )
                StatCard(
                    stringResource(R.string.civic_stats_avg_resolution),
                    analytics.avgResolutionHours?.let { "%.1f".format(it) } ?: "—",
                    Modifier.weight(1f),
                )
                StatCard(
                    stringResource(R.string.civic_stats_sla),
                    analytics.slaRate?.let { "${(it * 100).roundToInt()}%" } ?: "—",
                    Modifier.weight(1f),
                )
            }

            if (analytics.resolvedTotal > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                SlaBar(analytics)
            }

            if (analytics.trend14d.any { it.count > 0 }) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.civic_analytics_trend),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                TrendChart(analytics.trend14d)
            }

            val topCategories = analytics.byCategory.sortedByDescending { it.count }.take(6)
            if (topCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.civic_analytics_categories),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                val maxCategoryCount = topCategories.maxOf { it.count }.coerceAtLeast(1)
                topCategories.forEach { category ->
                    CategoryBar(category, maxCategoryCount)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun SlaBar(analytics: CivicAnalytics) {
    val metFraction = analytics.slaMet.toFloat() / analytics.resolvedTotal
    Column {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(metFraction)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Text(
                text = stringResource(R.string.civic_sla_met, analytics.slaMet),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.civic_sla_missed, analytics.slaMissed),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrendChart(days: List<CivicDayCount>) {
    val barColor = MaterialTheme.colorScheme.primary
    val stubColor = MaterialTheme.colorScheme.surfaceVariant
    val maxCount = days.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(72.dp),
    ) {
        if (days.isNotEmpty()) {
            val gap = 3.dp.toPx()
            val barWidth = (size.width - gap * (days.size - 1)) / days.size
            days.forEachIndexed { index, day ->
                val barHeight =
                    if (day.count > 0) {
                        (day.count.toFloat() / maxCount) * (size.height - 2.dp.toPx())
                    } else {
                        2.dp.toPx()
                    }
                drawRoundRect(
                    color = if (day.count > 0) barColor else stubColor,
                    topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun CategoryBar(
    category: CivicCategoryCount,
    maxCount: Int,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = category.labelSw,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = category.count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(category.count.toFloat() / maxCount)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
