package com.smartjirani.feature.profile

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
private data class KarmaDto(
    val score: Double = 0.0,
    val trust_pct: Int = 0,
    val actions_count: Int = 0,
    val favors_done: Int = 0,
    val shared_items: Int = 0,
    val barazas: Int = 0,
    val rank_label: String = "",
)

@Serializable
private data class LockerItemDto(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val state_label: String = "Tayari",
    val is_available: Boolean = true,
    val loans_count: Int = 0,
)

@Serializable
private data class SkillDto(
    val id: String,
    val title: String,
    val description: String = "",
    val testimonial: String = "",
    val endorsement_count: Int = 0,
)

@Serializable
private data class ReviewDto(
    val id: String,
    val author_label: String = "",
    val body: String,
    val rating: Int = 5,
    val created_at: String = "",
)

@Serializable
private data class SecurityCircleDto(
    val id: String,
    val profile_id: String,
    val cluster_label: String = "",
    val designation_label: String = "",
    val sos_enabled: Boolean = true,
    val contacts: List<SecurityContactDto> = emptyList(),
)

@Serializable
private data class SecurityContactDto(
    val label: String = "",
    val icon: String = "person",
)

@Serializable
private data class PrivacyDto(
    val id: String,
    val profile_id: String,
    val visibility_label: String = "",
    val visibility_radius_m: Int = 500,
    val house_number_visibility: String = "",
    val household_label: String = "",
    val status_label: String = "Imelindwa",
    val show_house_number: Boolean = false,
)

@Serializable
private data class HouseholdMemberDto(
    val id: String,
    val member_name: String,
    val relation_label: String = "",
)

@Singleton
class ProfileExtrasRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
    ) : ProfileExtrasRepository {
        override suspend fun getExtras(userId: String): Resource<ProfileExtras> =
            safeCall {
                val karma =
                    supabaseClient.postgrest
                        .rpc(
                            "karma_summary",
                            mapOf("p_profile_id" to userId),
                        ).decodeList<KarmaDto>()
                        .firstOrNull()
                        ?.toDomain() ?: KarmaSummary()

                val lockerItems =
                    supabaseClient.postgrest
                        .from("shared_items")
                        .select(Columns.raw("id, title, subtitle, state_label, is_available, loans_count")) {
                            filter { eq("owner_id", userId) }
                            order("created_at", Order.DESCENDING)
                        }.decodeList<LockerItemDto>()
                        .map { it.toDomain() }

                val skills =
                    supabaseClient.postgrest
                        .from("skills_offered")
                        .select(Columns.raw("id, title, description, testimonial, endorsement_count")) {
                            filter { eq("owner_id", userId) }
                            order("created_at", Order.DESCENDING)
                        }.decodeList<SkillDto>()
                        .map { it.toDomain() }

                val reviews =
                    supabaseClient.postgrest
                        .from("profile_reviews")
                        .select(Columns.raw("id, author_label, body, rating, created_at")) {
                            filter { eq("subject_id", userId) }
                            order("created_at", Order.DESCENDING)
                            limit(6)
                        }.decodeList<ReviewDto>()
                        .map { it.toDomain() }

                val securityCircle =
                    supabaseClient.postgrest
                        .from("security_circle")
                        .select(Columns.raw(SECURITY_COLUMNS)) {
                            filter { eq("profile_id", userId) }
                            limit(1)
                        }.decodeList<SecurityCircleDto>()
                        .firstOrNull()
                        ?.toDomain()

                val privacy =
                    supabaseClient.postgrest
                        .from("privacy_settings")
                        .select(Columns.raw(PRIVACY_COLUMNS)) {
                            filter { eq("profile_id", userId) }
                            limit(1)
                        }.decodeList<PrivacyDto>()
                        .firstOrNull()
                        ?.toDomain()

                val household =
                    supabaseClient.postgrest
                        .from("household_members")
                        .select(Columns.raw("id, member_name, relation_label")) {
                            filter { eq("profile_id", userId) }
                            order("created_at", Order.ASCENDING)
                        }.decodeList<HouseholdMemberDto>()
                        .map { it.toDomain() }

                ProfileExtras(
                    karma = karma,
                    lockerItems = lockerItems,
                    skills = skills,
                    reviews = reviews,
                    securityCircle = securityCircle,
                    privacy = privacy,
                    household = household,
                )
            }

        override suspend fun setSosEnabled(enabled: Boolean): Resource<Unit> =
            safeCall {
                val userId = currentUserId()
                val existing = selectSecurityCircle(userId)
                if (existing == null) {
                    supabaseClient.postgrest
                        .from("security_circle")
                        .insert(
                            SecurityCircleWriteDto(
                                profile_id = userId,
                                cluster_label = "",
                                designation_label = "",
                                sos_enabled = enabled,
                            ),
                        )
                } else {
                    supabaseClient.postgrest
                        .from("security_circle")
                        .update(SecurityCircleUpdateDto(sos_enabled = enabled)) {
                            filter { eq("profile_id", userId) }
                        }
                }
            }

        override suspend fun setHouseNumberVisible(visible: Boolean): Resource<Unit> =
            safeCall {
                val userId = currentUserId()
                val existing = selectPrivacy(userId)
                if (existing == null) {
                    supabaseClient.postgrest
                        .from("privacy_settings")
                        .insert(
                            PrivacyWriteDto(
                                profile_id = userId,
                                show_house_number = visible,
                                house_number_visibility = if (visible) "Majirani wote" else "Majirani waliothibitishwa tu",
                            ),
                        )
                } else {
                    supabaseClient.postgrest
                        .from("privacy_settings")
                        .update(
                            PrivacyUpdateDto(
                                show_house_number = visible,
                                house_number_visibility = if (visible) "Majirani wote" else "Majirani waliothibitishwa tu",
                            ),
                        ) {
                            filter { eq("profile_id", userId) }
                        }
                }
            }

        private suspend fun selectSecurityCircle(userId: String): SecurityCircleDto? =
            supabaseClient.postgrest
                .from("security_circle")
                .select(Columns.raw(SECURITY_COLUMNS)) {
                    filter { eq("profile_id", userId) }
                    limit(1)
                }.decodeList<SecurityCircleDto>()
                .firstOrNull()

        private suspend fun selectPrivacy(userId: String): PrivacyDto? =
            supabaseClient.postgrest
                .from("privacy_settings")
                .select(Columns.raw(PRIVACY_COLUMNS)) {
                    filter { eq("profile_id", userId) }
                    limit(1)
                }.decodeList<PrivacyDto>()
                .firstOrNull()

        private suspend fun currentUserId(): String =
            supabaseClient.auth
                .currentSessionOrNull()
                ?.user
                ?.id ?: error("Not authenticated")

        private fun KarmaDto.toDomain() =
            KarmaSummary(
                score = score,
                trustPct = trust_pct,
                actionsCount = actions_count,
                favorsDone = favors_done,
                sharedItems = shared_items,
                barazas = barazas,
                rankLabel = rank_label,
            )

        private fun LockerItemDto.toDomain() =
            LockerItem(
                id = id,
                title = title,
                subtitle = subtitle,
                stateLabel = state_label,
                isAvailable = is_available,
                loansCount = loans_count,
            )

        private fun SkillDto.toDomain() =
            SkillOffer(
                id = id,
                title = title,
                description = description,
                testimonial = testimonial,
                endorsementCount = endorsement_count,
            )

        private fun ReviewDto.toDomain() =
            NeighborReview(
                id = id,
                authorLabel = author_label,
                body = body,
                rating = rating,
            )

        private fun SecurityCircleDto.toDomain() =
            SecurityCircle(
                id = id,
                clusterLabel = cluster_label,
                designationLabel = designation_label,
                sosEnabled = sos_enabled,
                contacts = contacts.map { SecurityContact(it.label, it.icon) },
            )

        private fun PrivacyDto.toDomain() =
            PrivacySettings(
                id = id,
                visibilityLabel = visibility_label,
                visibilityRadiusM = visibility_radius_m,
                houseNumberVisibility = house_number_visibility,
                householdLabel = household_label,
                statusLabel = status_label,
                showHouseNumber = show_house_number,
            )

        private fun HouseholdMemberDto.toDomain() =
            HouseholdMember(
                id = id,
                memberName = member_name,
                relationLabel = relation_label,
            )

        private companion object {
            const val SECURITY_COLUMNS =
                "id, profile_id, cluster_label, designation_label, sos_enabled, contacts"
            const val PRIVACY_COLUMNS =
                "id, profile_id, visibility_label, visibility_radius_m, house_number_visibility, " +
                    "household_label, status_label, show_house_number"
        }
    }

@Serializable
private data class SecurityCircleWriteDto(
    val profile_id: String,
    val cluster_label: String,
    val designation_label: String,
    val sos_enabled: Boolean,
)

@Serializable
private data class SecurityCircleUpdateDto(
    val sos_enabled: Boolean,
)

@Serializable
private data class PrivacyWriteDto(
    val profile_id: String,
    val show_house_number: Boolean,
    val house_number_visibility: String,
)

@Serializable
private data class PrivacyUpdateDto(
    val show_house_number: Boolean,
    val house_number_visibility: String,
)
