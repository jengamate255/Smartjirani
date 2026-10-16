package com.smartjirani.feature.civic

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class ReportDto(
    val id: String,
    val public_reference: String,
    val title: String,
    val description: String = "",
    val status: String,
    val severity: String = "MEDIUM",
    val priority: String = "NORMAL",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location_description: String? = null,
    val due_at: String? = null,
    val escalation_level: Int = 0,
    val report_count: Int = 1,
    val resolution_description: String? = null,
    val resident_confirmation: String? = null,
    val canonical_report_id: String? = null,
    val reporter_id: String? = null,
    val image_url: String? = null,
    val created_at: String,
    val updated_at: String,
    val civic_categories: CategoryDto? = null,
)

@Serializable
private data class CategoryDto(
    val id: String,
    val name_en: String = "",
    val name_sw: String = "",
    val icon: String? = null,
    val requires_verification: Boolean = false,
)

@Serializable
private data class RoleDto(
    val role: String,
    val organization_id: String,
    val org_unit_id: String? = null,
)

@Serializable
private data class UnitDto(
    val id: String,
    val name: String,
    val parent_id: String? = null,
)

@Serializable
private data class HistoryDto(
    val from_status: String? = null,
    val to_status: String,
    val changed_by_role: String? = null,
    val reason: String? = null,
    val created_at: String,
)

@Serializable
private data class AssignmentDto(
    val id: String,
    val assigned_to: String? = null,
    val assigned_by: String? = null,
    val org_unit_id: String? = null,
    val note: String? = null,
    val status: String? = null,
    val assigned_at: String,
)

@Serializable
private data class AnnouncementDto(
    val id: String,
    val organization_id: String,
    val title: String,
    val body: String,
    val category: String = "NOTICE",
    val pinned: Boolean = false,
    val published_at: String,
    val profiles: PublisherDto? = null,
)

@Serializable
private data class PublisherDto(
    val display_name: String = "",
)

@Serializable
private data class AnalyticsDto(
    val total: Int = 0,
    val active: Int = 0,
    val overdue: Int = 0,
    val escalated: Int = 0,
    val duplicates_folded: Int = 0,
    val avg_resolution_hours: Double? = null,
    val sla_met: Int = 0,
    val sla_missed: Int = 0,
    val by_status: List<StatusCountDto> = emptyList(),
    val by_severity: List<SeverityCountDto> = emptyList(),
    val by_category: List<CategoryCountDto> = emptyList(),
    val trend_14d: List<DayCountDto> = emptyList(),
)

@Serializable
private data class StatusCountDto(
    val status: String,
    val count: Int = 0,
)

@Serializable
private data class SeverityCountDto(
    val severity: String,
    val count: Int = 0,
)

@Serializable
private data class CategoryCountDto(
    val category_id: String,
    val category: String,
    val category_sw: String? = null,
    val count: Int = 0,
)

@Serializable
private data class DayCountDto(
    val day: String,
    val count: Int = 0,
)

@Serializable
private data class AdminUnitTypeDto(
    val id: String,
    val name: String,
    val level: Int = 0,
    val is_department: Boolean = false,
)

@Serializable
private data class AdminUnitDto(
    val id: String,
    val name: String,
    val code: String? = null,
    val parent_id: String? = null,
    val unit_type_id: String,
    val unit_type: String,
    val unit_level: Int = 0,
    val is_active: Boolean = true,
)

@Serializable
private data class AdminSlaRuleDto(
    val id: String,
    val category_id: String? = null,
    val category_sw: String? = null,
    val severity: String? = null,
    val priority: String? = null,
    val response_hours: Double,
    val resolution_hours: Double,
    val is_active: Boolean = true,
)

@Serializable
private data class AdminRoleDto(
    val id: String,
    val user_id: String,
    val role: String,
    val org_unit_id: String,
    val unit_name: String,
    val display_name: String? = null,
    val email: String? = null,
    val is_active: Boolean = true,
)

@Serializable
private data class AdminDirectoryDto(
    val id: String,
    val display_name: String? = null,
    val email: String? = null,
    val username: String? = null,
)

@Serializable
private data class AdminCategoryDto(
    val id: String,
    val name_en: String = "",
    val name_sw: String = "",
    val description: String? = null,
    val requires_verification: Boolean = true,
)

@Singleton
class CivicRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
    ) : CivicRepository {
        override suspend fun getQueue(statusFilter: String?): Resource<CivicQueueData> =
            safeCall {
                val userId = currentUserId()

                val roles =
                    supabaseClient.postgrest
                        .rpc("civic_active_roles", mapOf("p_user" to userId))
                        .decodeList<RoleDto>()
                        .map { CivicRole(it.role, it.organization_id, it.org_unit_id) }

                val reportBuilder = supabaseClient.postgrest.from("civic_reports")
                val reports =
                    reportBuilder
                        .select(Columns.raw(REPORT_COLUMNS)) {
                            if (statusFilter != null) filter { eq("status", statusFilter) }
                            order("created_at", Order.DESCENDING)
                            limit(200)
                        }.decodeList<ReportDto>()
                        // Clusters: dashboards show canonical issues only; the duplicate
                        // count rides along on report_count.
                        .filter { it.canonical_report_id == null }
                        .map { it.toDomain() }

                val categories =
                    supabaseClient.postgrest
                        .from("civic_categories")
                        .select(Columns.raw(CATEGORY_COLUMNS)) {
                            filter { eq("is_active", true) }
                            order("sort_order", Order.ASCENDING)
                        }.decodeList<CategoryDto>()
                        .map {
                            CivicCategory(it.id, it.name_en, it.name_sw, it.icon, it.requires_verification)
                        }

                val units =
                    supabaseClient.postgrest
                        .from("org_units")
                        .select(Columns.raw("id, name, parent_id")) {
                            filter { eq("is_active", true) }
                            order("name", Order.ASCENDING)
                        }.decodeList<UnitDto>()
                        .map { CivicUnit(it.id, it.name, it.parent_id) }

                CivicQueueData(reports, roles, categories, units)
            }

        override suspend fun getAnalytics(): Resource<CivicAnalytics> =
            safeCall {
                val dto =
                    supabaseClient.postgrest
                        .rpc("civic_analytics")
                        .decodeAs<AnalyticsDto>()
                CivicAnalytics(
                    total = dto.total,
                    active = dto.active,
                    overdue = dto.overdue,
                    escalated = dto.escalated,
                    duplicatesFolded = dto.duplicates_folded,
                    avgResolutionHours = dto.avg_resolution_hours,
                    slaMet = dto.sla_met,
                    slaMissed = dto.sla_missed,
                    byStatus = dto.by_status.map { CivicCount(it.status, it.count) },
                    bySeverity = dto.by_severity.map { CivicCount(it.severity, it.count) },
                    byCategory =
                        dto.by_category.map {
                            CivicCategoryCount(
                                categoryId = it.category_id,
                                label = it.category,
                                labelSw = it.category_sw ?: it.category,
                                count = it.count,
                            )
                        },
                    trend14d = dto.trend_14d.map { CivicDayCount(it.day, it.count) },
                )
            }

        override suspend fun getMyRoles(): Resource<List<CivicRole>> =
            safeCall {
                val userId = currentUserId()
                supabaseClient.postgrest
                    .rpc("civic_active_roles", mapOf("p_user" to userId))
                    .decodeList<RoleDto>()
                    .map { CivicRole(it.role, it.organization_id, it.org_unit_id) }
            }

        override suspend fun getReportDetail(reportId: String): Resource<CivicReportDetail> =
            safeCall {
                val dto =
                    supabaseClient.postgrest
                        .from("civic_reports")
                        .select(Columns.raw("$REPORT_COLUMNS, civic_report_status_history(*), civic_report_assignments(*)")) {
                            filter { eq("id", reportId) }
                            limit(1)
                        }.decodeList<ReportDetailDto>()
                        .firstOrNull() ?: error("Ripoti haipatikani")

                CivicReportDetail(
                    report = dto.toReport(),
                    history =
                        dto.civic_report_status_history
                            .sortedBy { it.created_at }
                            .map {
                                CivicHistoryEntry(
                                    fromStatus = it.from_status,
                                    toStatus = it.to_status,
                                    changedByRole = it.changed_by_role,
                                    reason = it.reason,
                                    createdAt = it.created_at,
                                )
                            },
                    assignments =
                        dto.civic_report_assignments
                            .sortedByDescending { it.assigned_at }
                            .map {
                                CivicAssignment(
                                    id = it.id,
                                    assignedTo = it.assigned_to,
                                    assignedBy = it.assigned_by,
                                    orgUnitId = it.org_unit_id,
                                    note = it.note,
                                    status = it.status,
                                    assignedAt = it.assigned_at,
                                )
                            },
                    isReporter = dto.reporter_id != null && dto.reporter_id == currentUserId(),
                )
            }

        override suspend fun getMapReports(): Resource<List<CivicReport>> =
            safeCall {
                supabaseClient.postgrest
                    .from("civic_reports")
                    .select(Columns.raw(REPORT_COLUMNS)) {
                        order("created_at", Order.DESCENDING)
                        limit(300)
                    }.decodeList<ReportDto>()
                    .filter { it.canonical_report_id == null && it.latitude != null && it.longitude != null }
                    .map { it.toDomain() }
            }

        override suspend fun transitionReport(
            reportId: String,
            toStatus: String,
            reason: String?,
        ): Resource<Unit> =
            safeCall {
                supabaseClient.postgrest.rpc(
                    "transition_report",
                    buildMap {
                        put("p_report_id", reportId)
                        put("p_to_status", toStatus)
                        if (!reason.isNullOrBlank()) put("p_reason", reason)
                    },
                )
                Unit
            }

        override suspend fun assignReport(
            reportId: String,
            unitId: String,
            assigneeId: String?,
            note: String?,
        ): Resource<Unit> =
            safeCall {
                supabaseClient.postgrest.rpc(
                    "assign_civic_report",
                    buildMap {
                        put("p_report_id", reportId)
                        put("p_unit_id", unitId)
                        if (!assigneeId.isNullOrBlank()) put("p_assignee", assigneeId)
                        if (!note.isNullOrBlank()) put("p_note", note)
                    },
                )
                Unit
            }

        override suspend fun markDuplicate(
            reportId: String,
            canonicalId: String,
        ): Resource<Unit> =
            safeCall {
                supabaseClient.postgrest.rpc(
                    "mark_duplicate",
                    mapOf("p_report_id" to reportId, "p_canonical_id" to canonicalId),
                )
                Unit
            }

        override suspend fun findReportByRef(reference: String): Resource<CivicReport?> =
            safeCall {
                val query = reference.trim()
                val isUuid = query.length == 36 && query.count { it == '-' } == 4
                supabaseClient.postgrest
                    .from("civic_reports")
                    .select(Columns.raw(REPORT_COLUMNS)) {
                        filter {
                            if (isUuid) eq("id", query) else eq("public_reference", query)
                        }
                        limit(1)
                    }.decodeList<ReportDto>()
                    .firstOrNull()
                    ?.takeIf { it.id != query }
                    ?.toDomain()
            }

        override suspend fun getCategories(): Resource<List<CivicCategory>> =
            safeCall {
                supabaseClient.postgrest
                    .rpc("civic_list_categories")
                    .decodeList<CategoryDto>()
                    .map { CivicCategory(it.id, it.name_en, it.name_sw, it.icon, it.requires_verification) }
            }

        override suspend fun submitReport(
            categoryId: String,
            title: String,
            description: String,
            latitude: Double?,
            longitude: Double?,
            locationDescription: String?,
            severity: String,
            isAnonymous: Boolean,
            imageUrl: String?,
        ): Resource<CivicSubmitResult> =
            safeCall {
                val json =
                    supabaseClient.postgrest
                        .rpc(
                            "submit_civic_report",
                            buildMap {
                                put("p_category_id", categoryId)
                                put("p_title", title)
                                put("p_description", description)
                                if (latitude != null && longitude != null) {
                                    put("p_latitude", latitude)
                                    put("p_longitude", longitude)
                                }
                                if (!locationDescription.isNullOrBlank()) put("p_location_description", locationDescription)
                                put("p_severity", severity)
                                put("p_is_anonymous", isAnonymous)
                                put("p_source", "APP")
                                if (!imageUrl.isNullOrBlank()) put("p_image_url", imageUrl)
                            },
                        ).decodeAs<JsonObject>()

                if (json["ok"]?.jsonPrimitive?.booleanOrNull != true) {
                    error(json["error"]?.jsonPrimitive?.content ?: "submission_failed")
                }
                CivicSubmitResult(
                    reportId = json["report_id"]?.jsonPrimitive?.content.orEmpty(),
                    publicReference = json["public_reference"]?.jsonPrimitive?.content.orEmpty(),
                    merged = json["merged"]?.jsonPrimitive?.booleanOrNull == true,
                    canonicalReference = json["canonical_reference"]?.jsonPrimitive?.content,
                )
            }

        override suspend fun getMyReports(): Resource<List<CivicReport>> =
            safeCall {
                val userId = currentUserId()
                supabaseClient.postgrest
                    .from("civic_reports")
                    .select(Columns.raw(REPORT_COLUMNS)) {
                        filter { eq("reporter_id", userId) }
                        order("created_at", Order.DESCENDING)
                        limit(100)
                    }.decodeList<ReportDto>()
                    .map { it.toDomain() }
            }

        override suspend fun uploadReportImage(imageBytes: ByteArray): Resource<String> =
            safeCall {
                val path = "civic/${UUID.randomUUID()}.jpg"
                supabaseClient.storage.from("media").upload(path, imageBytes)
                supabaseClient.storage.from("media").publicUrl(path)
            }

        override suspend fun getAnnouncements(): Resource<List<CivicAnnouncement>> =
            safeCall {
                supabaseClient.postgrest
                    .from("civic_announcements")
                    .select(Columns.raw(ANNOUNCEMENT_COLUMNS)) {
                        order("published_at", Order.DESCENDING)
                        limit(50)
                    }.decodeList<AnnouncementDto>()
                    .map { it.toDomain() }
                    .sortedByDescending { it.pinned }
            }

        override suspend fun publishAnnouncement(
            title: String,
            body: String,
            category: String,
            pinned: Boolean,
        ): Resource<Unit> =
            safeCall {
                val json =
                    supabaseClient.postgrest
                        .rpc(
                            "publish_civic_announcement",
                            mapOf(
                                "p_title" to title,
                                "p_body" to body,
                                "p_category" to category,
                                "p_pinned" to pinned,
                            ),
                        ).decodeAs<JsonObject>()

                if (json["ok"]?.jsonPrimitive?.booleanOrNull != true) {
                    error(json["error"]?.jsonPrimitive?.content ?: "publish_failed")
                }
                Unit
            }

        override suspend fun getAdminData(): Resource<CivicAdminData> =
            safeCall {
                val unitTypes =
                    supabaseClient.postgrest
                        .rpc("civic_admin_unit_types")
                        .decodeList<AdminUnitTypeDto>()
                        .map { CivicAdminUnitType(it.id, it.name, it.level, it.is_department) }

                val units =
                    supabaseClient.postgrest
                        .rpc("civic_admin_units")
                        .decodeList<AdminUnitDto>()
                        .map {
                            CivicAdminUnit(
                                id = it.id,
                                name = it.name,
                                code = it.code,
                                parentId = it.parent_id,
                                unitTypeId = it.unit_type_id,
                                unitType = it.unit_type,
                                unitLevel = it.unit_level,
                                isActive = it.is_active,
                            )
                        }

                val slaRules =
                    supabaseClient.postgrest
                        .rpc("civic_admin_sla_rules")
                        .decodeList<AdminSlaRuleDto>()
                        .map {
                            CivicSlaRule(
                                id = it.id,
                                categoryId = it.category_id,
                                categorySw = it.category_sw,
                                severity = it.severity,
                                priority = it.priority,
                                responseHours = it.response_hours,
                                resolutionHours = it.resolution_hours,
                                isActive = it.is_active,
                            )
                        }

                val roles =
                    supabaseClient.postgrest
                        .rpc("civic_admin_roles")
                        .decodeList<AdminRoleDto>()
                        .map {
                            CivicRoleRow(
                                id = it.id,
                                userId = it.user_id,
                                role = it.role,
                                orgUnitId = it.org_unit_id,
                                unitName = it.unit_name,
                                displayName = it.display_name,
                                email = it.email,
                                isActive = it.is_active,
                            )
                        }

                val categories =
                    supabaseClient.postgrest
                        .rpc("civic_admin_categories")
                        .decodeList<AdminCategoryDto>()
                        .map {
                            CivicAdminCategory(
                                id = it.id,
                                nameEn = it.name_en,
                                nameSw = it.name_sw,
                                description = it.description,
                                requiresVerification = it.requires_verification,
                            )
                        }

                CivicAdminData(unitTypes, units, slaRules, roles, categories)
            }

        override suspend fun searchDirectory(query: String): Resource<List<CivicDirectoryUser>> =
            safeCall {
                supabaseClient.postgrest
                    .rpc("civic_admin_directory", mapOf("p_query" to query.trim()))
                    .decodeList<AdminDirectoryDto>()
                    .map {
                        CivicDirectoryUser(
                            id = it.id,
                            displayName = it.display_name,
                            email = it.email,
                            username = it.username,
                        )
                    }
            }

        override suspend fun createUnit(
            unitTypeId: String,
            name: String,
            parentId: String?,
            code: String?,
        ): Resource<Unit> =
            safeCall {
                val json =
                    supabaseClient.postgrest
                        .rpc(
                            "civic_admin_create_unit",
                            buildMap {
                                put("p_unit_type_id", unitTypeId)
                                put("p_name", name)
                                if (parentId != null) put("p_parent_id", parentId)
                                if (!code.isNullOrBlank()) put("p_code", code)
                            },
                        ).decodeAs<JsonObject>()

                if (json["ok"]?.jsonPrimitive?.booleanOrNull != true) {
                    error(json["error"]?.jsonPrimitive?.content ?: "create_unit_failed")
                }
                Unit
            }

        override suspend fun createCategory(
            nameEn: String,
            nameSw: String,
            description: String?,
            requiresVerification: Boolean,
        ): Resource<Unit> =
            safeCall {
                val json =
                    supabaseClient.postgrest
                        .rpc(
                            "civic_admin_create_category",
                            buildMap {
                                put("p_name_en", nameEn)
                                put("p_name_sw", nameSw)
                                if (!description.isNullOrBlank()) put("p_description", description)
                                put("p_requires_verification", requiresVerification)
                            },
                        ).decodeAs<JsonObject>()

                if (json["ok"]?.jsonPrimitive?.booleanOrNull != true) {
                    error(json["error"]?.jsonPrimitive?.content ?: "create_category_failed")
                }
                Unit
            }

        override suspend fun createSlaRule(
            categoryId: String?,
            severity: String?,
            priority: String?,
            responseHours: Double,
            resolutionHours: Double,
        ): Resource<Unit> =
            safeCall {
                val json =
                    supabaseClient.postgrest
                        .rpc(
                            "civic_admin_upsert_sla_rule",
                            buildMap {
                                if (categoryId != null) put("p_category_id", categoryId)
                                if (!severity.isNullOrBlank()) put("p_severity", severity)
                                if (!priority.isNullOrBlank()) put("p_priority", priority)
                                put("p_response_hours", responseHours)
                                put("p_resolution_hours", resolutionHours)
                            },
                        ).decodeAs<JsonObject>()

                if (json["ok"]?.jsonPrimitive?.booleanOrNull != true) {
                    error(json["error"]?.jsonPrimitive?.content ?: "sla_rule_failed")
                }
                Unit
            }

        override suspend fun assignRole(
            userId: String,
            role: String,
            orgUnitId: String,
        ): Resource<Unit> =
            safeCall {
                val json =
                    supabaseClient.postgrest
                        .rpc(
                            "civic_admin_assign_role",
                            mapOf(
                                "p_user_id" to userId,
                                "p_role" to role,
                                "p_org_unit_id" to orgUnitId,
                            ),
                        ).decodeAs<JsonObject>()

                if (json["ok"]?.jsonPrimitive?.booleanOrNull != true) {
                    error(json["error"]?.jsonPrimitive?.content ?: "assign_role_failed")
                }
                Unit
            }

        override suspend fun revokeRole(roleRowId: String): Resource<Unit> =
            safeCall {
                val json =
                    supabaseClient.postgrest
                        .rpc(
                            "civic_admin_revoke_role",
                            mapOf("p_role_id" to roleRowId),
                        ).decodeAs<JsonObject>()

                if (json["ok"]?.jsonPrimitive?.booleanOrNull != true) {
                    error(json["error"]?.jsonPrimitive?.content ?: "revoke_role_failed")
                }
                Unit
            }

        private fun AnnouncementDto.toDomain() =
            CivicAnnouncement(
                id = id,
                organizationId = organization_id,
                title = title,
                body = body,
                category = category,
                pinned = pinned,
                publishedByName = profiles?.display_name?.takeIf { it.isNotBlank() },
                publishedAt = published_at,
            )

        private suspend fun currentUserId(): String =
            supabaseClient.auth
                .currentSessionOrNull()
                ?.user
                ?.id ?: error("Not authenticated")

        private fun ReportDto.toDomain() =
            CivicReport(
                id = id,
                publicReference = public_reference,
                title = title,
                description = description,
                status = status,
                severity = severity,
                priority = priority,
                categoryName =
                    civic_categories?.name_sw?.takeIf { it.isNotBlank() }
                        ?: civic_categories?.name_en,
                latitude = latitude,
                longitude = longitude,
                locationDescription = location_description,
                dueAt = due_at,
                escalationLevel = escalation_level,
                reportCount = report_count,
                resolutionDescription = resolution_description,
                residentConfirmation = resident_confirmation,
                imageUrl = image_url,
                createdAt = created_at,
                updatedAt = updated_at,
            )

        private fun ReportDetailDto.toReport(): CivicReport =
            ReportDto(
                id = id,
                public_reference = public_reference,
                title = title,
                description = description,
                status = status,
                severity = severity,
                priority = priority,
                latitude = latitude,
                longitude = longitude,
                location_description = location_description,
                due_at = due_at,
                escalation_level = escalation_level,
                report_count = report_count,
                resolution_description = resolution_description,
                resident_confirmation = resident_confirmation,
                canonical_report_id = canonical_report_id,
                reporter_id = reporter_id,
                image_url = image_url,
                created_at = created_at,
                updated_at = updated_at,
                civic_categories = civic_categories,
            ).toDomain()

        private companion object {
            const val REPORT_COLUMNS =
                "id, public_reference, title, description, status, severity, priority, latitude, " +
                    "longitude, location_description, due_at, escalation_level, report_count, " +
                    "resolution_description, resident_confirmation, canonical_report_id, " +
                    "reporter_id, image_url, " +
                    "created_at, updated_at, civic_categories(id, name_en, name_sw, icon, requires_verification)"

            const val CATEGORY_COLUMNS = "id, name_en, name_sw, icon, requires_verification"

            const val ANNOUNCEMENT_COLUMNS =
                "id, organization_id, title, body, category, pinned, published_at, profiles(display_name)"
        }

        @Serializable
        private data class ReportDetailDto(
            val id: String,
            val public_reference: String,
            val title: String,
            val description: String = "",
            val status: String,
            val severity: String = "MEDIUM",
            val priority: String = "NORMAL",
            val latitude: Double? = null,
            val longitude: Double? = null,
            val location_description: String? = null,
            val due_at: String? = null,
            val escalation_level: Int = 0,
            val report_count: Int = 1,
            val resolution_description: String? = null,
            val resident_confirmation: String? = null,
            val canonical_report_id: String? = null,
            val reporter_id: String? = null,
            val image_url: String? = null,
            val created_at: String,
            val updated_at: String,
            val civic_categories: CategoryDto? = null,
            val civic_report_status_history: List<HistoryDto> = emptyList(),
            val civic_report_assignments: List<AssignmentDto> = emptyList(),
        )
    }
