package com.smartjirani.feature.messaging

import com.smartjirani.core.common.result.Resource
import com.smartjirani.feature.messaging.domain.Conversation
import com.smartjirani.feature.messaging.domain.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeConversations(userId: String): Flow<List<Conversation>>

    fun observeMessages(conversationId: String): Flow<List<Message>>

    suspend fun syncConversations(userId: String): Resource<List<Conversation>>

    suspend fun syncMessages(conversationId: String): Resource<List<Message>>

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        content: String,
    ): Resource<Message>

    suspend fun getOrCreateDirectConversation(
        userA: String,
        userB: String,
    ): Resource<Conversation>

    suspend fun markAsRead(conversationId: String)
}
