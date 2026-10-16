package com.smartjirani.feature.civic

class AiProviderResolver(
    private val supabase: CivicAiProvider,
    private val mock: KeywordMockCivicAiProvider,
    private val cfg: CivicAiConfig,
) {
    fun resolve(): CivicAiProvider =
        when {
            cfg.aiProvider == "mock" -> mock
            cfg.aiProvider == "gemini" && cfg.geminiKey.isNotBlank() -> supabase
            else -> mock
        }
}
