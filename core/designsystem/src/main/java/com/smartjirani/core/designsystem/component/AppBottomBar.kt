package com.smartjirani.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartjirani.core.designsystem.theme.SmartSpacing

/** The top-level destinations of the SmartJirani shell. */
enum class AppTab { FEED, UJAMAA, BARAZA, CIVIC, MESSAGES, PROFILE }

/**
 * Bottom navigation matching the design: 80dp bar, rounded icon well that fills with
 * `primary/10` when active, label flips to `primary-container` + bold.
 *
 * @param ujamaaBadge optional count shown on the Ujamaa Hub tab; null = no badge.
 * @param civicVisible when true the Serikali (civic) tab is shown between Baraza
 *   and Messages; it is role-gated to civic officials.
 */
@Composable
fun AppBottomBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    ujamaaBadge: String? = null,
    civicVisible: Boolean = false,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(8.dp)
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(80.dp)
                .padding(horizontal = SmartSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        AppBottomItem(
            tab = AppTab.FEED,
            label = "Mtaa Feed",
            icon = Icons.Filled.Newspaper,
            badge = null,
            currentTab = currentTab,
            onTabSelected = onTabSelected,
        )
        AppBottomItem(
            tab = AppTab.UJAMAA,
            label = "Ujamaa Hub",
            icon = Icons.Filled.Diversity1,
            badge = ujamaaBadge,
            currentTab = currentTab,
            onTabSelected = onTabSelected,
        )
        AppBottomItem(
            tab = AppTab.BARAZA,
            label = "Baraza",
            icon = Icons.Filled.Forum,
            badge = null,
            currentTab = currentTab,
            onTabSelected = onTabSelected,
        )
        if (civicVisible) {
            AppBottomItem(
                tab = AppTab.CIVIC,
                label = "Serikali",
                icon = Icons.Filled.AccountBalance,
                badge = null,
                currentTab = currentTab,
                onTabSelected = onTabSelected,
            )
        }
        AppBottomItem(
            tab = AppTab.MESSAGES,
            label = "Messages",
            icon = Icons.Filled.Message,
            badge = null,
            currentTab = currentTab,
            onTabSelected = onTabSelected,
        )
        AppBottomItem(
            tab = AppTab.PROFILE,
            label = "Profile",
            icon = Icons.Filled.VerifiedUser,
            badge = null,
            currentTab = currentTab,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
private fun AppBottomItem(
    tab: AppTab,
    label: String,
    icon: ImageVector,
    badge: String?,
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
) {
    val selected = tab == currentTab
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .clip(RoundedCornerShape(SmartSpacing.sm))
                .clickable { onTabSelected(tab) }
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                ).padding(horizontal = SmartSpacing.sm, vertical = SmartSpacing.xs),
    ) {
        Box {
            Box(
                modifier =
                    Modifier
                        .width(40.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(SmartSpacing.sm)),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(22.dp),
                    tint = contentColor,
                )
            }
            if (badge != null) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(start = 20.dp)
                            .clip(RoundedCornerShape(SmartSpacing.xs))
                            .background(MaterialTheme.colorScheme.secondary)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondary,
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
