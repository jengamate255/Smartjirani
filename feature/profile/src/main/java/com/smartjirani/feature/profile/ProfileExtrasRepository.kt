package com.smartjirani.feature.profile

import com.smartjirani.core.common.result.Resource

data class KarmaSummary(
    val score: Double = 0.0,
    val trustPct: Int = 0,
    val actionsCount: Int = 0,
    val favorsDone: Int = 0,
    val sharedItems: Int = 0,
    val barazas: Int = 0,
    val rankLabel: String = "",
)

data class LockerItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val stateLabel: String,
    val isAvailable: Boolean,
    val loansCount: Int,
)

data class SkillOffer(
    val id: String,
    val title: String,
    val description: String,
    val testimonial: String,
    val endorsementCount: Int,
)

data class NeighborReview(
    val id: String,
    val authorLabel: String,
    val body: String,
    val rating: Int,
)

data class SecurityContact(
    val label: String,
    val icon: String = "person",
)

data class SecurityCircle(
    val id: String,
    val clusterLabel: String,
    val designationLabel: String,
    val sosEnabled: Boolean,
    val contacts: List<SecurityContact> = emptyList(),
)

data class PrivacySettings(
    val id: String,
    val visibilityLabel: String,
    val visibilityRadiusM: Int,
    val houseNumberVisibility: String,
    val householdLabel: String,
    val statusLabel: String,
    val showHouseNumber: Boolean,
)

data class HouseholdMember(
    val id: String,
    val memberName: String,
    val relationLabel: String,
)

data class ProfileExtras(
    val karma: KarmaSummary = KarmaSummary(),
    val lockerItems: List<LockerItem> = emptyList(),
    val skills: List<SkillOffer> = emptyList(),
    val reviews: List<NeighborReview> = emptyList(),
    val securityCircle: SecurityCircle? = null,
    val privacy: PrivacySettings? = null,
    val household: List<HouseholdMember> = emptyList(),
)

/** Karma, valuables locker, skills, reviews and the safety/privacy controls. */
interface ProfileExtrasRepository {
    suspend fun getExtras(userId: String): Resource<ProfileExtras>

    suspend fun setSosEnabled(enabled: Boolean): Resource<Unit>

    suspend fun setHouseNumberVisible(visible: Boolean): Resource<Unit>
}
