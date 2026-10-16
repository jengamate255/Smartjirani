package com.smartjirani.core.realtime

import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeServiceImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
    ) : RealtimeService {
        private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
        override val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

        private val _isConnected = MutableStateFlow(false)
        override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val scope = CoroutineScope(Dispatchers.Default + Job())
        private val subscriptions = mutableMapOf<String, List<Job>>()
        private val channels = mutableMapOf<String, RealtimeChannel>()

        private fun conversationKey(conversationId: String) = "conv:$conversationId"

        override suspend fun subscribeToNeighborhood(neighborhoodId: String) {
            if (channels.containsKey(neighborhoodId)) return
            try {
                val topic = "neighborhood:$neighborhoodId"
                val channel = supabaseClient.realtime.channel(topic)

                val jobs =
                    listOf(
                        listenForTable(channel, neighborhoodId, "baraza_events") {
                            RealtimeEvent.BarazaEventChanged(neighborhoodId)
                        },
                        listenForTable(channel, neighborhoodId, "forum_threads") {
                            RealtimeEvent.ForumThreadChanged(neighborhoodId)
                        },
                        listenForTable(channel, neighborhoodId, "harambee_projects") {
                            RealtimeEvent.HarambeeProjectChanged(neighborhoodId)
                        },
                        listenForTable(channel, neighborhoodId, "urgent_needs") {
                            RealtimeEvent.UrgentNeedChanged(neighborhoodId)
                        },
                        listenForTable(channel, neighborhoodId, "resources") {
                            RealtimeEvent.SharedResourceChanged(neighborhoodId)
                        },
                        listenForTable(channel, neighborhoodId, "harambee_funds") {
                            RealtimeEvent.HarambeeFundChanged(neighborhoodId)
                        },
                        listenForTable(channel, neighborhoodId, "posts") { action ->
                            val id = action.extractId() ?: return@listenForTable null
                            RealtimeEvent.NewPost(postId = id, neighborhoodId = neighborhoodId)
                        },
                        listenForTable(channel, neighborhoodId, "comments") { action ->
                            val postId = action.extractColumn("post_id") ?: return@listenForTable null
                            val commentId = action.extractId() ?: return@listenForTable null
                            RealtimeEvent.NewComment(commentId = commentId, postId = postId)
                        },
                    )

                channel.subscribe()
                channels[neighborhoodId] = channel
                subscriptions[neighborhoodId] = jobs
                _isConnected.value = true
            } catch (_: Exception) {
                _isConnected.value = false
            }
        }

        override suspend fun unsubscribeFromNeighborhood(neighborhoodId: String) {
            subscriptions.remove(neighborhoodId)?.forEach { it.cancel() }
            channels.remove(neighborhoodId)?.let { supabaseClient.realtime.removeChannel(it) }
            if (channels.isEmpty()) _isConnected.value = false
        }

        override suspend fun subscribeToConversation(conversationId: String) {
            val key = conversationKey(conversationId)
            if (channels.containsKey(key)) return
            try {
                val topic = "conversation:$conversationId"
                val channel = supabaseClient.realtime.channel(topic)

                val jobs =
                    listOf(
                        scope.launch {
                            channel
                                .postgresChangeFlow<PostgresAction>(schema = "public") {
                                    this.table = "messages"
                                    filter("conversation_id", FilterOperator.EQ, conversationId)
                                }.collect { action ->
                                    val messageId = action.extractId() ?: return@collect
                                    val senderId = action.extractColumn("sender_id") ?: return@collect
                                    _events.emit(
                                        RealtimeEvent.NewMessage(
                                            messageId = messageId,
                                            conversationId = conversationId,
                                            senderId = senderId,
                                        ),
                                    )
                                }
                        },
                    )

                channel.subscribe()
                channels[key] = channel
                subscriptions[key] = jobs
            } catch (_: Exception) {
            }
        }

        override suspend fun unsubscribeFromConversation(conversationId: String) {
            val key = conversationKey(conversationId)
            subscriptions.remove(key)?.forEach { it.cancel() }
            channels.remove(key)?.let { supabaseClient.realtime.removeChannel(it) }
        }

        override suspend fun connect() {
            _isConnected.value = true
        }

        override suspend fun disconnect() {
            subscriptions.values.flatten().forEach { it.cancel() }
            subscriptions.clear()
            channels.values.forEach { supabaseClient.realtime.removeChannel(it) }
            channels.clear()
            _isConnected.value = false
        }

        private fun listenForTable(
            channel: RealtimeChannel,
            neighborhoodId: String,
            table: String,
            mapper: (PostgresAction) -> RealtimeEvent?,
        ): Job =
            scope.launch {
                channel
                    .postgresChangeFlow<PostgresAction>(schema = "public") {
                        this.table = table
                        filter("neighborhood_id", FilterOperator.EQ, neighborhoodId)
                    }.collect { action ->
                        mapper(action)?.let { _events.emit(it) }
                    }
            }

        private fun PostgresAction.extractId(): String? = extractColumn("id")

        private fun PostgresAction.extractColumn(name: String): String? {
            val record: JsonObject? =
                when (this) {
                    is PostgresAction.Insert -> record
                    is PostgresAction.Update -> record
                    is PostgresAction.Delete -> oldRecord
                    else -> null
                }
            return (record?.get(name) as? JsonPrimitive)?.contentOrNull
        }
    }
