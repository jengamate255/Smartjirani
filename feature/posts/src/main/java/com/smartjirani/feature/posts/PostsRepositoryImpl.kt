package com.smartjirani.feature.posts

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.database.dao.PostDao
import com.smartjirani.core.database.entity.PostEntity
import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PostDto(
    val id: String,
    val author_id: String,
    val content: String,
    val image_url: String? = null,
    val post_type: String = "GENERAL",
    val neighborhood_id: String,
    val created_at: String,
    val updated_at: String,
)

@Serializable
private data class FeedPostDto(
    val id: String,
    val author_id: String,
    val content: String,
    val image_url: String? = null,
    val post_type: String = "GENERAL",
    val neighborhood_id: String,
    val created_at: String,
    val updated_at: String,
    val profiles: PostAuthorDto? = null,
    val post_comments: List<PostCountDto> = emptyList(),
    val post_likes: List<PostCountDto> = emptyList(),
)

@Serializable
private data class PostAuthorDto(
    val display_name: String = "",
    val avatar_url: String? = null,
)

@Serializable
private data class PostCountDto(
    val count: Long = 0,
)

@Serializable
private data class PostUserLikeDto(
    val post_id: String,
)

@Serializable
private data class ReportedPostDto(
    val post_id: String,
)

@Singleton
class PostsRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
        private val postDao: PostDao,
    ) : PostsRepository {
        override fun observePosts(neighborhoodId: String): Flow<List<Post>> =
            postDao.getPostsByNeighborhood(neighborhoodId).map { posts ->
                posts.map { entity -> entity.toDomain() }
            }

        override suspend fun getPosts(neighborhoodId: String): Resource<List<Post>> =
            safeCall {
                val currentUserId =
                    supabaseClient.auth
                        .currentSessionOrNull()
                        ?.user
                        ?.id
                val feedPosts = fetchFeedPosts(neighborhoodId)
                val likedIds = likedPostIds(feedPosts.map { it.id }, currentUserId)
                val posts = feedPosts.map { it.toDomain(likedIds) }.distinctBy { it.id }
                postDao.replacePosts(neighborhoodId, posts.map { it.toEntity() })
                posts
            }

        override suspend fun getPostById(postId: String): Resource<Post> =
            safeCall {
                val currentUserId =
                    supabaseClient.auth
                        .currentSessionOrNull()
                        ?.user
                        ?.id
                val post = fetchPost(postId).toDomain(likedPostIds(listOf(postId), currentUserId))
                post.copy(isReported = fetchReportedPostIds().contains(postId))
            }

        override suspend fun createPost(post: Post): Resource<Post> =
            safeCall {
                val authorId =
                    supabaseClient.auth
                        .currentSessionOrNull()
                        ?.user
                        ?.id
                        ?: error("You must be signed in to publish a post")
                val now = Instant.now().toString()
                val dto =
                    PostDto(
                        id = UUID.randomUUID().toString(),
                        author_id = authorId,
                        content = post.content,
                        image_url = post.imageUrl,
                        post_type = post.postType.name,
                        neighborhood_id = post.neighborhoodId,
                        created_at = now,
                        updated_at = now,
                    )
                supabaseClient.postgrest.from("posts").insert(dto)
                post.copy(id = dto.id, authorId = authorId)
            }

        override suspend fun deletePost(postId: String): Resource<Unit> =
            safeCall {
                supabaseClient.postgrest.from("posts").delete { filter { eq("id", postId) } }
                postDao.deletePost(postId)
            }

        override suspend fun likePost(postId: String): Resource<Unit> =
            safeCall {
                supabaseClient.postgrest.rpc("like_post", mapOf("post_id_param" to postId))
                postDao.updateLikeState(postId, isLiked = true, countDelta = 1)
            }

        override suspend fun unlikePost(postId: String): Resource<Unit> =
            safeCall {
                supabaseClient.postgrest.rpc("unlike_post", mapOf("post_id_param" to postId))
                postDao.updateLikeState(postId, isLiked = false, countDelta = -1)
            }

        override suspend fun uploadImage(imageBytes: ByteArray): Resource<String> =
            safeCall {
                val path = "posts/${UUID.randomUUID()}.jpg"
                supabaseClient.storage.from("media").upload(path, imageBytes)
                supabaseClient.storage.from("media").publicUrl(path)
            }

        override suspend fun flagPost(
            postId: String,
            reason: String?,
        ): Resource<Unit> =
            safeCall {
                val params =
                    buildMap {
                        put("p_post_id", postId)
                        if (!reason.isNullOrBlank()) put("p_reason", reason)
                    }
                val json = supabaseClient.postgrest.rpc("flag_post", params).decodeAs<JsonObject>()
                if (json["ok"]?.jsonPrimitive?.booleanOrNull != true) {
                    error(json["error"]?.jsonPrimitive?.content ?: "flag_failed")
                }
                Unit
            }

        override suspend fun getReportedPostIds(): Resource<Set<String>> =
            safeCall {
                fetchReportedPostIds()
            }

        private suspend fun fetchFeedPosts(neighborhoodId: String): List<FeedPostDto> =
            supabaseClient.postgrest
                .from("posts")
                .select(Columns.raw(POST_SELECT)) {
                    filter { eq("neighborhood_id", neighborhoodId) }
                    order("created_at", Order.DESCENDING)
                }.decodeList<FeedPostDto>()

        private suspend fun fetchPost(postId: String): FeedPostDto =
            supabaseClient.postgrest
                .from("posts")
                .select(Columns.raw(POST_SELECT)) {
                    filter { eq("id", postId) }
                    limit(1)
                }.decodeList<FeedPostDto>()
                .firstOrNull()
                ?: error("Post not found")

        private suspend fun likedPostIds(
            postIds: List<String>,
            userId: String?,
        ): Set<String> {
            if (userId == null || postIds.isEmpty()) return emptySet()
            return supabaseClient.postgrest
                .from("post_likes")
                .select(Columns.raw("post_id")) {
                    filter {
                        isIn("post_id", postIds)
                        eq("user_id", userId)
                    }
                }.decodeList<PostUserLikeDto>()
                .mapTo(mutableSetOf(), PostUserLikeDto::post_id)
        }

        /** Posts this user flagged themselves plus posts the community flagged (3+ distinct flaggers). */
        private suspend fun fetchReportedPostIds(): Set<String> {
            val own =
                supabaseClient.postgrest
                    .from("post_flags")
                    .select(Columns.raw("post_id"))
                    .decodeList<ReportedPostDto>()
                    .mapTo(mutableSetOf(), ReportedPostDto::post_id)
            val community =
                supabaseClient.postgrest
                    .rpc("civic_reported_posts")
                    .decodeList<ReportedPostDto>()
                    .mapTo(mutableSetOf(), ReportedPostDto::post_id)
            return own + community
        }

        private fun FeedPostDto.toDomain(likedPostIds: Set<String>) =
            Post(
                id = id,
                authorId = author_id,
                authorName = profiles?.display_name.orEmpty().ifBlank { "Neighbour" },
                authorAvatarUrl = profiles?.avatar_url,
                neighborhoodId = neighborhood_id,
                content = content,
                imageUrl = image_url,
                postType = PostType.fromString(post_type),
                likesCount = post_likes.firstOrNull()?.count.toCount(),
                commentsCount = post_comments.firstOrNull()?.count.toCount(),
                isLikedByUser = id in likedPostIds,
                createdAt = created_at.toEpochMillis(),
                updatedAt = updated_at.toEpochMillis(),
            )

        private fun Post.toEntity() =
            PostEntity(
                id = id,
                authorId = authorId,
                authorName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                neighborhoodId = neighborhoodId,
                content = content,
                imageUrl = imageUrl,
                postType = postType.name,
                likesCount = likesCount,
                commentsCount = commentsCount,
                isLikedByUser = isLikedByUser,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun PostEntity.toDomain() =
            Post(
                id = id,
                authorId = authorId,
                authorName = authorName.ifBlank { "Neighbour" },
                authorAvatarUrl = authorAvatarUrl,
                neighborhoodId = neighborhoodId,
                content = content,
                imageUrl = imageUrl,
                postType = PostType.fromString(postType),
                likesCount = likesCount.coerceAtLeast(0),
                commentsCount = commentsCount.coerceAtLeast(0),
                isLikedByUser = isLikedByUser,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun Long?.toCount(): Int = this?.coerceIn(0L, Int.MAX_VALUE.toLong())?.toInt() ?: 0

        private fun String.toEpochMillis(): Long =
            runCatching {
                Instant.parse(this).toEpochMilli()
            }.getOrDefault(System.currentTimeMillis())

        private companion object {
            const val POST_SELECT =
                "id, author_id, content, image_url, post_type, neighborhood_id, " +
                    "created_at, updated_at, " +
                    "profiles!posts_author_id_fkey(display_name, avatar_url), " +
                    "post_comments(count), post_likes(count)"
        }
    }
