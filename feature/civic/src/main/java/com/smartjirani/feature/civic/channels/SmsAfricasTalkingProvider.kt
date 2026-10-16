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

class SmsAfricasTalkingProvider(
    private val cfg: CivicChannelConfig,
    private val http: HttpClient = defaultHttp(),
) : CivicChannelProvider {
    override val name = "sms_africas_talking"

    override suspend fun deliver(payload: CivicNotificationPayload): ChannelResult {
        require(cfg.smsProvider == "africas_talking") { "CIVIC_SMS_PROVIDER != africas_talking" }
        val username =
            cfg.africasTalkingUsername
                ?: return ChannelResult.Failed("africas_talking_no_config", "missing username")
        val apiKey =
            cfg.africasTalkingApiKey
                ?: return ChannelResult.Failed("africas_talking_no_config", "missing API key")
        val from =
            cfg.africasTalkingFrom
                ?: return ChannelResult.Failed("africas_talking_no_config", "missing from sender")

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
        val body = resp.bodyAsText()
        return when (resp.status) {
            HttpStatusCode.OK, HttpStatusCode.Created, HttpStatusCode.Accepted -> {
                val mid =
                    runCatching {
                        Json.parseToJsonElement(body).let {
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
                ChannelResult.Failed("africas_talking_${resp.status.value}", body)
            }
        }
    }
}

private fun defaultHttp(): HttpClient = HttpClient(CIO)
