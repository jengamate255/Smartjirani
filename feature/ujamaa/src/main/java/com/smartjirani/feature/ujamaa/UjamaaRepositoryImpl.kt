package com.smartjirani.feature.ujamaa

import android.util.Log
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.network.PesaPalClient
import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class NeedDto(
    val id: String,
    val title: String,
    val body: String = "",
    val requester_label: String = "",
    val requester_meta: String = "",
    val distance_label: String = "",
    val status_label: String = "",
    val is_open: Boolean = true,
    val created_at: String,
)

@Serializable
private data class NeedCountDto(
    val id: String,
    val urgent_need_offers: List<CountDto> = emptyList(),
)

@Serializable
private data class ResourceDto(
    val id: String,
    val category: String = "TOOL",
    val tag_label: String = "",
    val title: String,
    val description: String = "",
    val image_url: String? = null,
    val distance_label: String = "",
    val history_label: String = "",
    val availability_note: String = "",
    val deposit_note: String = "",
    val cta_label: String = "",
    val terms_label: String = "",
    val available_until: String = "",
    val is_available: Boolean = true,
    val meta: JsonObject? = null,
)

@Serializable
private data class FundDto(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val audit_label: String = "",
    val target_kes: Double = 0.0,
    val raised_kes: Double = 0.0,
    val progress_pct: Int = 0,
    val purpose_label: String = "",
    val remaining_label: String = "",
)

@Serializable
private data class CountDto(
    val count: Long = 0,
)

@Serializable
private data class CountRow(
    val count: Long = 0,
)

@Serializable
private data class OfferDto(
    val need_id: String,
)

@Serializable
private data class LoanDto(
    val resource_id: String,
    val status: String = "REQUESTED",
)

@Serializable
private data class ContributionDto(
    val id: String,
    val checkout_request_id: String = "",
)

@Singleton
class UjamaaRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
        private val pesaPalClient: PesaPalClient,
    ) : UjamaaRepository {
        override suspend fun getContent(neighborhoodId: String): Resource<UjamaaContent> =
            safeCall {
                Log.d(TAG, "getContent: starting for neighborhoodId=$neighborhoodId")
                val userId =
                    supabaseClient.auth
                        .currentSessionOrNull()
                        ?.user
                        ?.id
                Log.d(TAG, "getContent: userId=$userId")

                val needs =
                    supabaseClient.postgrest
                        .from("urgent_needs")
                        .select(Columns.raw(NEED_COLUMNS)) {
                            filter {
                                eq("neighborhood_id", neighborhoodId)
                                eq("is_open", true)
                            }
                            order("created_at", Order.DESCENDING)
                            limit(6)
                        }.decodeList<NeedDto>()
                Log.d(TAG, "getContent: fetched ${needs.size} needs")

                val resources =
                    supabaseClient.postgrest
                        .from("resources")
                        .select(Columns.raw(RESOURCE_COLUMNS)) {
                            filter {
                                eq("neighborhood_id", neighborhoodId)
                                eq("is_available", true)
                            }
                            order("created_at", Order.DESCENDING)
                            limit(12)
                        }.decodeList<ResourceDto>()
                Log.d(TAG, "getContent: fetched ${resources.size} resources")

                val fund =
                    supabaseClient.postgrest
                        .from("harambee_funds")
                        .select(Columns.raw(FUND_COLUMNS)) {
                            filter { eq("neighborhood_id", neighborhoodId) }
                            limit(1)
                        }.decodeList<FundDto>()
                        .firstOrNull()
                Log.d(TAG, "getContent: fetched fund=${fund?.id}")

                val myOffers =
                    if (userId == null) {
                        emptySet()
                    } else {
                        supabaseClient.postgrest
                            .from("urgent_need_offers")
                            .select(Columns.raw("need_id")) { filter { eq("helper_id", userId) } }
                            .decodeList<OfferDto>()
                            .mapTo(mutableSetOf()) { it.need_id }
                    }

                val myLoans =
                    if (userId == null) {
                        emptySet()
                    } else {
                        supabaseClient.postgrest
                            .from("resource_loans")
                            .select(Columns.raw("resource_id, status")) { filter { eq("borrower_id", userId) } }
                            .decodeList<LoanDto>()
                            .filter { it.status != "DECLINED" && it.status != "RETURNED" }
                            .mapTo(mutableSetOf()) { it.resource_id }
                    }

                val stats =
                    UjamaaStats(
                        neighborsHelped = countOfferedHelp(neighborhoodId),
                        activeBorrows = countActiveLoans(),
                        sharedItems = resources.size,
                        fundRaisedKes = fund?.raised_kes ?: 0.0,
                    )

                UjamaaContent(
                    stats = stats,
                    needs = needs.map { it.toDomain(it.id in myOffers) },
                    resources = resources.map { it.toDomain(it.id in myLoans) },
                    fund = fund?.toDomain(),
                )
            }

        override suspend fun offerHelp(needId: String): Resource<Unit> =
            safeCall {
                supabaseClient.postgrest
                    .from("urgent_need_offers")
                    .insert(NeedOfferDto(need_id = needId, helper_id = currentUserId()))
            }

        override suspend fun withdrawHelp(needId: String): Resource<Unit> =
            safeCall {
                supabaseClient.postgrest
                    .from("urgent_need_offers")
                    .delete {
                        filter {
                            eq("need_id", needId)
                            eq("helper_id", currentUserId())
                        }
                    }
            }

        override suspend fun requestResource(resourceId: String): Resource<Unit> =
            safeCall {
                supabaseClient.postgrest
                    .from("resource_loans")
                    .insert(
                        ResourceLoanDto(
                            resource_id = resourceId,
                            borrower_id = currentUserId(),
                            status = "REQUESTED",
                        ),
                    )
            }

        override suspend fun contribute(
            fundId: String,
            amountKes: Double,
        ): Resource<ContributionResult> =
            safeCall {
                require(amountKes > 0) { "Chagua kiasi sahihi" }
                val reference = "SMJ-${UUID.randomUUID().toString().take(8).uppercase()}"

                if (!pesaPalClient.isConfigured) {
                    // Development / demo mode: confirm immediately, auditable as SIMULATED-*.
                    recordContribution(fundId, amountKes, reference, "", "CONFIRMED")
                    return@safeCall ContributionResult(amountKes = amountKes, reference = reference)
                }

                val order =
                    pesaPalClient.createOrder(
                        amountKes = amountKes,
                        description = "SmartJirani Harambee",
                        reference = reference,
                    )
                ContributionResult(
                    amountKes = amountKes,
                    reference = order.orderTrackingId,
                    checkoutUrl = order.redirectUrl.ifBlank { null },
                )
            }

        override suspend fun settleContribution(
            orderTrackingId: String,
            neighborhoodId: String,
        ): Resource<Boolean> =
            safeCall {
                val status = pesaPalClient.getPaymentStatus(orderTrackingId)
                if (!status.isCompleted) return@safeCall false

                // Idempotent: a contribution already recorded for this order is never charged twice.
                val alreadyRecorded =
                    supabaseClient.postgrest
                        .from("harambee_contributions")
                        .select(Columns.raw("id")) {
                            filter { eq("checkout_request_id", orderTrackingId) }
                            limit(1)
                        }.decodeList<ContributionDto>()
                        .isNotEmpty()
                if (alreadyRecorded) return@safeCall true

                val fundId =
                    supabaseClient.postgrest
                        .from("harambee_funds")
                        .select(Columns.raw("id")) {
                            filter { eq("neighborhood_id", neighborhoodId) }
                            limit(1)
                        }.decodeList<ContributionDto>()
                        .firstOrNull()
                        ?.id
                        ?: return@safeCall false

                recordContribution(
                    fundId = fundId,
                    amountKes = status.amount?.toDoubleOrNull() ?: 0.0,
                    reference = orderTrackingId,
                    checkoutRequestId = orderTrackingId,
                    status = "CONFIRMED",
                )
                true
            }

        private suspend fun recordContribution(
            fundId: String,
            amountKes: Double,
            reference: String,
            checkoutRequestId: String,
            status: String,
        ) {
            supabaseClient.postgrest.rpc(
                "record_contribution",
                mapOf(
                    "p_fund_id" to fundId,
                    "p_amount_kes" to amountKes,
                    "p_reference" to reference,
                    "p_checkout_request_id" to checkoutRequestId,
                    "p_status" to status,
                ),
            )
        }

        private suspend fun countOfferedHelp(neighborhoodId: String): Int {
            val rows =
                supabaseClient.postgrest
                    .from("urgent_needs")
                    .select(Columns.raw("id, urgent_need_offers(count)")) {
                        filter { eq("neighborhood_id", neighborhoodId) }
                    }.decodeList<NeedCountDto>()
            return rows.sumOf { row -> row.urgent_need_offers.firstOrNull()?.count ?: 0L }.toInt()
        }

        private suspend fun countActiveLoans(): Int {
            val rows =
                supabaseClient.postgrest
                    .from("resource_loans")
                    .select(Columns.raw("id")) {
                        filter { eq("status", "ACTIVE") }
                    }.decodeList<LoanIdDto>()
            return rows.size
        }

        private suspend fun currentUserId(): String =
            supabaseClient.auth
                .currentSessionOrNull()
                ?.user
                ?.id ?: error("Not authenticated")

        private fun NeedDto.toDomain(offered: Boolean) =
            UrgentNeed(
                id = id,
                title = title,
                body = body,
                requesterLabel = requester_label,
                requesterMeta = requester_meta,
                distanceLabel = distance_label,
                statusLabel = status_label,
                isOpen = is_open,
                iOfferedHelp = offered,
            )

        private fun ResourceDto.toDomain(requested: Boolean) =
            SharedResource(
                id = id,
                category = category,
                tagLabel = tag_label,
                title = title,
                description = description,
                imageUrl = image_url,
                distanceLabel = distance_label,
                historyLabel = history_label,
                availabilityNote = availability_note,
                depositNote = deposit_note,
                ctaLabel = cta_label,
                termsLabel = terms_label,
                routeLabel = meta?.string("route"),
                seatLabel = meta?.string("seats"),
                departureLabel = meta?.string("departure"),
                isAvailable = is_available,
                iRequested = requested,
            )

        private fun FundDto.toDomain() =
            HarambeeFund(
                id = id,
                name = name,
                subtitle = subtitle,
                auditLabel = audit_label,
                targetKes = target_kes,
                raisedKes = raised_kes,
                progressPct = progress_pct,
                purposeLabel = purpose_label,
                remainingLabel = remaining_label,
            )

        private fun JsonObject.string(key: String): String? = this[key]?.toString()?.trim('"')?.takeIf { it.isNotBlank() }

        private companion object {
            const val TAG = "UjamaaRepository"
            const val NEED_COLUMNS =
                "id, title, body, requester_label, requester_meta, distance_label, status_label, " +
                    "is_open, created_at"

            const val RESOURCE_COLUMNS =
                "id, category, tag_label, title, description, image_url, distance_label, " +
                    "history_label, availability_note, deposit_note, cta_label, terms_label, " +
                    "available_until, is_available, meta"

            const val FUND_COLUMNS =
                "id, name, subtitle, audit_label, target_kes, raised_kes, progress_pct, " +
                    "purpose_label, remaining_label"
        }
    }

@Serializable
private data class NeedOfferDto(
    val need_id: String,
    val helper_id: String,
)

@Serializable
private data class ResourceLoanDto(
    val resource_id: String,
    val borrower_id: String,
    val status: String,
)

@Serializable
private data class LoanIdDto(
    val id: String,
)
