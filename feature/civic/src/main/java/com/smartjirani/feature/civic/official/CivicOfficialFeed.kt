package com.smartjirani.feature.civic.official

import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CivicOfficialFeedRow(
    val report_id: String,
    val ref: String,
    val title: String,
    val category_sw: String,
    val category_en: String,
    val severity: String,
    val status: String,
    val due_at_minutes: Int,
    val assigned_unit_name: String,
    val created_at: String,
    val neighborhood_name: String,
)

enum class CivicOfficialFilter(
    val value: String,
) {
    ALL("all"),
    AWAITING_VERIFICATION("awaiting_verification"),
    OVERDUE("overdue"),
    MINE("mine"),
    NEW_TODAY("new_today"),
}

@Singleton
class CivicOfficialFeed
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
    ) {
        suspend fun list(
            filter: CivicOfficialFilter,
            scopeUnit: String? = null,
        ): List<CivicOfficialFeedRow> =
            supabaseClient.postgrest
                .rpc(
                    "civic_official_feed",
                    mapOf(
                        "p_filter" to filter.value,
                        "p_scope_unit" to scopeUnit,
                    ),
                ).decodeList<CivicOfficialFeedRow>()
    }
