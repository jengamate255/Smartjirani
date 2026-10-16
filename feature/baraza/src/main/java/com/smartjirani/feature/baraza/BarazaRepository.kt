package com.smartjirani.feature.baraza

import com.smartjirani.core.common.result.Resource

data class BarazaEvent(
    val id: String,
    val title: String,
    val mode: String,
    val councilLabel: String,
    val committeeLabel: String,
    val districtLabel: String,
    val householdsLabel: String,
    val quorumLabel: String,
    val dayLabel: String,
    val dateLabel: String,
    val timeLabel: String,
    val venue: String,
    val streamNote: String,
    val agenda: List<String>,
    val attendingCount: Int,
    val questionsQueued: Int = 0,
    val eventAt: Long,
    val iAmAttending: Boolean,
)

data class ForumThread(
    val id: String,
    val authorName: String,
    val tag: String,
    val title: String,
    val body: String,
    val solved: Boolean,
    val votes: Int,
    val replyCount: Int,
    val createdAt: Long,
    val votedByMe: Boolean,
)

data class HarambeeProject(
    val id: String,
    val status: String,
    val statusLabel: String,
    val phaseLabel: String,
    val categoryLabel: String,
    val title: String,
    val subtitle: String,
    val iconLabel: String,
    val photoUrl: String?,
    val targetKes: Double,
    val raisedKes: Double,
    val progressPct: Int,
    val progressLabel: String,
    val volunteerSlots: Int,
    val volunteersFilled: Int,
    val volunteerNote: String,
    val iAmVolunteering: Boolean,
) {
    val isJoinable: Boolean get() = status != "CLOSED"
    val hasMoneyGoal: Boolean get() = targetKes > 0
}

data class BarazaContent(
    val events: List<BarazaEvent> = emptyList(),
    val threads: List<ForumThread> = emptyList(),
    val projects: List<HarambeeProject> = emptyList(),
) {
    val isEmpty: Boolean get() = events.isEmpty() && threads.isEmpty() && projects.isEmpty()
}

/** Everything the Baraza screen renders, plus the three actions it exposes. */
interface BarazaRepository {
    suspend fun getContent(neighborhoodId: String): Resource<BarazaContent>

    suspend fun toggleRsvp(
        eventId: String,
        attending: Boolean,
    ): Resource<Unit>

    suspend fun toggleThreadVote(
        threadId: String,
        voted: Boolean,
    ): Resource<Unit>

    suspend fun toggleVolunteer(
        projectId: String,
        joined: Boolean,
    ): Resource<Unit>
}
