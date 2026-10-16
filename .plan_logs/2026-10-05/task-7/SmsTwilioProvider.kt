package com.smartjirani.feature.civic.channels

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class SmsTwilioProvider(
    private val cfg: CivicChannelConfig,
    private val http: HttpClient = defaultHttp(),
) : CivicChannelProvider {
    override val name = "sms_twilio"

    override suspend fun deliver(payload: CivicNotificationPayload): ChannelResult {
        require(cfg.smsProvider == "twilio") { "CIVIC_SMS_PROVIDER != twilio" }
        val sid = cfg.twilioAccountSid ?: return ChannelResult.Failed("twilio_no_config", "missing account SID")
        val token = cfg.twilioAuthToken ?: return ChannelResult.Failed("twilio_no_config", "missing auth token")
        val from = cfg.twilioFromNumber ?: return ChannelResult.Failed("twilio_no_config", "missing from number")

        val url = "https://api.twilio.com/2010-04-01/Accounts/$sid/Messages.json"
        val form = "To=${payload.toPhone}&From=$from&Body=${payload.body}"
        val resp =
            http.post(url) {
                basicAuth(sid, token)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(form)
            }
        val body = resp.bodyAsText()
        return when (resp.status) {
            HttpStatusCode.Created, HttpStatusCode.OK -> {
                val messageSid =
                    runCatching {
                        Json.parseToJsonElement(body).let { (it as JsonObject)["sid"]?.jsonPrimitive?.content }
                    }.getOrNull() ?: "unknown"
                ChannelResult.Sent(messageSid)
            }

            else -> {
                ChannelResult.Failed("twilio_${resp.status.value}", body)
            }
        }
    }
}

private fun defaultHttp(): HttpClient = HttpClient(CIO)
