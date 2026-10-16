package com.smartjirani.feature.civic

import com.smartjirani.core.common.result.Resource

/** One active civic role row from `civic_active_roles`. */
data class CivicRole(
    val role: String,
    val organizationId: String,
    val orgUnitId: String? = null,
) {
    val isOfficial: Boolean get() = role != ROLE_RESIDENT

    companion object {
        const val ROLE_RESIDENT = "RESIDENT"
        const val ROLE_MJUMBE = "MJUMBE"
        const val ROLE_MWENYEKITI = "MWENYEKITI"
        const val ROLE_MTAA_OFFICER = "MTAA_OFFICER"
        const val ROLE_WARD_OFFICER = "WARD_OFFICER"
        const val ROLE_MUNICIPAL_OFFICER = "MUNICIPAL_OFFICER"
        const val ROLE_COUNCIL_ADMIN = "COUNCIL_ADMIN"
        const val ROLE_DEPARTMENT_OFFICER = "DEPARTMENT_OFFICER"
        const val ROLE_SYSTEM_ADMIN = "SYSTEM_ADMIN"
    }
}

data class CivicCategory(
    val id: String,
    val nameEn: String,
    val nameSw: String,
    val icon: String? = null,
    val requiresVerification: Boolean = false,
)

data class CivicUnit(
    val id: String,
    val name: String,
    val parentId: String? = null,
)

data class CivicReport(
    val id: String,
    val publicReference: String,
    val title: String,
    val description: String,
    val status: String,
    val severity: String,
    val priority: String,
    val categoryName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationDescription: String? = null,
    val dueAt: String? = null,
    val escalationLevel: Int = 0,
    val reportCount: Int = 1,
    val resolutionDescription: String? = null,
    val residentConfirmation: String? = null,
    val imageUrl: String? = null,
    val createdAt: String,
    val updatedAt: String,
) {
    val isOverdue: Boolean
        get() =
            dueAt != null && status !in TERMINAL_STATUSES &&
                runCatching {
                    java.time.Instant
                        .parse(dueAt)
                        .isBefore(java.time.Instant.now())
                }.getOrDefault(false)

    companion object {
        val TERMINAL_STATUSES = setOf("RESOLVED", "CLOSED", "REJECTED")
    }
}

data class CivicHistoryEntry(
    val fromStatus: String? = null,
    val toStatus: String,
    val changedByRole: String? = null,
    val reason: String? = null,
    val createdAt: String,
)

data class CivicAssignment(
    val id: String,
    val assignedTo: String? = null,
    val assignedBy: String? = null,
    val orgUnitId: String? = null,
    val note: String? = null,
    val status: String? = null,
    val assignedAt: String,
)

data class CivicReportDetail(
    val report: CivicReport,
    val history: List<CivicHistoryEntry>,
    val assignments: List<CivicAssignment>,
    val isReporter: Boolean = false,
)

/** Outcome of submit_civic_report: either a fresh report or a merge into an existing one. */
data class CivicSubmitResult(
    val reportId: String,
    val publicReference: String,
    val merged: Boolean = false,
    val canonicalReference: String? = null,
)

/** An official announcement published by a government organization. */
data class CivicAnnouncement(
    val id: String,
    val organizationId: String,
    val title: String,
    val body: String,
    val category: String = "NOTICE",
    val pinned: Boolean = false,
    val publishedByName: String? = null,
    val publishedAt: String,
)

data class CivicQueueData(
    val reports: List<CivicReport>,
    val roles: List<CivicRole>,
    val categories: List<CivicCategory>,
    val units: List<CivicUnit>,
) {
    val officialRoles: List<CivicRole> get() = roles.filter { it.isOfficial }
    val isOfficial: Boolean get() = officialRoles.isNotEmpty()
}

/** A single histogram bucket from [CivicAnalytics]. */
data class CivicCount(
    val label: String,
    val count: Int,
)

data class CivicCategoryCount(
    val categoryId: String,
    val label: String,
    val labelSw: String,
    val count: Int,
)

data class CivicDayCount(
    val day: String,
    val count: Int,
)

/**
 * Government analytics computed server-side from the reports the caller can see
 * (officials → their organization, residents → their own reports).
 */
data class CivicAnalytics(
    val total: Int = 0,
    val active: Int = 0,
    val overdue: Int = 0,
    val escalated: Int = 0,
    val duplicatesFolded: Int = 0,
    val avgResolutionHours: Double? = null,
    val slaMet: Int = 0,
    val slaMissed: Int = 0,
    val byStatus: List<CivicCount> = emptyList(),
    val bySeverity: List<CivicCount> = emptyList(),
    val byCategory: List<CivicCategoryCount> = emptyList(),
    val trend14d: List<CivicDayCount> = emptyList(),
) {
    val resolvedTotal: Int get() = slaMet + slaMissed
    val slaRate: Double?
        get() = if (resolvedTotal == 0) null else slaMet.toDouble() / resolvedTotal
}

/** An org-unit type definition (e.g. Mtaa, Kata, Idara) from the admin config. */
data class CivicAdminUnitType(
    val id: String,
    val name: String,
    val level: Int,
    val isDepartment: Boolean,
)

data class CivicAdminUnit(
    val id: String,
    val name: String,
    val code: String? = null,
    val parentId: String? = null,
    val unitTypeId: String,
    val unitType: String,
    val unitLevel: Int,
    val isActive: Boolean,
)

data class CivicSlaRule(
    val id: String,
    val categoryId: String? = null,
    val categorySw: String? = null,
    val severity: String? = null,
    val priority: String? = null,
    val responseHours: Double,
    val resolutionHours: Double,
    val isActive: Boolean = true,
)

data class CivicRoleRow(
    val id: String,
    val userId: String,
    val role: String,
    val orgUnitId: String,
    val unitName: String,
    val displayName: String? = null,
    val email: String? = null,
    val isActive: Boolean = true,
)

data class CivicDirectoryUser(
    val id: String,
    val displayName: String? = null,
    val email: String? = null,
    val username: String? = null,
)

data class CivicAdminCategory(
    val id: String,
    val nameEn: String,
    val nameSw: String,
    val description: String? = null,
    val requiresVerification: Boolean = true,
)

/** Everything the admin configuration screen manages. */
data class CivicAdminData(
    val unitTypes: List<CivicAdminUnitType> = emptyList(),
    val units: List<CivicAdminUnit> = emptyList(),
    val slaRules: List<CivicSlaRule> = emptyList(),
    val roles: List<CivicRoleRow> = emptyList(),
    val categories: List<CivicAdminCategory> = emptyList(),
)

/** Everything the official dashboard, detail and map screens need. */
interface CivicRepository {
    suspend fun getQueue(statusFilter: String? = null): Resource<CivicQueueData>

    /**
     * Aggregate metrics over the visible report set: status/severity/category
     * histograms, SLA outcomes, and a 14-day submission trend.
     */
    suspend fun getAnalytics(): Resource<CivicAnalytics>

    /** Lightweight role probe used by the shell to decide tab visibility. */
    suspend fun getMyRoles(): Resource<List<CivicRole>>

    suspend fun getReportDetail(reportId: String): Resource<CivicReportDetail>

    suspend fun getMapReports(): Resource<List<CivicReport>>

    /** Status-machine move; the server validates role + transition legality. */
    suspend fun transitionReport(
        reportId: String,
        toStatus: String,
        reason: String?,
    ): Resource<Unit>

    /** Route a verified/escalated report to a responsible unit (and optionally a person). */
    suspend fun assignReport(
        reportId: String,
        unitId: String,
        assigneeId: String?,
        note: String?,
    ): Resource<Unit>

    /** Cluster this report under an existing canonical report. */
    suspend fun markDuplicate(
        reportId: String,
        canonicalId: String,
    ): Resource<Unit>

    /** Resolve a public reference (or raw uuid) to a report, for duplicate linking. */
    suspend fun findReportByRef(reference: String): Resource<CivicReport?>

    /** Active categories for the reporter's organization (server-side org resolution). */
    suspend fun getCategories(): Resource<List<CivicCategory>>

    /** Submit a new issue; the RPC auto-resolves org/neighborhood and clusters duplicates. */
    suspend fun submitReport(
        categoryId: String,
        title: String,
        description: String,
        latitude: Double?,
        longitude: Double?,
        locationDescription: String?,
        severity: String = "MEDIUM",
        isAnonymous: Boolean = false,
        imageUrl: String? = null,
    ): Resource<CivicSubmitResult>

    /** Reports submitted by the signed-in resident, newest first (duplicates included). */
    suspend fun getMyReports(): Resource<List<CivicReport>>

    /** Official announcements visible to every authenticated resident (pinned first). */
    suspend fun getAnnouncements(): Resource<List<CivicAnnouncement>>

    /**
     * Publish an official announcement. Server-side the caller must hold an active civic
     * role in their organization; residents receive a forbidden error.
     */
    suspend fun publishAnnouncement(
        title: String,
        body: String,
        category: String = "NOTICE",
        pinned: Boolean = false,
    ): Resource<Unit>

    /** Upload photo evidence to storage; returns the public URL. */
    suspend fun uploadReportImage(imageBytes: ByteArray): Resource<String>

    /** Admin config bundle: unit types, org tree, SLA rules, role rows, categories. */
    suspend fun getAdminData(): Resource<CivicAdminData>

    /** Profile search for role assignment (name / username / email, top 10). */
    suspend fun searchDirectory(query: String): Resource<List<CivicDirectoryUser>>

    suspend fun createUnit(
        unitTypeId: String,
        name: String,
        parentId: String?,
        code: String?,
    ): Resource<Unit>

    suspend fun createCategory(
        nameEn: String,
        nameSw: String,
        description: String?,
        requiresVerification: Boolean,
    ): Resource<Unit>

    /** At least one of category/severity/priority scopes the rule; hours must be positive. */
    suspend fun createSlaRule(
        categoryId: String?,
        severity: String?,
        priority: String?,
        responseHours: Double,
        resolutionHours: Double,
    ): Resource<Unit>

    suspend fun assignRole(
        userId: String,
        role: String,
        orgUnitId: String,
    ): Resource<Unit>

    suspend fun revokeRole(roleRowId: String): Resource<Unit>
}
