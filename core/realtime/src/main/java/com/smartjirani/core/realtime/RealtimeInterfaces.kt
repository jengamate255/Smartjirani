package com.smartjirani.core.realtime

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed class RealtimeEvent {
    data class NewPost(
        val postId: String,
        val neighborhoodId: String,
    ) : RealtimeEvent()

    data class NewComment(
        val commentId: String,
        val postId: String,
    ) : RealtimeEvent()

    data class NewLike(
        val postId: String,
        val userId: String,
    ) : RealtimeEvent()

    data class NewNotification(
        val notificationId: String,
    ) : RealtimeEvent()

    data class IncomingCall(
        val callId: String,
        val callerName: String,
    ) : RealtimeEvent()

    data class CallStatusChanged(
        val callId: String,
        val status: String,
    ) : RealtimeEvent()

    data class ConferenceStarted(
        val conferenceId: String,
    ) : RealtimeEvent()

    data class ConferenceEnded(
        val conferenceId: String,
    ) : RealtimeEvent()

    data class BarazaEventChanged(
        val neighborhoodId: String,
    ) : RealtimeEvent()

    data class ForumThreadChanged(
        val neighborhoodId: String,
    ) : RealtimeEvent()

    data class HarambeeProjectChanged(
        val neighborhoodId: String,
    ) : RealtimeEvent()

    data class UrgentNeedChanged(
        val neighborhoodId: String,
    ) : RealtimeEvent()

    data class SharedResourceChanged(
        val neighborhoodId: String,
    ) : RealtimeEvent()

    data class HarambeeFundChanged(
        val neighborhoodId: String,
    ) : RealtimeEvent()

    data class NewMessage(
        val messageId: String,
        val conversationId: String,
        val senderId: String,
    ) : RealtimeEvent()
}

interface RealtimeService {
    val events: SharedFlow<RealtimeEvent>
    val isConnected: StateFlow<Boolean>

    suspend fun subscribeToNeighborhood(neighborhoodId: String)

    suspend fun unsubscribeFromNeighborhood(neighborhoodId: String)

    suspend fun subscribeToConversation(conversationId: String)

    suspend fun unsubscribeFromConversation(conversationId: String)

    suspend fun connect()

    suspend fun disconnect()
}
