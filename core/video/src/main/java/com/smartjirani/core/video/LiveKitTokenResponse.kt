package com.smartjirani.core.video

import com.smartjirani.core.network.SupabaseClientProvider
import io.ktor.client.call.body
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class LiveKitTokenResponse(
    val token: String,
    val url: String,
    val roomName: String,
)

internal suspend fun fetchLiveKitToken(
    supabaseClientProvider: SupabaseClientProvider,
    roomName: String,
    participantName: String,
    participantId: String,
): LiveKitTokenResponse {
    val response =
        supabaseClientProvider.functions.invoke(
            "generate-livekit-token",
            body =
                buildJsonObject {
                    put("roomName", roomName)
                    put("participantName", participantName)
                    put("participantId", participantId)
                },
        )
    val responseBody = response.body<String>()
    return Json.decodeFromString<LiveKitTokenResponse>(responseBody)
}
