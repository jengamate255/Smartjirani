package com.smartjirani.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String? = null,
    val isGroup: Boolean = false,
    val createdBy: String,
    val lastMessageAt: Long,
    val lastMessageContent: String? = null,
    val lastMessageSenderId: String? = null,
    val unreadCount: Int = 0,
    val participantIds: String = "",
    val participantNames: String = "",
    val createdAt: Long,
)
