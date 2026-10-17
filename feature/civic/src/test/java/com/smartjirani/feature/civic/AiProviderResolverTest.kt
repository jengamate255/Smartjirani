package com.smartjirani.feature.civic

import org.junit.Test
import kotlin.test.assertTrue

class AiProviderResolverTest {
    @Test
    fun `provider=gemini with key returns Supabase provider`() {
        val supabase = FakeSupabaseProvider()
        val mock = KeywordMockCivicAiProvider()
        val resolver = AiProviderResolver(supabase, mock, CivicAiConfig(aiProvider = "gemini", geminiKey = "k"))
        val p = resolver.resolve()
        assertTrue(p is FakeSupabaseProvider)
    }

    @Test
    fun `provider=gemini without key falls back to mock`() {
        val supabase = FakeSupabaseProvider()
        val mock = KeywordMockCivicAiProvider()
        val resolver = AiProviderResolver(supabase, mock, CivicAiConfig(aiProvider = "gemini", geminiKey = ""))
        val p = resolver.resolve()
        assertTrue(p is KeywordMockCivicAiProvider)
    }

    @Test
    fun `provider=mock always returns mock`() {
        val supabase = FakeSupabaseProvider()
        val mock = KeywordMockCivicAiProvider()
        val resolver = AiProviderResolver(supabase, mock, CivicAiConfig(aiProvider = "mock", geminiKey = "k"))
        val p = resolver.resolve()
        assertTrue(p is KeywordMockCivicAiProvider)
    }
}

// Fake for testing — implements CivicAiProvider but is distinguishable from mock
private class FakeSupabaseProvider : CivicAiProvider {
    override suspend fun triageReport(reportId: String) = throw NotImplementedError()

    override suspend fun sendMessage(message: String) = throw NotImplementedError()

    override suspend fun summarizeReport(reportId: String) = throw NotImplementedError()

    override suspend fun routeReport(reportId: String) = throw NotImplementedError()

    override suspend fun checkDuplicates(
        title: String,
        description: String,
    ) = throw NotImplementedError()

    override suspend fun transcribeAudio(
        audioBase64: String,
        filename: String,
    ) = throw NotImplementedError()

    override suspend fun analyzeReport(reportId: String) = throw NotImplementedError()
}
