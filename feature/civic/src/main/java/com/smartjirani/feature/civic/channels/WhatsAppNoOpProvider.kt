package com.smartjirani.feature.civic.channels

class WhatsAppNoOpProvider : CivicChannelProvider {
    override val name = "whatsapp_noop"

    override suspend fun deliver(payload: CivicNotificationPayload): ChannelResult = ChannelResult.Skipped("whatsapp_noop")
}
