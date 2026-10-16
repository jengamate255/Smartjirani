package com.smartjirani.feature.messaging.domain

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String = "",
    val content: String,
    val messageType: MessageType = MessageType.TEXT,
    val mediaUrl: String? = null,
    val replyToId: String? = null,
    val isEdited: Boolean = false,
    val createdAt: Long,
)

enum class MessageType {
    TEXT,
    IMAGE,
    VOICE,
    SYSTEM,
}
