package com.smartjirani.feature.civic.ui

import com.smartjirani.core.audio.AudioRecorder
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.location.LocationRepository
import com.smartjirani.feature.civic.CivicAiAssistantReply
import com.smartjirani.feature.civic.CivicAiDedupResult
import com.smartjirani.feature.civic.CivicAiProvider
import com.smartjirani.feature.civic.CivicAiTriageResult
import com.smartjirani.feature.civic.CivicRepository
import com.smartjirani.feature.civic.CivicSubmitResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CivicAssistantViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var civicAiProvider: CivicAiProvider

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        civicAiProvider = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `send appends user message then ai reply`() =
        runTest {
            coEvery { civicAiProvider.sendMessage("Habari") } returns
                Resource.Success(
                    CivicAiAssistantReply(reply = "Karibu! Mimi ni Msaidizi wa AI.", provider = "keyword-mock", mock = true),
                )

            val vm = CivicAssistantViewModel(civicAiProvider)
            advanceUntilIdle()

            vm.onInputChange("Habari")
            vm.send()
            advanceUntilIdle()

            val messages = vm.uiState.value.messages
            assertEquals(3, messages.size)
            assertEquals("Habari", messages[1].text)
            assertTrue(messages[1].isUser)
            assertEquals("Karibu! Mimi ni Msaidizi wa AI.", messages[2].text)
            assertFalse(messages[2].isUser)
            assertTrue(messages[2].isMock)
            assertFalse(vm.uiState.value.isSending)
        }

    @Test
    fun `send failure sets error and keeps messages`() =
        runTest {
            coEvery { civicAiProvider.sendMessage("Ripoti yangu iko wapi") } returns Resource.Error("assistant_rate_limited")

            val vm = CivicAssistantViewModel(civicAiProvider)
            advanceUntilIdle()

            vm.onInputChange("Ripoti yangu iko wapi")
            vm.send()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(2, state.messages.size)
            assertEquals("assistant_rate_limited", state.error)
            assertFalse(state.isSending)
        }

    @Test
    fun `send with blank input is ignored`() =
        runTest {
            val vm = CivicAssistantViewModel(civicAiProvider)
            advanceUntilIdle()

            vm.onInputChange("   ")
            vm.send()
            advanceUntilIdle()

            assertEquals(1, vm.uiState.value.messages.size)
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `dismissError clears error`() =
        runTest {
            coEvery { civicAiProvider.sendMessage(any()) } returns Resource.Error("forbidden")

            val vm = CivicAssistantViewModel(civicAiProvider)
            advanceUntilIdle()

            vm.onInputChange("hello")
            vm.send()
            advanceUntilIdle()

            assertEquals("forbidden", vm.uiState.value.error)
            vm.dismissError()
            assertNull(vm.uiState.value.error)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CivicNewReportTriageTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submit triggers triage and records transition`() =
        runTest {
            val civicRepository = mockk<CivicRepository>()
            val civicAiProvider = mockk<CivicAiProvider>()
            val locationRepository = mockk<LocationRepository>()

            coEvery { civicRepository.getCategories() } returns Resource.Success(emptyList())
            coEvery { civicAiProvider.checkDuplicates(any(), any()) } returns
                Resource.Success(
                    CivicAiDedupResult(matches = emptyList(), provider = "keyword-mock", mock = true),
                )
            coEvery {
                civicRepository.submitReport(any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns
                Resource.Success(
                    CivicSubmitResult(
                        reportId = "r-1",
                        publicReference = "ilala-mc-2026-00099",
                        merged = false,
                        canonicalReference = null,
                    ),
                )
            coEvery { civicAiProvider.triageReport("r-1") } returns
                Resource.Success(
                    CivicAiTriageResult(
                        categoryId = "cat-1",
                        severity = "HIGH",
                        confidence = 0.35,
                        summary = "Muhtasari",
                        transitioned = true,
                        provider = "keyword-mock",
                        mock = true,
                    ),
                )

            val audioRecorder = mockk<AudioRecorder>()
            val vm = CivicNewReportViewModel(civicRepository, civicAiProvider, locationRepository, audioRecorder)
            advanceUntilIdle()

            vm.submit(
                categoryId = "cat-1",
                title = "Barabara yenye mashimo",
                description = "Barabara ya Mtaa A ina mashomo makubwa yanayotishia usalama.",
                locationDescription = null,
                severity = "HIGH",
                isAnonymous = false,
            )
            advanceUntilIdle()

            assertTrue(vm.uiState.value.result != null)
            assertTrue(vm.uiState.value.aiTriaged)
        }

    @Test
    fun `triage failure does not block submit result`() =
        runTest {
            val civicRepository = mockk<CivicRepository>()
            val civicAiProvider = mockk<CivicAiProvider>()
            val locationRepository = mockk<LocationRepository>()

            coEvery { civicRepository.getCategories() } returns Resource.Success(emptyList())
            coEvery { civicAiProvider.checkDuplicates(any(), any()) } returns
                Resource.Success(
                    CivicAiDedupResult(matches = emptyList(), provider = "keyword-mock", mock = true),
                )
            coEvery {
                civicRepository.submitReport(any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns
                Resource.Success(
                    CivicSubmitResult(
                        reportId = "r-2",
                        publicReference = "ilala-mc-2026-00100",
                        merged = false,
                        canonicalReference = null,
                    ),
                )
            coEvery { civicAiProvider.triageReport("r-2") } returns Resource.Error("gemini_http_500")

            val audioRecorder = mockk<AudioRecorder>()
            val vm = CivicNewReportViewModel(civicRepository, civicAiProvider, locationRepository, audioRecorder)
            advanceUntilIdle()

            vm.submit(
                categoryId = "cat-1",
                title = "Taa zilizozima",
                description = "Taa za mtaa hazifanyi kazi kwa wiki mbili mfululizo.",
                locationDescription = null,
                severity = "MEDIUM",
                isAnonymous = false,
            )
            advanceUntilIdle()

            assertTrue(vm.uiState.value.result != null)
            assertFalse(vm.uiState.value.aiTriaged)
            assertNull(vm.uiState.value.error)
        }
}
