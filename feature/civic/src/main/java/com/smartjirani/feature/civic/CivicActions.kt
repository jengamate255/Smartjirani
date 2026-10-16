package com.smartjirani.feature.civic

/**
 * Status-machine actions the UI offers for a report in a given status. The
 * server re-validates every move, so the UI list is a convenience: a stale
 * client only gets a clean error, never an illegal write.
 */
enum class CivicActionKind { TRANSITION, ASSIGN, CONFIRM, REOPEN, APPEAL }

data class CivicAction(
    val kind: CivicActionKind,
    val toStatus: String? = null,
    val needsReason: Boolean = false,
)

object CivicActions {
    val ALL_STATUSES =
        listOf(
            "NEW",
            "AI_TRIAGED",
            "AWAITING_VERIFICATION",
            "VERIFIED",
            "ASSIGNED",
            "IN_PROGRESS",
            "WAITING_EXTERNAL_ACTION",
            "AWAITING_RESIDENT_CONFIRMATION",
            "RESOLVED",
            "CLOSED",
            "REJECTED",
            "ESCALATED",
        )

    /** Higher rank = broader authority; used only to narrow the UI, server enforces. */
    private val ROLE_RANK =
        mapOf(
            CivicRole.ROLE_MJUMBE to 1,
            CivicRole.ROLE_DEPARTMENT_OFFICER to 1,
            CivicRole.ROLE_MWENYEKITI to 2,
            CivicRole.ROLE_MTAA_OFFICER to 3,
            CivicRole.ROLE_WARD_OFFICER to 4,
            CivicRole.ROLE_MUNICIPAL_OFFICER to 5,
            CivicRole.ROLE_COUNCIL_ADMIN to 6,
            CivicRole.ROLE_SYSTEM_ADMIN to 7,
        )

    private val matrix: Map<String, List<CivicAction>> =
        mapOf(
            "NEW" to
                listOf(
                    CivicAction(CivicActionKind.TRANSITION, "AWAITING_VERIFICATION"),
                    CivicAction(CivicActionKind.TRANSITION, "AI_TRIAGED"),
                    CivicAction(CivicActionKind.TRANSITION, "REJECTED", needsReason = true),
                    CivicAction(CivicActionKind.TRANSITION, "ESCALATED", needsReason = true),
                ),
            "AI_TRIAGED" to
                listOf(
                    CivicAction(CivicActionKind.TRANSITION, "AWAITING_VERIFICATION"),
                    CivicAction(CivicActionKind.TRANSITION, "REJECTED", needsReason = true),
                ),
            "AWAITING_VERIFICATION" to
                listOf(
                    CivicAction(CivicActionKind.TRANSITION, "VERIFIED"),
                    CivicAction(CivicActionKind.TRANSITION, "REJECTED", needsReason = true),
                    CivicAction(CivicActionKind.TRANSITION, "ESCALATED", needsReason = true),
                ),
            "VERIFIED" to
                listOf(
                    CivicAction(CivicActionKind.ASSIGN),
                    CivicAction(CivicActionKind.TRANSITION, "ESCALATED", needsReason = true),
                    CivicAction(CivicActionKind.TRANSITION, "REJECTED", needsReason = true),
                ),
            "ASSIGNED" to
                listOf(
                    CivicAction(CivicActionKind.TRANSITION, "IN_PROGRESS"),
                    CivicAction(CivicActionKind.TRANSITION, "ESCALATED", needsReason = true),
                ),
            "IN_PROGRESS" to
                listOf(
                    CivicAction(CivicActionKind.TRANSITION, "AWAITING_RESIDENT_CONFIRMATION"),
                    CivicAction(CivicActionKind.TRANSITION, "WAITING_EXTERNAL_ACTION"),
                    CivicAction(CivicActionKind.ASSIGN),
                    CivicAction(CivicActionKind.TRANSITION, "ESCALATED", needsReason = true),
                ),
            "WAITING_EXTERNAL_ACTION" to
                listOf(
                    CivicAction(CivicActionKind.TRANSITION, "IN_PROGRESS"),
                    CivicAction(CivicActionKind.TRANSITION, "AWAITING_RESIDENT_CONFIRMATION"),
                    CivicAction(CivicActionKind.TRANSITION, "ESCALATED", needsReason = true),
                ),
            "AWAITING_RESIDENT_CONFIRMATION" to
                listOf(
                    CivicAction(CivicActionKind.TRANSITION, "IN_PROGRESS"),
                ),
            "RESOLVED" to
                listOf(
                    CivicAction(CivicActionKind.TRANSITION, "CLOSED"),
                ),
            "ESCALATED" to
                listOf(
                    CivicAction(CivicActionKind.ASSIGN),
                    CivicAction(CivicActionKind.TRANSITION, "IN_PROGRESS"),
                    CivicAction(CivicActionKind.TRANSITION, "REJECTED", needsReason = true),
                ),
            "REJECTED" to
                listOf(
                    CivicAction(CivicActionKind.TRANSITION, "AWAITING_VERIFICATION"),
                ),
            "CLOSED" to emptyList(),
        )

    fun actionsFor(
        status: String,
        roles: List<CivicRole>,
    ): List<CivicAction> {
        val rank = roles.mapNotNull { ROLE_RANK[it.role] }.maxOrNull() ?: return emptyList()
        return (matrix[status] ?: emptyList()).filter { action ->
            when {
                // MTAA_OFFICER and above may escalate out of WAITING_EXTERNAL_ACTION
                action.toStatus == "ESCALATED" && status == "WAITING_EXTERNAL_ACTION" -> {
                    rank >= ROLE_RANK.getValue(CivicRole.ROLE_MTAA_OFFICER)
                }

                // Rejection after verification needs WARD_OFFICER and above
                action.toStatus == "REJECTED" && status == "VERIFIED" -> {
                    rank >= ROLE_RANK.getValue(CivicRole.ROLE_WARD_OFFICER)
                }

                // Only MUNICIPAL+ may reject an escalated report
                action.toStatus == "REJECTED" && status == "ESCALATED" -> {
                    rank >= ROLE_RANK.getValue(CivicRole.ROLE_MUNICIPAL_OFFICER)
                }

                // WARD+ handles escalated reports back into the flow
                status == "ESCALATED" -> {
                    rank >= ROLE_RANK.getValue(CivicRole.ROLE_WARD_OFFICER)
                }

                else -> {
                    true
                }
            }
        }
    }

    fun hasAnyOfficialRole(roles: List<CivicRole>): Boolean = roles.any { ROLE_RANK.containsKey(it.role) }

    /**
     * Reporter-owned moves: confirm a requested resolution, dispute one
     * (reopen), or appeal a rejection. The server still verifies the caller
     * actually authored the report.
     */
    fun reporterActionsFor(status: String): List<CivicAction> =
        when (status) {
            "AWAITING_RESIDENT_CONFIRMATION" -> {
                listOf(
                    CivicAction(CivicActionKind.CONFIRM, "RESOLVED"),
                    CivicAction(CivicActionKind.REOPEN, "IN_PROGRESS", needsReason = true),
                )
            }

            "REJECTED" -> {
                listOf(
                    CivicAction(CivicActionKind.APPEAL, "AWAITING_VERIFICATION", needsReason = true),
                )
            }

            else -> {
                emptyList()
            }
        }
}
