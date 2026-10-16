package com.smartjirani.feature.messaging.domain

data class Conversation(
    val id: String,
    val title: String? = null,
    val isGroup: Boolean = false,
    val createdBy: String,
    val lastMessageAt: Long,
    val lastMessageContent: String? = null,
    val lastMessageSenderId: String? = null,
    val unreadCount: Int = 0,
    val participantIds: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val createdAt: Long,
) {
    fun displayName(currentUserId: String): String {
        if (isGroup) return title ?: "Chat ya kikundi"
        val otherIndex = participantIds.indexOfFirst { it != currentUserId }
        return if (otherIndex >= 0 && otherIndex < participantNames.size) {
            participantNames[otherIndex]
        } else {
            title ?: "Haijulikani"
        }
    }
}
