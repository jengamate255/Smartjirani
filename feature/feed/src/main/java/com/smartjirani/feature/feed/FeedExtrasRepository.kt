package com.smartjirani.feature.feed

import com.smartjirani.core.common.result.Resource

/** Row pinned above the feed: temperature, power, gate and the street note. */
data class StreetStatus(
    val id: String,
    val temperatureC: Int,
    val powerStatus: String,
    val gateStatus: String,
    val streetNote: String,
)

data class StreetNotice(
    val id: String,
    val eyebrow: String,
    val body: String,
    val authority: String,
    val statusLabel: String,
)

data class PollOption(
    val id: String,
    val label: String,
    val votes: Int,
    val percentage: Int,
    val isMine: Boolean,
)

data class StreetPoll(
    val id: String,
    val eyebrow: String,
    val question: String,
    val subtext: String,
    val endsLabel: String,
    val options: List<PollOption>,
    val totalVotes: Int,
) {
    val hasVoted: Boolean get() = options.any { it.isMine }
}

/** Everything the Mtaa Feed shows besides the post list itself. */
data class FeedExtras(
    val streetStatus: StreetStatus? = null,
    val notices: List<StreetNotice> = emptyList(),
    val polls: List<StreetPoll> = emptyList(),
) {
    val isEmpty: Boolean
        get() = streetStatus == null && notices.isEmpty() && polls.isEmpty()
}

interface FeedExtrasRepository {
    suspend fun getFeedExtras(neighborhoodId: String): Resource<FeedExtras>

    suspend fun vote(
        pollId: String,
        optionId: String,
    ): Resource<Unit>
}
