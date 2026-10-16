package com.smartjirani.feature.comments

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.database.dao.CommentDao
import com.smartjirani.core.database.entity.CommentEntity
import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CommentDto(
    val id: String,
    val post_id: String,
    val author_id: String,
    val content: String,
    val created_at: String,
    val updated_at: String,
)

@Serializable
private data class CommentAuthorDto(
    val display_name: String = "",
    val avatar_url: String? = null,
)

/** A comment plus the author profile joined in one round-trip, so no per-row lookups. */
@Serializable
private data class CommentWithAuthorDto(
    val id: String,
    val post_id: String,
    val author_id: String,
    val content: String,
    val created_at: String,
    val updated_at: String,
    val profiles: CommentAuthorDto? = null,
)

@Singleton
class CommentsRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
        private val commentDao: CommentDao,
    ) : CommentsRepository {
        override fun observeComments(postId: String): Flow<List<Comment>> = commentDao.getCommentsByPost(postId).map { entities -> entities.map { it.toDomain() } }

        override suspend fun getComments(postId: String): Resource<List<Comment>> =
            safeCall {
                val dtos =
                    supabaseClient.postgrest
                        .from("post_comments")
                        .select(Columns.raw(COMMENT_SELECT)) {
                            filter { eq("post_id", postId) }
                            order("created_at", Order.ASCENDING)
                        }.decodeList<CommentWithAuthorDto>()

                val comments =
                    dtos
                        .map { it.toDomain() }
                        .distinctBy { it.id }
                        .sortedBy { it.createdAt }

                // Swap the cache so a comment deleted elsewhere disappears instead of lingering.
                commentDao.replaceComments(postId, comments.map { it.toEntity() })
                comments
            }

        override suspend fun createComment(comment: Comment): Resource<Comment> =
            safeCall {
                // The author is always the signed-in user; a caller-supplied id would be rejected
                // by the post_comments RLS policy and would allow impersonation.
                val authorId =
                    supabaseClient.auth
                        .currentSessionOrNull()
                        ?.user
                        ?.id
                        ?: error("You must be signed in to comment")
                val now = Instant.now()
                val dto =
                    CommentDto(
                        id = UUID.randomUUID().toString(),
                        post_id = comment.postId,
                        author_id = authorId,
                        content = comment.content,
                        created_at = now.toString(),
                        updated_at = now.toString(),
                    )
                supabaseClient.postgrest.from("post_comments").insert(dto)
                commentDao.insertComment(
                    CommentEntity(
                        id = dto.id,
                        postId = dto.post_id,
                        authorId = authorId,
                        authorName = comment.authorName.ifBlank { "Wewe" },
                        authorAvatarUrl = comment.authorAvatarUrl,
                        content = dto.content,
                        createdAt = now.toEpochMilli(),
                        updatedAt = now.toEpochMilli(),
                    ),
                )
                comment.copy(id = dto.id, authorId = authorId, createdAt = now.toEpochMilli(), updatedAt = now.toEpochMilli())
            }

        override suspend fun deleteComment(commentId: String): Resource<Unit> =
            safeCall {
                supabaseClient.postgrest.from("post_comments").delete { filter { eq("id", commentId) } }
                commentDao.deleteComment(commentId)
            }

        private fun CommentWithAuthorDto.toDomain() =
            Comment(
                id = id,
                postId = post_id,
                authorId = author_id,
                authorName = profiles?.display_name.orEmpty().ifBlank { "Jirani" },
                authorAvatarUrl = profiles?.avatar_url,
                content = content,
                createdAt = created_at.toEpochMillis(),
                updatedAt = updated_at.toEpochMillis(),
            )

        private fun Comment.toEntity() =
            CommentEntity(
                id = id,
                postId = postId,
                authorId = authorId,
                authorName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                content = content,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun CommentEntity.toDomain() =
            Comment(
                id = id,
                postId = postId,
                authorId = authorId,
                authorName = authorName.ifBlank { "Jirani" },
                authorAvatarUrl = authorAvatarUrl,
                content = content,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun String.toEpochMillis(): Long =
            runCatching {
                Instant.parse(this).toEpochMilli()
            }.getOrDefault(System.currentTimeMillis())

        private companion object {
            const val COMMENT_SELECT =
                "id, post_id, author_id, content, created_at, updated_at, " +
                    "profiles!post_comments_author_id_fkey(display_name, avatar_url)"
        }
    }
