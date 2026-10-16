package com.smartjirani.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: String,
    val referenceId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long,
)
