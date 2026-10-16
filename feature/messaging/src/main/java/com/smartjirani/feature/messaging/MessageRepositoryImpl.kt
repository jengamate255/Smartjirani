package com.smartjirani.feature.messaging

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.database.dao.ConversationDao
import com.smartjirani.core.database.dao.MessageDao
import com.smartjirani.core.database.entity.ConversationEntity
import com.smartjirani.core.database.entity.MessageEntity
import com.smartjirani.core.network.SupabaseClientProvider
import com.smartjirani.feature.messaging.domain.Conversation
import com.smartjirani.feature.messaging.domain.Message
import com.smartjirani.feature.messaging.domain.MessageType
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
        private val conversationDao: ConversationDao,
        private val messageDao: MessageDao,
    ) : MessageRepository {
        override fun observeConversations(userId: String): Flow<List<Conversation>> =
            conversationDao.getConversationsForUser(userId).map { entities ->
                entities.map { it.toDomain() }
            }

        override fun observeMessages(conversationId: String): Flow<List<Message>> =
            messageDao.getMessages(conversationId).map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun syncConversations(userId: String): Resource<List<Conversation>> =
            safeCall {
                val result =
                    supabaseClient.postgrest
                        .from("conversations")
                        .select {
                            order("last_message_at", Order.DESCENDING)
                        }.decodeList<ConversationDto>()

                val entities = result.map { it.toEntity() }
                conversationDao.insertConversations(entities)
                entities.map { it.toDomain() }
            }

        override suspend fun syncMessages(conversationId: String): Resource<List<Message>> =
            safeCall {
                val result =
                    supabaseClient.postgrest
                        .from("messages")
                        .select {
                            filter { eq("conversation_id", conversationId) }
                            order("created_at", Order.ASCENDING)
                            limit(100)
                        }.decodeList<MessageDto>()

                val entities = result.map { it.toEntity() }
                messageDao.insertMessages(entities)
                entities.map { it.toDomain() }
            }

        override suspend fun sendMessage(
            conversationId: String,
            senderId: String,
            content: String,
        ): Resource<Message> =
            safeCall {
                val senderProfile =
                    supabaseClient.postgrest
                        .from("profiles")
                        .select {
                            filter { eq("id", senderId) }
                        }.decodeSingle<ProfileDto>()

                val messageDto =
                    supabaseClient.postgrest
                        .from("messages")
                        .insert(
                            MessageInsertDto(
                                conversationId = conversationId,
                                senderId = senderId,
                                senderName = senderProfile.displayName,
                                content = content,
                            ),
                        ) {
                            select()
                        }.decodeSingle<MessageDto>()

                val entity = messageDto.toEntity()
                messageDao.insertMessage(entity)
                conversationDao.updateLastMessage(
                    conversationId = conversationId,
                    content = content,
                    senderId = senderId,
                    timestamp = parseTimestamp(messageDto.createdAt),
                )
                entity.toDomain()
            }

        override suspend fun getOrCreateDirectConversation(
            userA: String,
            userB: String,
        ): Resource<Conversation> =
            safeCall {
                val result =
                    supabaseClient.postgrest
                        .rpc(
                            function = "get_or_create_direct_conversation",
                            parameters = mapOf("p_user_a" to userA, "p_user_b" to userB),
                        ).decodeSingle<ConversationDto>()

                val entity = result.toEntity()
                conversationDao.insertConversation(entity)
                entity.toDomain()
            }

        override suspend fun markAsRead(conversationId: String) {
            conversationDao.clearUnreadCount(conversationId)
        }

        // --- DTOs ---

        @Serializable
        data class ConversationDto(
            val id: String,
            val title: String? = null,
            @SerialName("is_group") val isGroup: Boolean = false,
            @SerialName("created_by") val createdBy: String,
            @SerialName("last_message_at") val lastMessageAt: String? = null,
            @SerialName("last_message_content") val lastMessageContent: String? = null,
            @SerialName("last_message_sender_id") val lastMessageSenderId: String? = null,
            @SerialName("participant_ids") val participantIds: List<String> = emptyList(),
            @SerialName("participant_names") val participantNames: List<String> = emptyList(),
            @SerialName("created_at") val createdAt: String? = null,
        ) {
            fun toEntity(): ConversationEntity =
                ConversationEntity(
                    id = id,
                    title = title,
                    isGroup = isGroup,
                    createdBy = createdBy,
                    lastMessageAt = parseTimestamp(lastMessageAt),
                    lastMessageContent = lastMessageContent,
                    lastMessageSenderId = lastMessageSenderId,
                    participantIds = participantIds.joinToString(","),
                    participantNames = participantNames.joinToString(","),
                    createdAt = parseTimestamp(createdAt),
                )
        }

        @Serializable
        data class MessageDto(
            val id: String,
            @SerialName("conversation_id") val conversationId: String,
            @SerialName("sender_id") val senderId: String,
            @SerialName("sender_name") val senderName: String = "",
            val content: String,
            @SerialName("message_type") val messageType: String = "TEXT",
            @SerialName("media_url") val mediaUrl: String? = null,
            @SerialName("reply_to_id") val replyToId: String? = null,
            @SerialName("is_edited") val isEdited: Boolean = false,
            @SerialName("created_at") val createdAt: String,
        ) {
            fun toEntity(): MessageEntity =
                MessageEntity(
                    id = id,
                    conversationId = conversationId,
                    senderId = senderId,
                    senderName = senderName,
                    content = content,
                    messageType = messageType,
                    mediaUrl = mediaUrl,
                    replyToId = replyToId,
                    isEdited = isEdited,
                    createdAt = parseTimestamp(createdAt),
                )
        }

        @Serializable
        data class MessageInsertDto(
            @SerialName("conversation_id") val conversationId: String,
            @SerialName("sender_id") val senderId: String,
            @SerialName("sender_name") val senderName: String,
            val content: String,
        )

        @Serializable
        data class ProfileDto(
            val id: String,
            @SerialName("display_name") val displayName: String = "",
        )

        // --- Mappers ---

        private fun ConversationEntity.toDomain(): Conversation =
            Conversation(
                id = id,
                title = title,
                isGroup = isGroup,
                createdBy = createdBy,
                lastMessageAt = lastMessageAt,
                lastMessageContent = lastMessageContent,
                lastMessageSenderId = lastMessageSenderId,
                unreadCount = unreadCount,
                participantIds = participantIds.split(",").filter { it.isNotBlank() },
                participantNames = participantNames.split(",").filter { it.isNotBlank() },
                createdAt = createdAt,
            )

        private fun MessageEntity.toDomain(): Message =
            Message(
                id = id,
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                content = content,
                messageType =
                    try {
                        MessageType.valueOf(messageType)
                    } catch (_: Exception) {
                        MessageType.TEXT
                    },
                mediaUrl = mediaUrl,
                replyToId = replyToId,
                isEdited = isEdited,
                createdAt = createdAt,
            )

        companion object {
            private fun parseTimestamp(iso: String?): Long {
                if (iso.isNullOrBlank()) return System.currentTimeMillis()
                return try {
                    java.time.OffsetDateTime
                        .parse(iso)
                        .toInstant()
                        .toEpochMilli()
                } catch (_: Exception) {
                    try {
                        java.time.LocalDateTime
                            .parse(iso)
                            .toInstant(java.time.ZoneOffset.UTC)
                            .toEpochMilli()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }
                }
            }
        }
    }
