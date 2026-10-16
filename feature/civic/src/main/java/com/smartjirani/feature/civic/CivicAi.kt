package com.smartjirani.feature.civic

import com.smartjirani.core.common.result.Resource
import kotlinx.serialization.Serializable

data class CivicAiTriageResult(
    val categoryId: String?,
    val severity: String,
    val confidence: Double,
    val summary: String?,
    val transitioned: Boolean,
    val provider: String,
    val mock: Boolean,
)

data class CivicAiAssistantReply(
    val reply: String,
    val provider: String,
    val mock: Boolean,
)

data class CivicAiSummary(
    val summary: String,
    val provider: String,
    val mock: Boolean,
)

data class CivicAiRoutingSuggestion(
    val suggestedDepartment: String,
    val routingReason: String,
    val priority: String,
    val slaHours: Int,
    val provider: String,
    val mock: Boolean,
)

@Serializable
data class CivicAiDuplicateMatch(
    val reportId: String,
    val publicReference: String,
    val title: String,
    val similarity: Double,
    val status: String,
    val createdAt: String,
)

data class CivicAiDedupResult(
    val matches: List<CivicAiDuplicateMatch>,
    val provider: String,
    val mock: Boolean,
)

data class CivicAiTranscriptionResult(
    val transcript: String,
    val provider: String,
    val mock: Boolean,
)

data class CivicAiAnalysisResult(
    val affectedArea: String,
    val populationImpact: String,
    val suggestedActions: List<String>,
    val urgencyFactors: List<String>,
    val infrastructureType: String,
    val severityEstimate: String,
    val provider: String,
    val mock: Boolean,
)

interface CivicAiProvider {
    suspend fun triageReport(reportId: String): Resource<CivicAiTriageResult>

    suspend fun sendMessage(message: String): Resource<CivicAiAssistantReply>

    suspend fun summarizeReport(reportId: String): Resource<CivicAiSummary>

    suspend fun routeReport(reportId: String): Resource<CivicAiRoutingSuggestion>

    suspend fun checkDuplicates(
        title: String,
        description: String,
    ): Resource<CivicAiDedupResult>

    suspend fun transcribeAudio(
        audioBase64: String,
        filename: String,
    ): Resource<CivicAiTranscriptionResult>

    suspend fun analyzeReport(reportId: String): Resource<CivicAiAnalysisResult>
}
