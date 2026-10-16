package com.smartjirani.feature.civic.channels

import kotlinx.serialization.Serializable

interface CivicChannelProvider {
    val name: String

    suspend fun deliver(payload: CivicNotificationPayload): ChannelResult
}

@Serializable
data class CivicNotificationPayload(
    val notificationId: String,
    val recipientUserId: String,
    val toPhone: String,
    val title: String,
    val body: String,
)

sealed class ChannelResult {
    data class Sent(
        val externalId: String,
    ) : ChannelResult()

    data class Failed(
        val code: String,
        val detail: String,
    ) : ChannelResult()

    data class Skipped(
        val reason: String,
    ) : ChannelResult()
}

data class CivicChannelConfig(
    val twilioAccountSid: String? = null,
    val twilioAuthToken: String? = null,
    val twilioFromNumber: String? = null,
    val africasTalkingUsername: String? = null,
    val africasTalkingApiKey: String? = null,
    val africasTalkingFrom: String? = null,
    val smsProvider: String? = null,
    val whatsappProvider: String? = null,
)
