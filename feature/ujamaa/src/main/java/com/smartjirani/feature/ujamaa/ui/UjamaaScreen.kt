package com.smartjirani.feature.ujamaa.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartEmptyState
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.core.designsystem.component.SmartTopBar
import com.smartjirani.core.designsystem.component.ToastPill
import com.smartjirani.core.designsystem.theme.SmartSpacing
import com.smartjirani.core.designsystem.theme.UjamaaEmerald
import com.smartjirani.core.designsystem.theme.UjamaaMint
import com.smartjirani.core.designsystem.theme.UjamaaTerracotta
import com.smartjirani.core.designsystem.theme.UjamaaTerracottaContainer
import com.smartjirani.core.designsystem.theme.UjamaaTerracottaDeep
import com.smartjirani.feature.ujamaa.HarambeeFund
import com.smartjirani.feature.ujamaa.R
import com.smartjirani.feature.ujamaa.SharedResource
import com.smartjirani.feature.ujamaa.UjamaaFilter
import com.smartjirani.feature.ujamaa.UrgentNeed
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UjamaaRoute(
    onNavigateBack: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onOpenMap: () -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    viewModel: UjamaaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.toast) {
        if (uiState.toast != null) {
            delay(2_500)
            viewModel.dismissToast()
        }
    }

    UjamaaScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onFilterSelected = viewModel::onFilterSelected,
        onOfferHelp = viewModel::onOfferHelp,
        onRequestResource = viewModel::onRequestResource,
        onContribute = viewModel::onContribute,
        onRefresh = viewModel::refresh,
        onOfferItemClick = viewModel::onOfferItemClicked,
        onAskHelpClick = viewModel::onAskHelpClicked,
        onOpenMap = onOpenMap,
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
fun UjamaaScreen(
    uiState: UjamaaUiState,
    onNavigateBack: () -> Unit,
    onFilterSelected: (UjamaaFilter) -> Unit,
    onOfferHelp: (String, Boolean) -> Unit,
    onRequestResource: (String) -> Unit,
    onContribute: (Double) -> Unit,
    onRefresh: () -> Unit,
    onOfferItemClick: () -> Unit = {},
    onAskHelpClick: () -> Unit = {},
    onOpenMap: () -> Unit = {},
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
                screenSuffix = stringResource(R.string.ujamaa_title),
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
                            title = stringResource(R.string.ujamaa_select_neighborhood),
                            message = stringResource(R.string.ujamaa_select_neighborhood_message),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    uiState.content.isEmpty -> {
                        SmartEmptyState(
                            title = stringResource(R.string.ujamaa_empty_title),
                            message = stringResource(R.string.ujamaa_empty_message),
                            actionText = stringResource(R.string.ujamaa_view_map),
                            onAction = onOpenMap,
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
                                item(key = "solidarity-ticker") {
                                    SolidarityTicker(
                                        neighborsHelped = uiState.content.stats.neighborsHelped,
                                        activeBorrows = uiState.content.stats.activeBorrows,
                                    )
                                }

                                item(key = "action-launcher") {
                                    ActionLauncher(
                                        onOfferClick = onOfferItemClick,
                                        onAskClick = onAskHelpClick,
                                    )
                                }

                                item(key = "filter-chips") {
                                    FilterChipRow(
                                        selected = uiState.selectedFilter,
                                        onFilterSelected = onFilterSelected,
                                    )
                                }

                                if (uiState.content.needs.isNotEmpty()) {
                                    item(key = "needs-header") {
                                        SectionHeader(
                                            icon = Icons.Filled.Warning,
                                            title = stringResource(R.string.ujamaa_urgent_needs),
                                            tint = UjamaaTerracotta,
                                        )
                                    }
                                    items(uiState.content.needs, key = { "need-${it.id}" }) { need ->
                                        UrgentNeedCard(
                                            need = need,
                                            onOfferHelp = { offer -> onOfferHelp(need.id, offer) },
                                        )
                                    }
                                }

                                item(key = "resources-header") {
                                    SectionHeader(
                                        icon = Icons.Filled.Handshake,
                                        title = stringResource(R.string.ujamaa_share_resources),
                                        tint = UjamaaEmerald,
                                        trailingLabel = stringResource(R.string.ujamaa_view_map),
                                        onTrailingClick = onOpenMap,
                                    )
                                }
                                items(uiState.visibleResources, key = { "resource-${it.id}" }) { resource ->
                                    ResourceCard(
                                        resource = resource,
                                        onRequest = { onRequestResource(resource.id) },
                                    )
                                }

                                uiState.content.fund?.let { fund ->
                                    item(key = "harambee-fund") {
                                        HarambeeFundCard(
                                            fund = fund,
                                            isContributing = uiState.isContributing,
                                            onContribute = onContribute,
                                        )
                                    }
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
private fun SolidarityTicker(
    neighborsHelped: Int,
    activeBorrows: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = UjamaaEmerald,
            ),
    ) {
        Column(
            modifier = Modifier.padding(SmartSpacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(UjamaaTerracottaContainer),
                )
                Spacer(Modifier.width(SmartSpacing.xs))
                Text(
                    text = stringResource(R.string.ujamaa_ticker_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = UjamaaMint,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.VolunteerActivism,
                    contentDescription = null,
                    tint = UjamaaMint,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.height(SmartSpacing.xs))

            Text(
                text = stringResource(R.string.ujamaa_ticker_neighbors_format, neighborsHelped),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.ujamaa_ticker_period),
                style = MaterialTheme.typography.labelMedium,
                color = UjamaaMint,
            )

            Spacer(Modifier.height(SmartSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
            ) {
                TickerBadge(
                    icon = Icons.Filled.SyncAlt,
                    label = stringResource(R.string.ujamaa_ticker_loans_format, activeBorrows),
                    modifier = Modifier.weight(1f),
                )
                TickerBadge(
                    icon = Icons.Filled.Favorite,
                    label = stringResource(R.string.ujamaa_ticker_interest),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TickerBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(
                    UjamaaEmerald.copy(alpha = 0.6f),
                    RoundedCornerShape(8.dp),
                ).padding(horizontal = SmartSpacing.sm, vertical = SmartSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SmartSpacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = UjamaaMint,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActionLauncher(
    onOfferClick: () -> Unit,
    onAskClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
    ) {
        Button(
            onClick = onOfferClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = UjamaaEmerald,
                ),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(SmartSpacing.xs))
            Text(
                text = stringResource(R.string.ujamaa_offer_item),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        OutlinedButton(
            onClick = onAskClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = UjamaaTerracottaDeep,
                ),
        ) {
            Icon(
                imageVector = Icons.Filled.PanTool,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(SmartSpacing.xs))
            Text(
                text = stringResource(R.string.ujamaa_request_help),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    selected: UjamaaFilter,
    onFilterSelected: (UjamaaFilter) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
    ) {
        items(UjamaaFilter.entries) { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (filter == selected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = UjamaaEmerald,
                        selectedLabelColor = Color.White,
                    ),
            )
        }
    }
}

@Composable
private fun UrgentNeedCard(
    need: UrgentNeed,
    onOfferHelp: (Boolean) -> Unit,
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
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier =
                    Modifier
                        .width(6.dp)
                        .height(120.dp)
                        .background(
                            UjamaaTerracotta,
                            RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                        ),
            )
            Column(modifier = Modifier.padding(SmartSpacing.md)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SmartSpacing.xs),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(UjamaaTerracotta),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.White,
                            )
                        }
                        Text(
                            text = stringResource(R.string.ujamaa_urgent_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = UjamaaTerracottaDeep,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = need.statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = UjamaaTerracotta,
                    )
                }

                Spacer(Modifier.height(SmartSpacing.xs))

                Text(
                    text = need.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = need.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(SmartSpacing.sm))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp),
                            ).padding(SmartSpacing.sm),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = need.requesterLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = UjamaaEmerald,
                            )
                        }
                        Text(
                            text = need.requesterMeta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = need.distanceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(SmartSpacing.sm))

                Button(
                    onClick = { onOfferHelp(!need.iOfferedHelp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = if (need.iOfferedHelp) UjamaaMint else UjamaaTerracotta,
                            contentColor = if (need.iOfferedHelp) UjamaaEmerald else Color.White,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolunteerActivism,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(SmartSpacing.xs))
                    Text(
                        text = if (need.iOfferedHelp) stringResource(R.string.ujamaa_help_offered) else stringResource(R.string.ujamaa_can_help),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceCard(
    resource: SharedResource,
    onRequest: () -> Unit,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = resource.tagLabel.take(2).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = resource.distanceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = resource.historyLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = resource.tagLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = UjamaaEmerald,
                )
            }

            Spacer(Modifier.height(SmartSpacing.sm))

            Row(
                horizontalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
            ) {
                if (resource.imageUrl != null) {
                    AsyncImage(
                        model = resource.imageUrl,
                        contentDescription = resource.title,
                        modifier =
                            Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resource.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = resource.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(SmartSpacing.xs))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.EventAvailable,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = UjamaaEmerald,
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = resource.availabilityNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = UjamaaEmerald,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = resource.depositNote,
                        style = MaterialTheme.typography.labelSmall,
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
                Text(
                    text = if (resource.isAvailable) stringResource(R.string.ujamaa_free_to_borrow) else stringResource(R.string.ujamaa_unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onRequest,
                    enabled = resource.isAvailable && !resource.iRequested,
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = UjamaaEmerald,
                        ),
                    contentPadding = PaddingValues(horizontal = SmartSpacing.md, vertical = SmartSpacing.xs),
                ) {
                    Text(
                        text = if (resource.iRequested) stringResource(R.string.ujamaa_request_sent) else resource.ctaLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HarambeeFundCard(
    fund: HarambeeFund,
    isContributing: Boolean,
    onContribute: (Double) -> Unit,
) {
    var showContributionSheet by remember { mutableStateOf(false) }

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
                horizontalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(UjamaaTerracottaContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolunteerActivism,
                        contentDescription = null,
                        tint = UjamaaTerracottaDeep,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fund.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = fund.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(SmartSpacing.md))

            Text(
                text = stringResource(R.string.ujamaa_fund_raised_format, fund.raisedKes.toLong().toLocaleString(), fund.targetKes.toLong().toLocaleString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(SmartSpacing.xs))
            LinearProgressIndicator(
                progress = { fund.progressPct / 100f },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                color = UjamaaTerracotta,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = fund.progressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = UjamaaTerracotta,
                )
                Text(
                    text = fund.remainingLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(SmartSpacing.sm))

            Text(
                text = fund.purposeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = fund.auditLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(SmartSpacing.md))

            Button(
                onClick = { showContributionSheet = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isContributing,
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = UjamaaTerracotta,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Filled.VolunteerActivism,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(SmartSpacing.xs))
                Text(
                    text = if (isContributing) stringResource(R.string.ujamaa_contributing) else stringResource(R.string.ujamaa_contribute_mpesa),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (showContributionSheet) {
        ContributionBottomSheet(
            onDismiss = { showContributionSheet = false },
            onContribute = { amount ->
                showContributionSheet = false
                onContribute(amount)
            },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ContributionBottomSheet(
    onDismiss: () -> Unit,
    onContribute: (Double) -> Unit,
) {
    val amounts = listOf(50.0, 100.0, 200.0, 500.0, 1000.0)
    var selectedAmount by remember { mutableStateOf(100.0) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(SmartSpacing.lg),
        ) {
            Text(
                text = stringResource(R.string.ujamaa_select_amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.ujamaa_contribute_mpesa),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(SmartSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
            ) {
                amounts.take(3).forEach { amount ->
                    OutlinedButton(
                        onClick = { selectedAmount = amount },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedAmount == amount) UjamaaEmerald.copy(alpha = 0.1f) else Color.Transparent,
                            ),
                    ) {
                        Text(
                            text = stringResource(R.string.ujamaa_amount_format, amount.toInt()),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(SmartSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SmartSpacing.sm),
            ) {
                amounts.drop(3).forEach { amount ->
                    OutlinedButton(
                        onClick = { selectedAmount = amount },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedAmount == amount) UjamaaEmerald.copy(alpha = 0.1f) else Color.Transparent,
                            ),
                    ) {
                        Text(
                            text = stringResource(R.string.ujamaa_amount_format, amount.toInt()),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.weight(2f))
            }

            Spacer(Modifier.height(SmartSpacing.lg))

            Button(
                onClick = { onContribute(selectedAmount) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = UjamaaTerracotta,
                    ),
            ) {
                Text(
                    text = stringResource(R.string.ujamaa_pay_format, selectedAmount.toInt()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(SmartSpacing.md))
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color,
    trailingLabel: String? = null,
    onTrailingClick: (() -> Unit)? = null,
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
            modifier = Modifier.weight(1f),
        )
        if (trailingLabel != null && onTrailingClick != null) {
            TextButton(onClick = onTrailingClick) {
                Text(
                    text = trailingLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = UjamaaEmerald,
                )
            }
        }
    }
}

private fun Long.toLocaleString(): String =
    java.text.NumberFormat
        .getNumberInstance()
        .format(this)
