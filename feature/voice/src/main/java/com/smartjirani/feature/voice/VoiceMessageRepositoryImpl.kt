package com.smartjirani.feature.voice

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class VoiceMessageDto(
    val id: String,
    val sender_id: String,
    val receiver_id: String? = null,
    val neighborhood_id: String? = null,
    val storage_path: String,
    val duration_ms: Long,
    val created_at: String,
)

@Singleton
class VoiceMessageRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
    ) : VoiceMessageRepository {
        override suspend fun sendVoiceMessage(message: VoiceMessage): Resource<VoiceMessage> =
            safeCall {
                val audioFile = java.io.File(message.storagePath)
                val storagePath = "voice_messages/${UUID.randomUUID()}.m4a"
                supabaseClient.storage.from("voice-messages").upload(storagePath, audioFile.readBytes())

                val dto =
                    VoiceMessageDto(
                        id = UUID.randomUUID().toString(),
                        sender_id = message.senderId,
                        receiver_id = message.receiverId,
                        neighborhood_id = message.neighborhoodId,
                        storage_path = storagePath,
                        duration_ms = message.durationMs,
                        created_at =
                            java.time.Instant
                                .now()
                                .toString(),
                    )
                supabaseClient.postgrest.from("voice_messages").insert(dto)
                message.copy(id = dto.id)
            }

        override suspend fun getVoiceMessages(userId: String): Resource<List<VoiceMessage>> =
            safeCall {
                val dtos =
                    supabaseClient.postgrest
                        .from("voice_messages")
                        .select { filter { eq("sender_id", userId) } }
                        .decodeList<VoiceMessageDto>()
                dtos.map { it.toDomain() }
            }

        override suspend fun deleteVoiceMessage(messageId: String): Resource<Unit> =
            safeCall {
                supabaseClient.postgrest.from("voice_messages").delete { filter { eq("id", messageId) } }
            }

        private fun VoiceMessageDto.toDomain() =
            VoiceMessage(
                id = id,
                senderId = sender_id,
                receiverId = receiver_id,
                neighborhoodId = neighborhood_id,
                storagePath = storage_path,
                durationMs = duration_ms,
                createdAt = System.currentTimeMillis(),
            )
    }
