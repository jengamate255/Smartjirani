package com.smartjirani.feature.voice

import com.smartjirani.core.common.result.Resource

data class VoiceMessage(
    val id: String,
    val senderId: String,
    val receiverId: String? = null,
    val neighborhoodId: String? = null,
    val storagePath: String,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

interface VoiceMessageRepository {
    suspend fun sendVoiceMessage(message: VoiceMessage): Resource<VoiceMessage>

    suspend fun getVoiceMessages(userId: String): Resource<List<VoiceMessage>>

    suspend fun deleteVoiceMessage(messageId: String): Resource<Unit>
}
