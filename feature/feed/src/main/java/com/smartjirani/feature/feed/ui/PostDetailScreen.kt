package com.smartjirani.feature.feed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.SmartEmptyState
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.core.designsystem.component.UserAvatar
import com.smartjirani.feature.comments.Comment
import com.smartjirani.feature.comments.ui.CommentsViewModel
import com.smartjirani.feature.posts.Post
import com.smartjirani.feature.posts.ui.PostsViewModel

private const val MAX_COMMENT_LENGTH = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailRoute(
    postId: String,
    onNavigateBack: () -> Unit,
    postsViewModel: PostsViewModel = hiltViewModel(),
    commentsViewModel: CommentsViewModel = hiltViewModel(),
) {
    val postState by postsViewModel.uiState.collectAsState()
    val commentState by commentsViewModel.uiState.collectAsState()

    LaunchedEffect(postId) { postsViewModel.loadPost(postId) }
    LaunchedEffect(postId) { commentsViewModel.start(postId) }

    PostDetailScreen(
        post = postState.selectedPost,
        isPostLoading = postState.isPostLoading,
        comments = commentState.comments,
        isCommentsLoading = commentState.isLoading,
        isSending = commentState.isSending,
        error = commentState.error ?: postState.error,
        onToggleLike = postsViewModel::toggleLike,
        onRefresh = { commentsViewModel.refresh(postId) },
        onSendComment = commentsViewModel::addComment,
        onDismissError = {
            commentsViewModel.dismissError()
            postsViewModel.dismissError()
        },
        onFlagPost = postsViewModel::flagPost,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    post: Post?,
    isPostLoading: Boolean,
    comments: List<Comment>,
    isCommentsLoading: Boolean,
    isSending: Boolean,
    error: String?,
    onToggleLike: (String) -> Unit,
    onRefresh: () -> Unit,
    onSendComment: (String) -> Unit,
    onDismissError: () -> Unit,
    onFlagPost: (String, String?) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    var showFlagDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Posti") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Rudi",
                        )
                    }
                },
                actions = {
                    if (post != null) {
                        IconButton(onClick = { showFlagDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = "Ripoti posti",
                            )
                        }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Onyesha upya",
                        )
                    }
                },
            )
        },
        bottomBar = {
            CommentComposer(
                draft = draft,
                onDraftChange = { if (it.length <= MAX_COMMENT_LENGTH) draft = it },
                isSending = isSending,
                onSend = {
                    onSendComment(draft)
                    draft = ""
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            error?.let { ErrorBanner(message = it, onDismiss = onDismissError) }

            when {
                isPostLoading && post == null -> {
                    SmartLoading()
                }

                post == null -> {
                    SmartEmptyState(
                        title = "Posti haipatikani",
                        message = "Posti hii haipo au umeondolewa. Rudi kwenye mtaa wako.",
                        modifier = Modifier.weight(1f),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(key = "post-${post.id}") {
                            PostCard(
                                post = post,
                                onToggleLike = { onToggleLike(post.id) },
                            )
                        }

                        item(key = "comments-header") {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text =
                                    if (comments.isEmpty()) {
                                        "Maoni"
                                    } else {
                                        "Maoni (${comments.size})"
                                    },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }

                        if (isCommentsLoading && comments.isEmpty()) {
                            item(key = "comments-loading") {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Inapakia maoni…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else if (comments.isEmpty()) {
                            item(key = "comments-empty") {
                                Text(
                                    text = "Bado hakuna maoni. Kuwa mwanzo wa mazungumzo!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        } else {
                            items(comments, key = { it.id }) { comment ->
                                CommentRow(comment = comment)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFlagDialog && post != null) {
        FlagDialog(
            onFlag = { reason ->
                showFlagDialog = false
                onFlagPost(post.id, reason)
            },
            onDismiss = { showFlagDialog = false },
        )
    }
}

@Composable
private fun FlagDialog(
    onFlag: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ripoti posti") },
        text = {
            Column {
                Text(
                    text =
                        "Posti hii ina maudhui yasiyofaa? Ripoti yako huungana na za majirani; " +
                            "posti zilizoripotiwa na wenzaji 3 au zaidi huonyesha alama ya \"Imeripotiwa\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 500) reason = it },
                    label = { Text("Sababu (hiari)") },
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onFlag(reason.takeIf { it.isNotBlank() })
                onDismiss()
            }) {
                Text("Ripoti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ghairi") }
        },
    )
}

@Composable
private fun CommentRow(comment: Comment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        UserAvatar(
            avatarUrl = comment.authorAvatarUrl,
            contentDescription = comment.authorName,
            size = 36,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = relativeTime(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun CommentComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    isSending: Boolean,
    onSend: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Andika jibu lako…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = draft.isNotBlank() && !isSending,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Tuma",
                    tint =
                        if (draft.isNotBlank() && !isSending) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Funga ujumbe",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
