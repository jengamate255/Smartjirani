package com.smartjirani.feature.civic.channels

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class WhatsAppAfricasTalkingProvider(
    private val cfg: CivicChannelConfig,
    private val http: HttpClient = defaultHttp(),
) : CivicChannelProvider {
    override val name = "whatsapp_africas_talking"

    override suspend fun deliver(payload: CivicNotificationPayload): ChannelResult {
        require(cfg.whatsappProvider == "africas_talking") { "CIVIC_WHATSAPP_PROVIDER != africas_talking" }
        val username =
            cfg.africasTalkingUsername
                ?: return ChannelResult.Failed("whatsapp_at_no_config", "missing username")
        val apiKey =
            cfg.africasTalkingApiKey
                ?: return ChannelResult.Failed("whatsapp_at_no_config", "missing API key")
        val from =
            cfg.africasTalkingWhatsappFrom
                ?: return ChannelResult.Failed("whatsapp_at_no_config", "missing whatsapp from")

        val url = "https://api.africastalking.com/version1/messaging"
        val form = "username=$username&to=${payload.toPhone}&message=${payload.body}&from=$from"
        val resp =
            http.post(url) {
                headers {
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    append("apiKey", apiKey)
                }
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(form)
            }
        val text = resp.bodyAsText()
        return when (resp.status) {
            HttpStatusCode.OK, HttpStatusCode.Created, HttpStatusCode.Accepted -> {
                val mid =
                    runCatching {
                        Json.parseToJsonElement(text).let {
                            (it as JsonObject)["SMSMessageData"]
                                ?.jsonObject
                                ?.get("MessageId")
                                ?.jsonPrimitive
                                ?.content
                        }
                    }.getOrNull() ?: "unknown"
                ChannelResult.Sent(mid)
            }

            else -> {
                ChannelResult.Failed("whatsapp_at_${resp.status.value}", text)
            }
        }
    }
}

private fun defaultHttp(): HttpClient = HttpClient(CIO)
