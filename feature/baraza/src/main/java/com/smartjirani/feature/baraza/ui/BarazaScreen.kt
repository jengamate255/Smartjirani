package com.smartjirani.feature.baraza.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartEmptyState
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.core.designsystem.component.SmartTopBar
import com.smartjirani.core.designsystem.component.ToastPill
import com.smartjirani.core.designsystem.theme.SmartSpacing
import com.smartjirani.core.designsystem.theme.UjamaaEmerald
import com.smartjirani.core.designsystem.theme.UjamaaMint
import com.smartjirani.core.designsystem.theme.UjamaaTerracotta
import com.smartjirani.core.designsystem.theme.UjamaaTerracottaMist
import com.smartjirani.feature.baraza.BarazaEvent
import com.smartjirani.feature.baraza.ForumThread
import com.smartjirani.feature.baraza.HarambeeProject
import com.smartjirani.feature.baraza.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarazaRoute(
    onNavigateBack: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    viewModel: BarazaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.toast) {
        if (uiState.toast != null) {
            delay(2_500)
            viewModel.dismissToast()
        }
    }

    BarazaScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRsvpToggled = viewModel::onRsvpToggled,
        onAskQuestion = viewModel::onAskQuestionClicked,
        onVoteToggled = viewModel::onVoteToggled,
        onVolunteerToggled = viewModel::onVolunteerToggled,
        onRefresh = viewModel::refresh,
        onDismissError = viewModel::dismissError,
        onDismissToast = viewModel::dismissToast,
        onNotificationsClick = onNotificationsClick,
        onAvatarClick = onAvatarClick,
        onLocationClick = onNavigateToExplore,
        bottomBar = bottomBar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarazaScreen(
    uiState: BarazaUiState,
    onNavigateBack: () -> Unit,
    onRsvpToggled: (String, Boolean) -> Unit,
    onAskQuestion: (String) -> Unit = {},
    onVoteToggled: (String, Boolean) -> Unit,
    onVolunteerToggled: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onDismissToast: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SmartTopBar(
                screenSuffix = stringResource(R.string.baraza_title),
                locationLabel = uiState.locationLabel,
                onNotificationsClick = onNotificationsClick,
                onAvatarClick = onAvatarClick,
                onLocationClick = onLocationClick,
            )
        },
        bottomBar = bottomBar,
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                uiState.error?.let { message ->
                    ErrorBanner(
                        message = message,
                        onDismiss = onDismissError,
                        onRetry = onRefresh,
                    )
                }

                when {
                    uiState.isLoading -> {
                        SmartLoading()
                    }

                    uiState.neighborhoodId == null -> {
                        SmartEmptyState(
                            title = stringResource(R.string.baraza_select_neighborhood),
                            message = stringResource(R.string.baraza_select_neighborhood_message),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    uiState.content.isEmpty -> {
                        SmartEmptyState(
                            title = stringResource(R.string.baraza_empty_title),
                            message = stringResource(R.string.baraza_empty_message),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    else -> {
                        PullToRefreshBox(
                            isRefreshing = false,
                            onRefresh = onRefresh,
                            modifier = Modifier.weight(1f),
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(SmartSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(SmartSpacing.md),
                            ) {
                                uiState.content.events.firstOrNull()?.let { event ->
                                    item(key = "council-banner") {
                                        CouncilBanner(event = event)
                                    }
                                }

                                if (uiState.content.events.isNotEmpty()) {
                                    item(key = "events-header") {
                                        SectionHeader(
                                            icon = Icons.Filled.Event,
                                            title = stringResource(R.string.baraza_upcoming_meetings),
                                            tint = UjamaaTerracotta,
                                        )
                                    }
                                    items(uiState.content.events, key = { "event-${it.id}" }) { event ->
                                        EventCard(
                                            event = event,
                                            onRsvpToggled = { attending ->
                                                onRsvpToggled(event.id, attending)
                                            },
                                            onAskQuestion = { onAskQuestion(event.id) },
                                        )
                                    }
                                }

                                if (uiState.content.projects.isNotEmpty()) {
                                    item(key = "projects-header") {
                                        SectionHeader(
                                            icon = Icons.Filled.VolunteerActivism,
                                            title = stringResource(R.string.baraza_harambee_projects),
                                            tint = UjamaaEmerald,
                                        )
                                    }
                                    items(uiState.content.projects, key = { "project-${it.id}" }) { project ->
                                        ProjectCard(
                                            project = project,
                                            onVolunteerToggled = { joined ->
                                                onVolunteerToggled(project.id, joined)
                                            },
                                        )
                                    }
                                }

                                if (uiState.content.threads.isNotEmpty()) {
                                    item(key = "threads-header") {
                                        SectionHeader(
                                            icon = Icons.Filled.Groups,
                                            title = stringResource(R.string.baraza_forum_threads),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    items(uiState.content.threads, key = { "thread-${it.id}" }) { thread ->
                                        ThreadCard(
                                            thread = thread,
                                            onVoteToggled = { voted ->
                                                onVoteToggled(thread.id, voted)
                                            },
                                        )
                                    }
                                }

                                item(key = "start-initiative-cta") {
                                    StartInitiativeCta()
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.toast != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = SmartSpacing.sm),
            ) {
                uiState.toast?.let { toast ->
                    ToastPill(message = stringResource(toast.messageId, *toast.args.toTypedArray()))
                }
            }
        }
    }
}

@Composable
private fun CouncilBanner(event: BarazaEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(modifier = Modifier.padding(SmartSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(UjamaaEmerald),
                        )
                        Spacer(Modifier.width(SmartSpacing.xs))
                        Text(
                            text = stringResource(R.string.baraza_live),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = event.councilLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = event.committeeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(SmartSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BannerStat(
                    label = stringResource(R.string.baraza_area),
                    value = event.districtLabel,
                )
                BannerStat(
                    label = stringResource(R.string.baraza_houses),
                    value = event.householdsLabel,
                )
                BannerStat(
                    label = "Quorum",
                    value = event.quorumLabel,
                    valueColor = UjamaaTerracotta,
                )
            }
        }
    }
}

@Composable
private fun BannerStat(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.primary,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

@Composable
private fun EventCard(
    event: BarazaEvent,
    onRsvpToggled: (Boolean) -> Unit,
    onAskQuestion: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(SmartSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .width(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = event.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = event.dateLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = event.timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.width(SmartSpacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (event.mode.equals("HYBRID", ignoreCase = true)) {
                            Spacer(Modifier.width(SmartSpacing.xs))
                            Text(
                                text = stringResource(R.string.baraza_hybrid),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = UjamaaTerracotta,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = event.venue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.height(SmartSpacing.sm))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp),
                        ).padding(SmartSpacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.baraza_main_agenda),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(SmartSpacing.xs))
                event.agenda.take(3).forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(SmartSpacing.xs))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (event.streamNote.isNotBlank()) {
                Spacer(Modifier.height(SmartSpacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(SmartSpacing.xs))
                    Text(
                        text = event.streamNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(SmartSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.baraza_attending_format, event.attendingCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(SmartSpacing.sm))
                    Text(
                        text = stringResource(R.string.baraza_questions_format, event.questionsQueued),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(SmartSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
            ) {
                Button(
                    onClick = { onRsvpToggled(!event.iAmAttending) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = if (event.iAmAttending) UjamaaEmerald else MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Icon(
                        imageVector = if (event.iAmAttending) Icons.Filled.Check else Icons.Filled.Event,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(SmartSpacing.sm))
                    Text(
                        text = if (event.iAmAttending) stringResource(R.string.baraza_rsvp_registered) else stringResource(R.string.baraza_rsvp_register),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    onClick = onAskQuestion,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(SmartSpacing.xs))
                    Text(
                        text = stringResource(R.string.baraza_ask_question),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: HarambeeProject,
    onVolunteerToggled: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(SmartSpacing.md)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (project.phaseLabel.isNotBlank()) {
                            Spacer(Modifier.width(SmartSpacing.xs))
                            Text(
                                text = project.phaseLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(SmartSpacing.xs))
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = project.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = project.iconLabel,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            if (project.hasMoneyGoal) {
                Spacer(Modifier.height(SmartSpacing.sm))
                Text(
                    text = stringResource(R.string.baraza_raised_format, project.raisedKes.toInt().toLocaleString(), project.targetKes.toInt().toLocaleString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(SmartSpacing.xs))
                LinearProgressIndicator(
                    progress = { project.progressPct / 100f },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = project.progressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End),
                )
            }

            Spacer(Modifier.height(SmartSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.VolunteerActivism,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(SmartSpacing.xs))
                    Text(
                        text = project.volunteerNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (project.isJoinable) {
                    Button(
                        onClick = { onVolunteerToggled(!project.iAmVolunteering) },
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = if (project.iAmVolunteering) UjamaaMint else MaterialTheme.colorScheme.primary,
                                contentColor = if (project.iAmVolunteering) UjamaaEmerald else UjamaaMint,
                            ),
                        contentPadding = PaddingValues(horizontal = SmartSpacing.md, vertical = SmartSpacing.xs),
                    ) {
                        Text(
                            text = if (project.iAmVolunteering) stringResource(R.string.baraza_joined) else stringResource(R.string.baraza_join),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.baraza_closed),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreadCard(
    thread: ForumThread,
    onVoteToggled: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(modifier = Modifier.padding(SmartSpacing.md)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(40.dp),
            ) {
                IconButton(
                    onClick = { onVoteToggled(!thread.votedByMe) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = stringResource(R.string.baraza_vote),
                        tint =
                            if (thread.votedByMe) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = "${thread.votes}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (thread.votedByMe) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }

            Spacer(Modifier.width(SmartSpacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = thread.authorName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (thread.tag.isNotBlank()) {
                        Spacer(Modifier.width(SmartSpacing.xs))
                        Text(
                            text = thread.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (thread.solved) {
                        Spacer(Modifier.width(SmartSpacing.xs))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.baraza_solved),
                            modifier = Modifier.size(14.dp),
                            tint = UjamaaEmerald,
                        )
                    }
                }

                Text(
                    text = thread.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = SmartSpacing.xs),
                )
                Text(
                    text = thread.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(SmartSpacing.sm))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SmartSpacing.md),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = stringResource(R.string.baraza_replies_format, thread.replyCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StartInitiativeCta() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = UjamaaTerracottaMist,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(SmartSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(UjamaaTerracotta),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(SmartSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.baraza_start_initiative),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = UjamaaTerracotta,
                )
                Text(
                    text = stringResource(R.string.baraza_start_initiative_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    tint: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SmartSpacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun Int.toLocaleString(): String =
    java.text.NumberFormat
        .getNumberInstance()
        .format(this)
