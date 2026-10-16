package com.smartjirani.feature.posts

import com.smartjirani.core.common.result.Resource
import kotlinx.coroutines.flow.Flow

enum class PostType {
    GENERAL,
    ANNOUNCEMENT,
    ALERT,
    LOST_FOUND,
    BUY_SELL,
    SERVICE,
    EVENT,
    ;

    companion object {
        fun fromString(value: String): PostType = entries.find { it.name.equals(value, ignoreCase = true) } ?: GENERAL
    }
}

data class Post(
    val id: String,
    val authorId: String,
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val neighborhoodId: String,
    val content: String,
    val imageUrl: String? = null,
    val postType: PostType = PostType.GENERAL,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByUser: Boolean = false,
    val isReported: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

interface PostsRepository {
    fun observePosts(neighborhoodId: String): Flow<List<Post>>

    suspend fun getPosts(neighborhoodId: String): Resource<List<Post>>

    suspend fun getPostById(postId: String): Resource<Post>

    suspend fun createPost(post: Post): Resource<Post>

    suspend fun deletePost(postId: String): Resource<Unit>

    suspend fun likePost(postId: String): Resource<Unit>

    suspend fun unlikePost(postId: String): Resource<Unit>

    suspend fun uploadImage(imageBytes: ByteArray): Resource<String>

    /** Flags a post as inappropriate. Server rate-limits and rejects your own posts. */
    suspend fun flagPost(
        postId: String,
        reason: String? = null,
    ): Resource<Unit>

    /** IDs the signed-in user has already flagged, so the feed can show "reported" state. */
    suspend fun getReportedPostIds(): Resource<Set<String>>
}
