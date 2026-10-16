package com.smartjirani.feature.feed

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class StreetStatusDto(
    val id: String,
    val temperature_c: Int = 24,
    val power_status: String = "NORMAL",
    val gate_status: String = "WAZI",
    val street_note: String = "",
    val updated_at: String = "",
)

@Serializable
private data class NoticeDto(
    val id: String,
    val eyebrow: String = "",
    val body: String,
    val authority: String = "",
    val status_label: String = "",
    val expires_at: String? = null,
    val created_at: String,
)

@Serializable
private data class PollDto(
    val id: String,
    val eyebrow: String = "",
    val question: String,
    val subtext: String = "",
    val ends_label: String = "",
    val ends_at: String? = null,
    val created_at: String,
)

@Serializable
private data class OptionDto(
    val id: String,
    val poll_id: String,
    val label: String,
    val position: Int = 0,
    val poll_votes: List<CountDto> = emptyList(),
)

@Serializable
private data class CountDto(
    val count: Long = 0,
)

@Serializable
private data class MyVoteDto(
    val poll_id: String,
    val option_id: String,
)

@Singleton
class FeedExtrasRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
    ) : FeedExtrasRepository {
        override suspend fun getFeedExtras(neighborhoodId: String): Resource<FeedExtras> =
            safeCall {
                val status =
                    supabaseClient.postgrest
                        .from("street_status")
                        .select(Columns.raw(STATUS_COLUMNS)) {
                            filter { eq("neighborhood_id", neighborhoodId) }
                            limit(1)
                        }.decodeList<StreetStatusDto>()
                        .firstOrNull()
                        ?.toDomain()

                val notices =
                    supabaseClient.postgrest
                        .from("street_notices")
                        .select(Columns.raw(NOTICE_COLUMNS)) {
                            filter { eq("neighborhood_id", neighborhoodId) }
                            order("created_at", Order.DESCENDING)
                            limit(3)
                        }.decodeList<NoticeDto>()
                        .filter { it.isActive() }
                        .map { it.toDomain() }

                val polls =
                    supabaseClient.postgrest
                        .from("polls")
                        .select(Columns.raw(POLL_COLUMNS)) {
                            filter { eq("neighborhood_id", neighborhoodId) }
                            order("created_at", Order.DESCENDING)
                            limit(2)
                        }.decodeList<PollDto>()
                        .filter { it.isLive() }

                val pollsWithOptions =
                    polls.map { poll ->
                        val options =
                            supabaseClient.postgrest
                                .from("poll_options")
                                .select(Columns.raw(OPTION_COLUMNS)) {
                                    filter { eq("poll_id", poll.id) }
                                    order("position", Order.ASCENDING)
                                }.decodeList<OptionDto>()

                        val myVotes =
                            supabaseClient.auth
                                .currentSessionOrNull()
                                ?.user
                                ?.id
                                ?.let { userId ->
                                    supabaseClient.postgrest
                                        .from("poll_votes")
                                        .select(Columns.raw("poll_id, option_id")) {
                                            filter {
                                                eq("user_id", userId)
                                                eq("poll_id", poll.id)
                                            }
                                        }.decodeList<MyVoteDto>()
                                }.orEmpty()
                                .associate { it.poll_id to it.option_id }

                        val total = options.sumOf { it.poll_votes.firstOrNull().toCount() }
                        val mine = myVotes[poll.id]

                        poll.toDomain(
                            options =
                                options.map { option ->
                                    val votes = option.poll_votes.firstOrNull().toCount()
                                    PollOption(
                                        id = option.id,
                                        label = option.label,
                                        votes = votes,
                                        percentage = if (total == 0) 0 else (votes * 100 / total),
                                        isMine = option.id == mine,
                                    )
                                },
                            totalVotes = total,
                        )
                    }

                FeedExtras(streetStatus = status, notices = notices, polls = pollsWithOptions)
            }

        override suspend fun vote(
            pollId: String,
            optionId: String,
        ): Resource<Unit> =
            safeCall {
                supabaseClient.postgrest.rpc(
                    "cast_poll_vote",
                    mapOf("p_poll_id" to pollId, "p_option_id" to optionId),
                )
                Unit
            }

        private fun StreetStatusDto.toDomain() =
            StreetStatus(
                id = id,
                temperatureC = temperature_c,
                powerStatus = power_status,
                gateStatus = gate_status,
                streetNote = street_note,
            )

        private fun NoticeDto.toDomain() =
            StreetNotice(
                id = id,
                eyebrow = eyebrow,
                body = body,
                authority = authority,
                statusLabel = status_label,
            )

        private fun PollDto.toDomain(
            options: List<PollOption>,
            totalVotes: Int,
        ) = StreetPoll(
            id = id,
            eyebrow = eyebrow,
            question = question,
            subtext = subtext,
            endsLabel = ends_label,
            options = options,
            totalVotes = totalVotes,
        )

        private fun NoticeDto.isActive(): Boolean {
            val expiry = expires_at ?: return true
            return runCatching {
                java.time.Instant
                    .parse(expiry)
                    .isAfter(java.time.Instant.now())
            }.getOrDefault(true)
        }

        private fun PollDto.isLive(): Boolean {
            val ends = ends_at ?: return true
            return runCatching {
                java.time.Instant
                    .parse(ends)
                    .isAfter(java.time.Instant.now())
            }.getOrDefault(true)
        }

        private fun CountDto?.toCount(): Int = this?.count?.coerceIn(0L, Int.MAX_VALUE.toLong())?.toInt() ?: 0

        private companion object {
            const val STATUS_COLUMNS = "id, temperature_c, power_status, gate_status, street_note, updated_at"
            const val NOTICE_COLUMNS = "id, eyebrow, body, authority, status_label, expires_at, created_at"
            const val POLL_COLUMNS = "id, eyebrow, question, subtext, ends_label, ends_at, created_at"
            const val OPTION_COLUMNS = "id, poll_id, label, position, poll_votes(count)"
        }
    }
