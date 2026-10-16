package com.smartjirani.feature.baraza

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class EventDto(
    val id: String,
    val title: String,
    val mode: String = "HYBRID",
    val council_label: String = "",
    val committee_label: String = "",
    val district_label: String = "",
    val households_label: String = "",
    val quorum_label: String = "",
    val day_label: String = "",
    val date_label: String = "",
    val time_label: String = "",
    val venue: String = "",
    val stream_note: String = "",
    val agenda: List<String> = emptyList(),
    val attending_count: Int = 0,
    val questions_queued: Int = 0,
    val event_at: String? = null,
    val created_at: String,
)

@Serializable
private data class ThreadDto(
    val id: String,
    val author_id: String,
    val tag: String = "",
    val title: String,
    val body: String = "",
    val solved: Boolean = false,
    val created_at: String,
    val profiles: ThreadAuthorDto? = null,
    val thread_votes: List<CountDto> = emptyList(),
    val thread_replies: List<CountDto> = emptyList(),
)

@Serializable
private data class ThreadAuthorDto(
    val display_name: String = "",
)

@Serializable
private data class CountDto(
    val count: Long = 0,
)

@Serializable
private data class ProjectDto(
    val id: String,
    val status: String = "ACTIVE",
    val status_label: String = "",
    val phase_label: String = "",
    val category_label: String = "",
    val title: String,
    val subtitle: String = "",
    val icon_label: String = "",
    val photo_url: String? = null,
    val target_kes: Double = 0.0,
    val raised_kes: Double = 0.0,
    val progress_pct: Int = 0,
    val progress_label: String = "",
    val volunteer_slots: Int = 0,
    val volunteers_filled: Int = 0,
    val volunteer_note: String = "",
    val created_at: String,
)

@Serializable
private data class IdDto(
    val id: String,
)

@Singleton
class BarazaRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
    ) : BarazaRepository {
        override suspend fun getContent(neighborhoodId: String): Resource<BarazaContent> =
            safeCall {
                val userId =
                    supabaseClient.auth
                        .currentSessionOrNull()
                        ?.user
                        ?.id

                val events =
                    supabaseClient.postgrest
                        .from("baraza_events")
                        .select(Columns.raw(EVENT_COLUMNS)) {
                            filter { eq("neighborhood_id", neighborhoodId) }
                            order("event_at", Order.ASCENDING)
                        }.decodeList<EventDto>()

                val threads =
                    supabaseClient.postgrest
                        .from("forum_threads")
                        .select(Columns.raw(THREAD_COLUMNS)) {
                            filter { eq("neighborhood_id", neighborhoodId) }
                            order("created_at", Order.DESCENDING)
                            limit(20)
                        }.decodeList<ThreadDto>()

                val projects =
                    supabaseClient.postgrest
                        .from("harambee_projects")
                        .select(Columns.raw(PROJECT_COLUMNS)) {
                            filter { eq("neighborhood_id", neighborhoodId) }
                            order("created_at", Order.DESCENDING)
                        }.decodeList<ProjectDto>()

                val myRsvps =
                    if (userId == null) {
                        emptySet()
                    } else {
                        supabaseClient.postgrest
                            .from("baraza_rsvps")
                            .select(Columns.raw("event_id")) {
                                filter {
                                    eq("user_id", userId)
                                    isIn("event_id", events.map { it.id }.ifEmpty { listOf(EMPTY_UUID) })
                                }
                            }.decodeList<IdField>()
                            .mapTo(mutableSetOf()) { it.event_id }
                    }

                val myVotes =
                    if (userId == null) {
                        emptySet()
                    } else {
                        supabaseClient.postgrest
                            .from("thread_votes")
                            .select(Columns.raw("thread_id")) {
                                filter {
                                    eq("user_id", userId)
                                    isIn("thread_id", threads.map { it.id }.ifEmpty { listOf(EMPTY_UUID) })
                                }
                            }.decodeList<IdField2>()
                            .mapTo(mutableSetOf()) { it.thread_id }
                    }

                val myVolunteers =
                    if (userId == null) {
                        emptySet()
                    } else {
                        supabaseClient.postgrest
                            .from("project_volunteers")
                            .select(Columns.raw("project_id")) {
                                filter {
                                    eq("user_id", userId)
                                    isIn("project_id", projects.map { it.id }.ifEmpty { listOf(EMPTY_UUID) })
                                }
                            }.decodeList<IdField3>()
                            .mapTo(mutableSetOf()) { it.project_id }
                    }

                BarazaContent(
                    events = events.map { event -> event.toDomain(event.id in myRsvps) },
                    threads = threads.map { it.toDomain(it.id in myVotes) },
                    projects = projects.map { it.toDomain(it.id in myVolunteers) },
                )
            }

        override suspend fun toggleRsvp(
            eventId: String,
            attending: Boolean,
        ): Resource<Unit> =
            safeCall {
                val function = if (attending) "rsvp_baraza" else "cancel_rsvp_baraza"
                supabaseClient.postgrest.rpc(function, mapOf("p_event_id" to eventId))
                Unit
            }

        override suspend fun toggleThreadVote(
            threadId: String,
            voted: Boolean,
        ): Resource<Unit> =
            safeCall {
                if (voted) {
                    supabaseClient.postgrest
                        .from("thread_votes")
                        .insert(ThreadVoteDto(thread_id = threadId, user_id = currentUserId()))
                } else {
                    supabaseClient.postgrest
                        .from("thread_votes")
                        .delete {
                            filter {
                                eq("thread_id", threadId)
                                eq("user_id", currentUserId())
                            }
                        }
                }
            }

        override suspend fun toggleVolunteer(
            projectId: String,
            joined: Boolean,
        ): Resource<Unit> =
            safeCall {
                val function = if (joined) "volunteer_project" else "withdraw_volunteer"
                supabaseClient.postgrest.rpc(function, mapOf("p_project_id" to projectId))
                Unit
            }

        private suspend fun currentUserId(): String =
            supabaseClient.auth
                .currentSessionOrNull()
                ?.user
                ?.id ?: error("Not authenticated")

        private fun EventDto.toDomain(attending: Boolean) =
            BarazaEvent(
                id = id,
                title = title,
                mode = mode,
                councilLabel = council_label,
                committeeLabel = committee_label,
                districtLabel = district_label,
                householdsLabel = households_label,
                quorumLabel = quorum_label,
                dayLabel = day_label,
                dateLabel = date_label,
                timeLabel = time_label,
                venue = venue,
                streamNote = stream_note,
                agenda = agenda,
                attendingCount = attending_count,
                questionsQueued = questions_queued,
                eventAt = (event_at ?: created_at).toEpochMillis(),
                iAmAttending = attending,
            )

        private fun ThreadDto.toDomain(voted: Boolean) =
            ForumThread(
                id = id,
                authorName = profiles?.display_name.orEmpty().ifBlank { "Jirani" },
                tag = tag,
                title = title,
                body = body,
                solved = solved,
                votes = thread_votes.firstOrNull().toCount(),
                replyCount = thread_replies.firstOrNull().toCount(),
                createdAt = created_at.toEpochMillis(),
                votedByMe = voted,
            )

        private fun ProjectDto.toDomain(volunteering: Boolean) =
            HarambeeProject(
                id = id,
                status = status,
                statusLabel = status_label,
                phaseLabel = phase_label,
                categoryLabel = category_label,
                title = title,
                subtitle = subtitle,
                iconLabel = icon_label,
                photoUrl = photo_url,
                targetKes = target_kes,
                raisedKes = raised_kes,
                progressPct = progress_pct,
                progressLabel = progress_label,
                volunteerSlots = volunteer_slots,
                volunteersFilled = volunteers_filled,
                volunteerNote = volunteer_note,
                iAmVolunteering = volunteering,
            )

        private fun CountDto?.toCount(): Int = this?.count?.coerceIn(0L, Int.MAX_VALUE.toLong())?.toInt() ?: 0

        private fun String.toEpochMillis(): Long =
            runCatching {
                Instant.parse(this).toEpochMilli()
            }.getOrDefault(System.currentTimeMillis())

        private companion object {
            const val EMPTY_UUID = "00000000-0000-0000-0000-000000000000"

            const val EVENT_COLUMNS =
                "id, title, mode, council_label, committee_label, district_label, households_label, " +
                    "quorum_label, day_label, date_label, time_label, venue, stream_note, agenda, " +
                    "attending_count, questions_queued, event_at, created_at"

            const val THREAD_COLUMNS =
                "id, author_id, tag, title, body, solved, created_at, " +
                    "profiles!forum_threads_author_id_fkey(display_name), " +
                    "thread_votes(count), thread_replies(count)"

            const val PROJECT_COLUMNS =
                "id, status, status_label, phase_label, category_label, title, subtitle, icon_label, " +
                    "photo_url, target_kes, raised_kes, progress_pct, progress_label, " +
                    "volunteer_slots, volunteers_filled, volunteer_note, created_at"
        }
    }

@Serializable
private data class IdField(
    val event_id: String,
)

@Serializable
private data class IdField2(
    val thread_id: String,
)

@Serializable
private data class IdField3(
    val project_id: String,
)

@Serializable
private data class ThreadVoteDto(
    val thread_id: String,
    val user_id: String,
)
