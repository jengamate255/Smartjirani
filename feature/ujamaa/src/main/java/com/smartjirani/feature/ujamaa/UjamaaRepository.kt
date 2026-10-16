package com.smartjirani.feature.ujamaa

import com.smartjirani.core.common.result.Resource

/** Live "street ticker" numbers pinned to the top of the Ujamaa Hub. */
data class UjamaaStats(
    val neighborsHelped: Int = 0,
    val activeBorrows: Int = 0,
    val sharedItems: Int = 0,
    val fundRaisedKes: Double = 0.0,
)

data class UrgentNeed(
    val id: String,
    val title: String,
    val body: String,
    val requesterLabel: String,
    val requesterMeta: String,
    val distanceLabel: String,
    val statusLabel: String,
    val isOpen: Boolean,
    val iOfferedHelp: Boolean,
)

data class SharedResource(
    val id: String,
    val category: String,
    val tagLabel: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val distanceLabel: String,
    val historyLabel: String,
    val availabilityNote: String,
    val depositNote: String,
    val ctaLabel: String,
    val termsLabel: String,
    val routeLabel: String?,
    val seatLabel: String?,
    val departureLabel: String?,
    val isAvailable: Boolean,
    val iRequested: Boolean,
)

data class HarambeeFund(
    val id: String,
    val name: String,
    val subtitle: String,
    val auditLabel: String,
    val targetKes: Double,
    val raisedKes: Double,
    val progressPct: Int,
    val purposeLabel: String,
    val remainingLabel: String,
) {
    val progressLabel: String get() = "$progressPct% imekusanywa"
}

data class UjamaaContent(
    val stats: UjamaaStats = UjamaaStats(),
    val needs: List<UrgentNeed> = emptyList(),
    val resources: List<SharedResource> = emptyList(),
    val fund: HarambeeFund? = null,
) {
    val isEmpty: Boolean get() = needs.isEmpty() && resources.isEmpty() && fund == null
}

/** Filter chips shown above the urgent needs section. */
enum class UjamaaFilter(
    val label: String,
) {
    ALL("Zote"),
    TOOLS("Zana & Vifaa"),
    RIDES("Usafiri"),
    BOOKS("Vitabu"),
    HARVEST("Mazao"),
    SKILLS("Ujuzi"),
}

data class ContributionResult(
    /** KSh that was charged (or would be charged). */
    val amountKes: Double,
    /** PesaPal order tracking id, or a SIMULATED-* reference when no credentials are set. */
    val reference: String,
    /** Non-null when the user still has to finish checkout in a browser / M-Pesa prompt. */
    val checkoutUrl: String? = null,
) {
    val isSettled: Boolean get() = checkoutUrl == null
}

interface UjamaaRepository {
    suspend fun getContent(neighborhoodId: String): Resource<UjamaaContent>

    suspend fun offerHelp(needId: String): Resource<Unit>

    suspend fun withdrawHelp(needId: String): Resource<Unit>

    suspend fun requestResource(resourceId: String): Resource<Unit>

    suspend fun contribute(
        fundId: String,
        amountKes: Double,
    ): Resource<ContributionResult>

    suspend fun settleContribution(
        orderTrackingId: String,
        neighborhoodId: String,
    ): Resource<Boolean>
}
