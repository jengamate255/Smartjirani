package com.smartjirani.feature.civic

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.call.body
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class CivicAiEnvelope(
    val ok: Boolean = false,
    val error: String? = null,
    // triage
    val categoryId: String? = null,
    val severity: String? = null,
    val confidence: Double? = null,
    val summary: String? = null,
    val transitioned: Boolean? = null,
    // assistant
    val reply: String? = null,
    // shared
    val provider: String? = null,
    val mock: Boolean? = null,
)

@Singleton
class SupabaseCivicAiProvider
    @Inject
    constructor(
        private val supabaseClientProvider: SupabaseClientProvider,
    ) : CivicAiProvider {
        private suspend fun invokeAi(body: kotlinx.serialization.json.JsonObject): Result<CivicAiEnvelope> =
            runCatching {
                val response = supabaseClientProvider.functions.invoke("civic-ai", body = body)
                Json.decodeFromString<CivicAiEnvelope>(response.body<String>())
            }.mapCatching { envelope ->
                if (!envelope.ok) error(envelope.error ?: "civic_ai_failed")
                envelope
            }

        private suspend fun recordAudit(
            reportId: String,
            operation: String,
            provider: String,
            confidence: Double? = null,
            latencyMs: Int? = null,
            errorMessage: String? = null,
        ) {
            runCatching<Unit> {
                supabaseClientProvider.postgrest.rpc(
                    "civic_record_ai_audit",
                    mapOf(
                        "p_report_id" to reportId,
                        "p_operation" to operation,
                        "p_provider" to provider,
                        "p_confidence" to confidence,
                        "p_latency_ms" to latencyMs,
                        "p_error_message" to errorMessage,
                    ),
                )
            }
        }

        override suspend fun triageReport(reportId: String): Resource<CivicAiTriageResult> =
            safeCall {
                val started = System.currentTimeMillis()
                val env =
                    invokeAi(
                        buildJsonObject {
                            put("action", "triage")
                            put("reportId", reportId)
                        },
                    ).getOrThrow()
                val result =
                    CivicAiTriageResult(
                        categoryId = env.categoryId,
                        severity = env.severity ?: "MEDIUM",
                        confidence = env.confidence ?: 0.0,
                        summary = env.summary,
                        transitioned = env.transitioned == true,
                        provider = env.provider ?: "unknown",
                        mock = env.mock == true,
                    )
                recordAudit(
                    reportId = reportId,
                    operation = "TRIAGE",
                    provider = result.provider,
                    confidence = result.confidence,
                    latencyMs = (System.currentTimeMillis() - started).toInt(),
                )
                result
            }

        override suspend fun sendMessage(message: String): Resource<CivicAiAssistantReply> =
            safeCall {
                val env =
                    invokeAi(
                        buildJsonObject {
                            put("action", "assistant")
                            put("message", message)
                        },
                    ).getOrThrow()
                CivicAiAssistantReply(
                    reply = env.reply.orEmpty(),
                    provider = env.provider ?: "unknown",
                    mock = env.mock == true,
                )
            }

        override suspend fun summarizeReport(reportId: String): Resource<CivicAiSummary> =
            safeCall {
                val started = System.currentTimeMillis()
                val env =
                    invokeAi(
                        buildJsonObject {
                            put("action", "summarize")
                            put("reportId", reportId)
                        },
                    ).getOrThrow()
                val result =
                    CivicAiSummary(
                        summary = env.summary.orEmpty(),
                        provider = env.provider ?: "unknown",
                        mock = env.mock == true,
                    )
                recordAudit(
                    reportId = reportId,
                    operation = "SUMMARY",
                    provider = result.provider,
                    latencyMs = (System.currentTimeMillis() - started).toInt(),
                )
                result
            }
    }
