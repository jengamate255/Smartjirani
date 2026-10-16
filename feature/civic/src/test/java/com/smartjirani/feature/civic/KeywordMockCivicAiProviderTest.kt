package com.smartjirani.feature.civic

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeywordMockCivicAiProviderTest {
    private val mock = KeywordMockCivicAiProvider()

    @Test
    fun `triageReport returns mock result`() =
        runTest {
            val result = mock.triageReport("r1")
            assertTrue(result is com.smartjirani.core.common.result.Resource.Success)
            val data = (result as com.smartjirani.core.common.result.Resource.Success).data
            assertEquals("mock", data.provider)
            assertTrue(data.mock)
        }

    @Test
    fun `sendMessage with hello returns greeting`() =
        runTest {
            val result = mock.sendMessage("hello")
            assertTrue(result is com.smartjirani.core.common.result.Resource.Success)
            val data = (result as com.smartjirani.core.common.result.Resource.Success).data
            assertTrue(data.reply.contains("Hello", ignoreCase = true))
            assertEquals("mock", data.provider)
        }

    @Test
    fun `sendMessage with habari returns greeting`() =
        runTest {
            val result = mock.sendMessage("habari")
            assertTrue(result is com.smartjirani.core.common.result.Resource.Success)
            val data = (result as com.smartjirani.core.common.result.Resource.Success).data
            assertTrue(data.reply.contains("Hello", ignoreCase = true))
        }

    @Test
    fun `summarizeReport returns mock summary`() =
        runTest {
            val result = mock.summarizeReport("r1")
            assertTrue(result is com.smartjirani.core.common.result.Resource.Success)
            val data = (result as com.smartjirani.core.common.result.Resource.Success).data
            assertEquals("mock", data.provider)
            assertTrue(data.mock)
        }
}
