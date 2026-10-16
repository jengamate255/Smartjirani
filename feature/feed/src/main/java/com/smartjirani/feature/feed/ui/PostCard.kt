package com.smartjirani.feature.feed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.smartjirani.core.designsystem.component.UserAvatar
import com.smartjirani.core.designsystem.theme.SmartSpacing
import com.smartjirani.core.designsystem.theme.UjamaaAmber
import com.smartjirani.core.designsystem.theme.UjamaaAmberContainer
import com.smartjirani.core.designsystem.theme.UjamaaAmberDeep
import com.smartjirani.core.designsystem.theme.UjamaaEmerald
import com.smartjirani.core.designsystem.theme.UjamaaError
import com.smartjirani.core.designsystem.theme.UjamaaSurfaceTint
import com.smartjirani.core.designsystem.theme.UjamaaTerracotta
import com.smartjirani.feature.posts.Post
import com.smartjirani.feature.posts.PostType
import com.smartjirani.feature.posts.label
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Localised label for a post type, using Tanzania-friendly code-switched copy. */
private fun PostType.accentColor(): Color =
    when (this) {
        PostType.GENERAL -> UjamaaEmerald
        PostType.SERVICE -> UjamaaSurfaceTint
        PostType.ANNOUNCEMENT -> UjamaaAmberContainer
        PostType.EVENT -> UjamaaAmberDeep
        PostType.ALERT -> UjamaaError
        PostType.LOST_FOUND -> UjamaaAmber
        PostType.BUY_SELL -> UjamaaTerracotta
    }

/**
 * A neighbourhood post card.
 *
 * [onClick] is optional so the card is only tappable where tapping actually navigates
 * (the feed); on the detail screen there is nowhere to go, so it renders as inert content
 * rather than a dead button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCard(
    post: Post,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            colors = colors,
            elevation = elevation,
        ) {
            PostCardContent(post = post, onToggleLike = onToggleLike, onOpenPost = onClick)
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = colors,
            elevation = elevation,
        ) {
            PostCardContent(post = post, onToggleLike = onToggleLike, onOpenPost = onClick)
        }
    }
}

@Composable
private fun PostCardContent(
    post: Post,
    onToggleLike: () -> Unit,
    onOpenPost: (() -> Unit)?,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                avatarUrl = post.authorAvatarUrl,
                contentDescription = post.authorName,
                size = 44,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.authorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = relativeTime(post.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PostTypeBadge(postType = post.postType)
            if (post.isReported) {
                Spacer(Modifier.width(6.dp))
                ReportedBadge()
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = post.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (!post.imageUrl.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(post.imageUrl)
                        .crossfade(true)
                        .build(),
                contentDescription = "Picha ya posti",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EngagementButton(
                icon = if (post.isLikedByUser) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = post.likesCount.toString(),
                contentDescription = if (post.isLikedByUser) "Ondoa upendeleo" else "Penda",
                tint =
                    if (post.isLikedByUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                onClick = onToggleLike,
            )
            EngagementButton(
                icon = Icons.Filled.List,
                label = post.commentsCount.toString(),
                contentDescription = "Maoni",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onOpenPost,
            )
            Spacer(Modifier.weight(1f))
            EngagementButton(
                icon = Icons.Filled.Share,
                label = "Shiriki",
                contentDescription = "Shiriki posti",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onOpenPost,
            )
        }
    }
}

@Composable
private fun ReportedBadge() {
    Surface(
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = "Imeripotiwa",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = SmartSpacing.sm, vertical = SmartSpacing.xs),
        )
    }
}

@Composable
private fun PostTypeBadge(postType: PostType) {
    val accent = postType.accentColor()
    Surface(
        color = accent.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = postType.label(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            modifier = Modifier.padding(horizontal = SmartSpacing.sm, vertical = SmartSpacing.xs),
        )
    }
}

@Composable
private fun EngagementButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    tint: Color,
    onClick: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}

/** Compact "time ago" label shared by the feed card and the post detail screen. */
internal fun relativeTime(timestamp: Long): String {
    val elapsed = System.currentTimeMillis() - timestamp
    val minutes = elapsed / 60_000L
    val hours = elapsed / 3_600_000L
    val days = elapsed / 86_400_000L
    return when {
        minutes < 1L -> "Sasa"
        minutes < 60L -> "${minutes}m"
        hours < 24L -> "${hours}h"
        days < 7L -> "${days}d"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
    }
}
