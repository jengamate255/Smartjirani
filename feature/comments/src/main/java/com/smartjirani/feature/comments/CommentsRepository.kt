package com.smartjirani.feature.comments

import com.smartjirani.core.common.result.Resource
import kotlinx.coroutines.flow.Flow

data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

interface CommentsRepository {
    /** Cached comments for a post; the offline-first source of truth for the UI. */
    fun observeComments(postId: String): Flow<List<Comment>>

    suspend fun getComments(postId: String): Resource<List<Comment>>

    suspend fun createComment(comment: Comment): Resource<Comment>

    suspend fun deleteComment(commentId: String): Resource<Unit>
}
