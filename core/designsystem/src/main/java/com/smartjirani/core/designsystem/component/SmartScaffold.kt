package com.smartjirani.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smartjirani.core.designsystem.R
import com.smartjirani.core.designsystem.theme.SmartSpacing

/**
 * The shared shell used by every top-level screen: fixed header (emblem, wordmark,
 * screen suffix, location chip, notifications bell, avatar) with an optional
 * four-tab bottom bar, matching the SmartJirani design system.
 *
 * [currentTab] is null for screens that do not show the bottom bar. Screens that receive their
 * tab bar from the nav graph pass [bottomBar] instead, which wins over [currentTab].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartScaffold(
    screenSuffix: String,
    locationLabel: String,
    unreadCount: Int = 0,
    avatarUrl: String? = null,
    currentTab: AppTab? = null,
    onTabSelected: (AppTab) -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    onNotificationsClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    toastMessage: String? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Scaffold(
            topBar = {
                SmartTopBar(
                    screenSuffix = screenSuffix,
                    locationLabel = locationLabel,
                    unreadCount = unreadCount,
                    avatarUrl = avatarUrl,
                    onNotificationsClick = onNotificationsClick,
                    onAvatarClick = onAvatarClick,
                    onLocationClick = onLocationClick,
                )
            },
            bottomBar = {
                if (bottomBar != null) {
                    bottomBar()
                } else if (currentTab != null) {
                    AppBottomBar(currentTab = currentTab, onTabSelected = onTabSelected)
                }
            },
            floatingActionButton = floatingActionButton,
            containerColor = MaterialTheme.colorScheme.background,
            content = content,
        )

        if (toastMessage != null) {
            ToastPill(
                message = toastMessage,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 16.dp),
            )
        }
    }
}

/**
 * The shell header on its own, for screens that keep their own [Scaffold] (Ujamaa Hub,
 * Baraza) but still need the branded wordmark, location chip, bell and avatar.
 */
@Composable
fun SmartTopBar(
    screenSuffix: String,
    locationLabel: String,
    unreadCount: Int = 0,
    avatarUrl: String? = null,
    onNotificationsClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(4.dp)
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(64.dp)
                .padding(horizontal = SmartSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.smartjirani_emblem),
            contentDescription = null,
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(SmartSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SmartJirani",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (screenSuffix.isNotBlank()) {
                    Spacer(modifier = Modifier.width(SmartSpacing.xs))
                    Text(
                        text = "· $screenSuffix",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            LocationChip(label = locationLabel, onClick = onLocationClick)
        }
        BellButton(unreadCount = unreadCount, onClick = onNotificationsClick)
        Spacer(modifier = Modifier.width(SmartSpacing.xs))
        AvatarButton(url = avatarUrl, onClick = onAvatarClick)
    }
}

@Composable
private fun LocationChip(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .padding(top = 2.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun BellButton(
    unreadCount: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Box {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = if (unreadCount > 0) "$unreadCount notifications unread" else "Notifications",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (unreadCount > 0) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                )
            }
        }
    }
}

@Composable
private fun AvatarButton(
    url: String?,
    onClick: () -> Unit,
) {
    Box {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = "Profile",
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                UserAvatar(
                    avatarUrl = null,
                    contentDescription = "Profile",
                    size = 32,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(1.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
        )
    }
}
