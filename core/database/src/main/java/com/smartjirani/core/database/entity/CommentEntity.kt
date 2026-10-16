package com.smartjirani.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)
