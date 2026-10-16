package com.smartjirani.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.smartjirani.core.database.entity.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE neighborhoodId = :neighborhoodId ORDER BY createdAt DESC")
    fun getPostsByNeighborhood(neighborhoodId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :postId")
    fun getPostById(postId: String): Flow<PostEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Transaction
    suspend fun replacePosts(
        neighborhoodId: String,
        posts: List<PostEntity>,
    ) {
        deletePostsByNeighborhood(neighborhoodId)
        if (posts.isNotEmpty()) insertPosts(posts)
    }

    @Query(
        """
        UPDATE posts
        SET isLikedByUser = :isLiked,
            likesCount = MAX(0, likesCount + :countDelta)
        WHERE id = :postId AND isLikedByUser != :isLiked
        """,
    )
    suspend fun updateLikeState(
        postId: String,
        isLiked: Boolean,
        countDelta: Int,
    )

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("DELETE FROM posts WHERE neighborhoodId = :neighborhoodId")
    suspend fun deletePostsByNeighborhood(neighborhoodId: String)
}
