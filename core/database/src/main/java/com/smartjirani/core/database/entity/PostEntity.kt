package com.smartjirani.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val neighborhoodId: String,
    val content: String,
    val imageUrl: String? = null,
    val postType: String = "GENERAL",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByUser: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
