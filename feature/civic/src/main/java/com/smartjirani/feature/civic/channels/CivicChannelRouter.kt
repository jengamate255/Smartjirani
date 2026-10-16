package com.smartjirani.feature.civic.channels

class CivicChannelRouter(
    private val registry: CivicChannelRegistry,
) {
    data class PlannedDelivery(
        val channel: String,
        val providerName: String,
    )

    fun planFor(pref: String): List<PlannedDelivery> {
        val wantsSms = pref.contains("sms")
        val wantsWhatsApp = pref.contains("whatsapp")
        val wantsInApp = pref.contains("in_app") || (!wantsSms && !wantsWhatsApp)
        val active = registry.active()
        val plan = mutableListOf<PlannedDelivery>()
        if (wantsInApp) plan += PlannedDelivery("in_app", "in_app")
        if (wantsSms) {
            val smsProv = active.firstOrNull { it.name.startsWith("sms_") }
            if (smsProv != null) {
                plan += PlannedDelivery("sms", smsProv.name)
            } else {
                plan += PlannedDelivery("sms", "skipped_no_provider")
            }
        }
        if (wantsWhatsApp) {
            val waProv = active.firstOrNull { it.name.startsWith("whatsapp_") && it.name != "whatsapp_noop" }
            if (waProv != null) {
                plan += PlannedDelivery("whatsapp", waProv.name)
            } else {
                plan += PlannedDelivery("whatsapp", "whatsapp_noop")
            }
        }
        return plan
    }

    suspend fun route(
        payload: CivicNotificationPayload,
        planned: List<PlannedDelivery>,
    ): List<Pair<PlannedDelivery, ChannelResult>> {
        val activeByName = registry.active().associateBy { it.name }
        return planned.map { p ->
            val provider = activeByName[p.providerName]
            val result =
                when {
                    provider != null -> provider.deliver(payload)
                    p.providerName == "skipped_no_provider" -> ChannelResult.Skipped("no_provider")
                    else -> ChannelResult.Skipped("no_provider")
                }
            p to result
        }
    }
}
