package com.smartjirani.feature.civic

import com.smartjirani.core.common.result.Resource

class KeywordMockCivicAiProvider : CivicAiProvider {
    override suspend fun triageReport(reportId: String): Resource<CivicAiTriageResult> =
        Resource.Success(
            CivicAiTriageResult(
                categoryId = "mock_category",
                severity = "LOW",
                confidence = 0.5,
                summary = "Mock triage (offline)",
                transitioned = false,
                provider = "mock",
                mock = true,
            ),
        )

    override suspend fun sendMessage(message: String): Resource<CivicAiAssistantReply> {
        val reply =
            when {
                message.contains("hello", ignoreCase = true) || message.contains("habari", ignoreCase = true) -> {
                    "Hello! I'm the civic assistant (offline mode)."
                }

                message.contains("help", ignoreCase = true) || message.contains("msaada", ignoreCase = true) -> {
                    "You can report issues like water leaks, power outages, or road damage."
                }

                else -> {
                    "I'm running in offline mode. Please connect to the internet for full assistance."
                }
            }
        return Resource.Success(
            CivicAiAssistantReply(
                reply = reply,
                provider = "mock",
                mock = true,
            ),
        )
    }

    override suspend fun summarizeReport(reportId: String): Resource<CivicAiSummary> =
        Resource.Success(
            CivicAiSummary(
                summary = "Mock summary (offline mode)",
                provider = "mock",
                mock = true,
            ),
        )
}
