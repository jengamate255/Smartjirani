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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class WhatsAppTwilioProvider(
    private val cfg: CivicChannelConfig,
    private val http: HttpClient = defaultHttp(),
) : CivicChannelProvider {
    override val name = "whatsapp_twilio"

    override suspend fun deliver(payload: CivicNotificationPayload): ChannelResult {
        require(cfg.whatsappProvider == "twilio") { "CIVIC_WHATSAPP_PROVIDER != twilio" }
        val sid =
            cfg.twilioAccountSid
                ?: return ChannelResult.Failed("whatsapp_twilio_no_config", "missing account SID")
        val token =
            cfg.twilioAuthToken
                ?: return ChannelResult.Failed("whatsapp_twilio_no_config", "missing auth token")
        val from =
            cfg.twilioWhatsappFrom
                ?: return ChannelResult.Failed("whatsapp_twilio_no_config", "missing whatsapp from number")

        val url = "https://api.twilio.com/2010-04-01/Accounts/$sid/Messages.json"
        val bodyJson =
            buildJsonObject {
                put("To", "whatsapp:${payload.toPhone}")
                put("From", "whatsapp:$from")
                put("Body", payload.body)
            }
        val resp =
            http.post(url) {
                basicAuth(sid, token)
                contentType(ContentType.Application.Json)
                setBody(bodyJson.toString())
            }
        val text = resp.bodyAsText()
        return when (resp.status) {
            HttpStatusCode.Created, HttpStatusCode.OK -> {
                val messageSid =
                    runCatching {
                        Json.parseToJsonElement(text).let { (it as JsonObject)["sid"]?.jsonPrimitive?.content }
                    }.getOrNull() ?: "unknown"
                ChannelResult.Sent(messageSid)
            }

            else -> {
                ChannelResult.Failed("whatsapp_twilio_${resp.status.value}", text)
            }
        }
    }
}

private fun defaultHttp(): HttpClient = HttpClient(CIO)
