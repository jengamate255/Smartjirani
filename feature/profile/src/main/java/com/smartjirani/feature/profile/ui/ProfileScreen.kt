package com.smartjirani.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartButton
import com.smartjirani.core.designsystem.component.SmartEmptyState
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.core.designsystem.component.SmartScaffold
import com.smartjirani.core.designsystem.component.SmartTextField
import com.smartjirani.core.designsystem.component.UserAvatar
import com.smartjirani.core.designsystem.theme.SmartSpacing
import com.smartjirani.feature.profile.KarmaSummary
import com.smartjirani.feature.profile.NeighborReview
import com.smartjirani.feature.profile.PrivacySettings
import com.smartjirani.feature.profile.SecurityCircle
import com.smartjirani.feature.profile.SkillOffer
import kotlinx.coroutines.delay

/**
 * Resident Profile: karma, valuables locker, skills, reviews, the safety circle with the
 * SOS switch, privacy controls, and the Calls/Explore rows that used to live in the
 * bottom bar. Edit-profile stays as the last section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCalls: () -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
    onNavigateToCivic: (() -> Unit)? = null,
    onNavigateToCivicNewReport: () -> Unit = {},
    onNavigateToCivicMyReports: () -> Unit = {},
    onNavigateToCivicAssistant: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.toast) {
        if (uiState.toast != null) {
            delay(2_500)
            viewModel.dismissToast()
        }
    }

    var displayName by remember(uiState.profile) { mutableStateOf(uiState.profile?.displayName ?: "") }
    var username by remember(uiState.profile) { mutableStateOf(uiState.profile?.username ?: "") }
    var bio by remember(uiState.profile) { mutableStateOf(uiState.profile?.bio ?: "") }

    SmartScaffold(
        screenSuffix = "Wasifu",
        locationLabel = uiState.locationLabel,
        onNotificationsClick = { },
        onAvatarClick = { },
        onLocationClick = onNavigateToExplore,
        toastMessage = uiState.toast,
        bottomBar = bottomBar,
        content = { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(SmartSpacing.md),
                verticalArrangement = Arrangement.spacedBy(SmartSpacing.md),
            ) {
                uiState.error?.let { message ->
                    ErrorBanner(
                        message = message,
                        onDismiss = viewModel::clearError,
                        onRetry = viewModel::refresh,
                    )
                }

                when {
                    uiState.isLoading && uiState.profile == null -> {
                        SmartLoading()
                    }

                    uiState.profile == null -> {
                        SmartEmptyState(
                            title = "Hakuna wasifu",
                            message = "Ingia tena ili kuona wasifu wako.",
                        )
                    }

                    else -> {
                        ProfileHeader(
                            name = uiState.profile?.displayName.orEmpty(),
                            username = uiState.profile?.username.orEmpty(),
                            bio = uiState.profile?.bio,
                            avatarUrl = uiState.profile?.avatarUrl,
                            rankLabel = uiState.extras.karma.rankLabel,
                        )

                        KarmaGrid(karma = uiState.extras.karma)

                        QuickActionRow(
                            icon = { Icon(Icons.Filled.Call, contentDescription = null) },
                            label = "Simu na Ujumbe",
                            onClick = onNavigateToCalls,
                        )
                        QuickActionRow(
                            icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                            label = "Chinjua Mtaa",
                            onClick = onNavigateToExplore,
                        )
                        QuickActionRow(
                            icon = { Icon(Icons.Filled.Report, contentDescription = null) },
                            label = "Ripoti Mtaa",
                            onClick = onNavigateToCivicNewReport,
                        )
                        QuickActionRow(
                            icon = { Icon(Icons.Filled.History, contentDescription = null) },
                            label = "Ripoti Zangu",
                            onClick = onNavigateToCivicMyReports,
                        )
                        QuickActionRow(
                            icon = { Icon(Icons.Filled.SmartToy, contentDescription = null) },
                            label = "Msaidizi wa AI",
                            onClick = onNavigateToCivicAssistant,
                        )
                        onNavigateToCivic?.let { openCivic ->
                            QuickActionRow(
                                icon = { Icon(Icons.Filled.AccountBalance, contentDescription = null) },
                                label = "Serikali (Baraza la Serikali)",
                                onClick = openCivic,
                            )
                        }

                        if (uiState.isExtrasLoading) {
                            SmartLoading()
                        }

                        if (uiState.extras.lockerItems.isNotEmpty()) {
                            SectionCard(title = "Sanduku la Thamani") {
                                uiState.extras.lockerItems.forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = SmartSpacing.xs),
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = item.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        PillLabel(
                                            text = item.stateLabel,
                                            container =
                                                if (item.isAvailable) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                },
                                            content =
                                                if (item.isAvailable) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.extras.skills.isNotEmpty()) {
                            SectionCard(title = "Kipaji Chako") {
                                uiState.extras.skills.forEach { skill ->
                                    SkillRow(skill = skill)
                                    Spacer(modifier = Modifier.height(SmartSpacing.xs))
                                }
                            }
                        }

                        if (uiState.extras.reviews.isNotEmpty()) {
                            SectionCard(title = "Maoni ya Majirani") {
                                uiState.extras.reviews.forEach { review ->
                                    ReviewRow(review = review)
                                    Spacer(modifier = Modifier.height(SmartSpacing.xs))
                                }
                            }
                        }

                        uiState.extras.securityCircle?.let { circle ->
                            SafetySection(
                                circle = circle,
                                onSosToggle = viewModel::onSosToggle,
                            )
                        }

                        uiState.extras.privacy?.let { privacy ->
                            PrivacySection(
                                privacy = privacy,
                                onHouseNumberToggle = viewModel::onHouseNumberToggle,
                            )
                        }

                        if (uiState.isBiometricAvailable) {
                            SectionCard(title = "Usalama") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(
                                        Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.width(SmartSpacing.sm))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Linda kwa vidole",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "Uliza vidole ukirudi kwenye app",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Switch(
                                        checked = uiState.appLockEnabled,
                                        onCheckedChange = viewModel::onAppLockToggle,
                                    )
                                }
                            }
                        }

                        if (uiState.extras.household.isNotEmpty()) {
                            SectionCard(title = "Wanaoishi Nyumbani") {
                                uiState.extras.household.forEach { member ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = SmartSpacing.xs),
                                    ) {
                                        Text(
                                            text = member.memberName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            text = member.relationLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        SectionCard(title = "Hariri Wasifu") {
                            SmartTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = "Jina Lako",
                            )
                            Spacer(modifier = Modifier.height(SmartSpacing.sm))
                            SmartTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = "Jina Mtumiaji",
                            )
                            Spacer(modifier = Modifier.height(SmartSpacing.sm))
                            SmartTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = "Kuhusu Wewe",
                                singleLine = false,
                            )
                            Spacer(modifier = Modifier.height(SmartSpacing.sm))
                            SmartButton(
                                text = "Hifadhi Wasifu",
                                onClick = {
                                    viewModel.updateProfile(
                                        displayName,
                                        username,
                                        bio.ifBlank { null },
                                    )
                                },
                                isLoading = uiState.isLoading,
                            )
                            if (uiState.isSaved) {
                                Spacer(modifier = Modifier.height(SmartSpacing.xs))
                                Text(
                                    text = "Wasifu umehifadhiwa",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(SmartSpacing.sm))
                        SmartButton(
                            text = "Toka Account",
                            onClick = onLogoutClick,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ProfileHeader(
    name: String,
    username: String,
    bio: String?,
    avatarUrl: String?,
    rankLabel: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(SmartSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            UserAvatar(
                avatarUrl = avatarUrl,
                contentDescription = name,
                size = 88,
            )
            Spacer(modifier = Modifier.height(SmartSpacing.sm))
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (username.isNotBlank()) {
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rankLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(SmartSpacing.xs))
                PillLabel(
                    text = rankLabel,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (!bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(SmartSpacing.sm))
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun KarmaGrid(karma: KarmaSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(SmartSpacing.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(SmartSpacing.xs)) {
            KarmaTile(
                label = "Karma",
                value = String.format("%.1f", karma.score),
                modifier = Modifier.weight(1f),
            )
            KarmaTile(
                label = "Uaminifu",
                value = "${karma.trustPct}%",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SmartSpacing.xs)) {
            KarmaTile(
                label = "Matendo",
                value = "${karma.actionsCount}",
                modifier = Modifier.weight(1f),
            )
            KarmaTile(
                label = "Favors",
                value = "${karma.favorsDone}",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SmartSpacing.xs)) {
            KarmaTile(
                label = "Vilivyoshirikiwa",
                value = "${karma.sharedItems}",
                modifier = Modifier.weight(1f),
            )
            KarmaTile(
                label = "Baraza",
                value = "${karma.barazas}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KarmaTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(SmartSpacing.sm)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(SmartSpacing.md),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(SmartSpacing.sm))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(SmartSpacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(SmartSpacing.sm))
            content()
        }
    }
}

@Composable
private fun SkillRow(skill: SkillOffer) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = skill.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            PillLabel(
                text = "${skill.endorsementCount} wamethibitisha",
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        if (skill.description.isNotBlank()) {
            Text(
                text = skill.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (skill.testimonial.isNotBlank()) {
            Text(
                text = skill.testimonial,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReviewRow(review: NeighborReview) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = review.authorLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(review.rating.coerceIn(0, 5)) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
            text = review.body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SafetySection(
    circle: SecurityCircle,
    onSosToggle: (Boolean) -> Unit,
) {
    SectionCard(title = "Ulinda Wako") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(SmartSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = circle.clusterLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${circle.designationLabel} · mawasiliano ${circle.contacts.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = circle.sosEnabled,
                onCheckedChange = onSosToggle,
            )
        }
        Spacer(modifier = Modifier.height(SmartSpacing.xs))
        Text(
            text =
                if (circle.sosEnabled) {
                    "SOS imewashwa: waalike wako wataarifiwa ukihitaji msaada."
                } else {
                    "SOS imezimwa."
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrivacySection(
    privacy: PrivacySettings,
    onHouseNumberToggle: (Boolean) -> Unit,
) {
    SectionCard(title = "Faragha") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Onyesha namba ya nyumba",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = privacy.houseNumberVisibility,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = privacy.showHouseNumber,
                onCheckedChange = onHouseNumberToggle,
            )
        }
        Spacer(modifier = Modifier.height(SmartSpacing.xs))
        Text(
            text = "Mtaa unaonekana: ${privacy.visibilityLabel} (${privacy.visibilityRadiusM} m)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Kaya: ${privacy.householdLabel} · ${privacy.statusLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PillLabel(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(container)
                .padding(horizontal = SmartSpacing.sm, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
    }
}
