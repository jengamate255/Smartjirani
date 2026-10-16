package com.smartjirani.feature.feed.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartEmptyState
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.core.designsystem.component.SmartScaffold
import com.smartjirani.core.designsystem.theme.SmartSpacing
import com.smartjirani.core.designsystem.theme.UjamaaTerracottaDeep
import com.smartjirani.core.designsystem.theme.UjamaaTerracottaMist
import com.smartjirani.feature.feed.FeedExtras
import com.smartjirani.feature.feed.StreetNotice
import com.smartjirani.feature.feed.StreetPoll
import com.smartjirani.feature.feed.StreetStatus
import kotlinx.coroutines.delay

/**
 * Mtaa Feed: the design-system shell (app top bar + four-tab bottom bar) with the
 * filter chips, the rose notice card, the green street-status strip, the poll card
 * and the post list underneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedRoute(
    onPostClick: (String) -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToCalls: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.toast) {
        if (uiState.toast != null) {
            delay(2_500)
            viewModel.dismissToast()
        }
    }

    FeedScreen(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onRefresh = viewModel::refresh,
        onToggleLike = viewModel::toggleLike,
        onDismissError = viewModel::dismissError,
        onDismissToast = viewModel::dismissToast,
        onVote = viewModel::onVote,
        onPostClick = onPostClick,
        onNavigateToExplore = onNavigateToExplore,
        onNavigateToCreate = onNavigateToCreate,
        onNavigateToCalls = onNavigateToCalls,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToNotifications = onNavigateToNotifications,
        bottomBar = bottomBar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    uiState: FeedUiState,
    onFilterSelected: (FeedFilter) -> Unit,
    onRefresh: () -> Unit,
    onToggleLike: (String) -> Unit,
    onDismissError: () -> Unit,
    onDismissToast: () -> Unit,
    onVote: (String, String) -> Unit,
    onPostClick: (String) -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToCalls: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SmartScaffold(
        screenSuffix = "Pulse ya Mtaa",
        locationLabel = uiState.locationLabel,
        onNotificationsClick = onNavigateToNotifications,
        onAvatarClick = onNavigateToProfile,
        onLocationClick = onNavigateToExplore,
        toastMessage = uiState.toast,
        bottomBar = bottomBar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                    )
                },
                text = { Text("Post mpya") },
            )
        },
        content = { paddingValues ->
            Column(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                uiState.error?.let { message ->
                    ErrorBanner(
                        message = message,
                        onDismiss = onDismissError,
                        onRetry = onRefresh,
                    )
                }

                FeedFilterRow(
                    selected = uiState.selectedFilter,
                    onSelect = onFilterSelected,
                )

                when {
                    uiState.isLoading -> {
                        SmartLoading()
                    }

                    uiState.neighborhoodId == null -> {
                        SmartEmptyState(
                            title = "Chagua mtaa wako",
                            message = "Chagua mtaa wako ili kuona posti za jirani zako karibu nawe.",
                            modifier = Modifier.weight(1f),
                            actionText = "Chagua mtaa",
                            onAction = onNavigateToExplore,
                        )
                    }

                    uiState.isEmpty && uiState.extras.isEmpty -> {
                        SmartEmptyState(
                            title = "Bado hakuna posti",
                            message = "Hakuna posti katika kichujio hiki. Badilisha kichujio au tengeneza posti ya kwanza.",
                            modifier = Modifier.weight(1f),
                            actionText = "Tengeneza posti mpya",
                            onAction = onNavigateToCreate,
                        )
                    }

                    else -> {
                        PullToRefreshBox(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = onRefresh,
                            modifier = Modifier.weight(1f),
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding =
                                    PaddingValues(
                                        horizontal = SmartSpacing.md,
                                        vertical = SmartSpacing.sm,
                                    ),
                                verticalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
                            ) {
                                val extras = uiState.extras

                                extras.notices.forEach { notice ->
                                    item(key = "notice_${notice.id}") {
                                        NoticeCard(notice = notice)
                                    }
                                }

                                extras.streetStatus?.let { status ->
                                    item(key = "street_status") {
                                        StreetStatusStrip(status = status)
                                    }
                                }

                                extras.polls.forEach { poll ->
                                    item(key = "poll_${poll.id}") {
                                        PollCard(
                                            poll = poll,
                                            onVote = onVote,
                                        )
                                    }
                                }

                                items(uiState.visiblePosts, key = { it.id }) { post ->
                                    PostCard(
                                        post = post,
                                        onToggleLike = { onToggleLike(post.id) },
                                        onClick = { onPostClick(post.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedFilterRow(
    selected: FeedFilter,
    onSelect: (FeedFilter) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(
                horizontal = SmartSpacing.md,
                vertical = SmartSpacing.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(SmartSpacing.xs),
    ) {
        items(FeedFilter.entries) { filter ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
                leadingIcon =
                    if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        null
                    },
            )
        }
    }
}

/** Rose notice card pinned above the feed: an official street notice. */
@Composable
private fun NoticeCard(notice: StreetNotice) {
    Surface(
        color = UjamaaTerracottaMist,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(SmartSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Campaign,
                        contentDescription = null,
                        tint = UjamaaTerracottaDeep,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(SmartSpacing.xs))
                    Text(
                        text = notice.eyebrow,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = UjamaaTerracottaDeep,
                    )
                }
                Surface(
                    color = UjamaaTerracottaDeep,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = notice.statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = SmartSpacing.sm, vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(SmartSpacing.xs))

            Text(
                text = notice.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(SmartSpacing.xs))

            Text(
                text = notice.authority,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Green strip with the live street status: temperature, power, gate and the street note. */
@Composable
private fun StreetStatusStrip(status: StreetStatus) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(SmartSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatusChip(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Thermostat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = "${status.temperatureC}°C",
                )
                StatusChip(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = "Umeme: ${status.powerStatus}",
                )
                StatusChip(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = "Lango: ${status.gateStatus}",
                )
            }

            if (status.streetNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(SmartSpacing.xs))
                Text(
                    text = status.streetNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    icon: @Composable () -> Unit,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

/** Poll card: one question, options as percentage bars, one vote per resident. */
@Composable
private fun PollCard(
    poll: StreetPoll,
    onVote: (String, String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(SmartSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.HowToVote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(SmartSpacing.xs))
                Text(
                    text = poll.eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(SmartSpacing.xs))

            Text(
                text = poll.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (poll.subtext.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = poll.subtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(SmartSpacing.sm))

            poll.options.forEach { option ->
                PollOptionRow(
                    label = option.label,
                    percentage = option.percentage,
                    isMine = option.isMine,
                    enabled = !poll.hasVoted || !option.isMine,
                    onClick = { onVote(poll.id, option.id) },
                )
                Spacer(modifier = Modifier.height(SmartSpacing.xs))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${poll.totalVotes} wamejibu",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = poll.endsLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PollOptionRow(
    label: String,
    percentage: Int,
    isMine: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val barColor =
        if (isMine) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (isMine) {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                        )
                    } else {
                        Modifier
                    },
                ).clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(fraction = percentage.coerceIn(0, 100) / 100f)
                    .fillMaxHeight()
                    .background(barColor),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = SmartSpacing.sm),
        ) {
            if (isMine) {
                Icon(
                    imageVector = Icons.Filled.HowToVote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(SmartSpacing.xs))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isMine) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
