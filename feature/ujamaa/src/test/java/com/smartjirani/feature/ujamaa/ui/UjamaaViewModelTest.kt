package com.smartjirani.feature.ujamaa.ui

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferences
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.realtime.RealtimeEvent
import com.smartjirani.core.realtime.RealtimeService
import com.smartjirani.feature.ujamaa.ContributionResult
import com.smartjirani.feature.ujamaa.HarambeeFund
import com.smartjirani.feature.ujamaa.R
import com.smartjirani.feature.ujamaa.SharedResource
import com.smartjirani.feature.ujamaa.UjamaaContent
import com.smartjirani.feature.ujamaa.UjamaaFilter
import com.smartjirani.feature.ujamaa.UjamaaRepository
import com.smartjirani.feature.ujamaa.UjamaaStats
import com.smartjirani.feature.ujamaa.UrgentNeed
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
class UjamaaViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var ujamaaRepository: UjamaaRepository
    private lateinit var realtimeService: RealtimeService
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var realtimeEvents: MutableSharedFlow<RealtimeEvent>

    private val neighborhoodId = "mtaa-1"

    private val sampleNeed =
        UrgentNeed(
            id = "need-1",
            title = "Mama anahitaji msaada",
            body = "Chakula kimeisha",
            requesterLabel = "Mama Amina",
            requesterMeta = "Nyumba ya 4",
            distanceLabel = "200m",
            statusLabel = "Wazi",
            isOpen = true,
            iOfferedHelp = false,
        )

    private val sampleResource =
        SharedResource(
            id = "res-1",
            category = "TOOL",
            tagLabel = "Zana",
            title = "Drilli",
            description = "Drilli ya umeme",
            imageUrl = null,
            distanceLabel = "100m",
            historyLabel = "Imeazimwa mara 5",
            availabilityNote = "Bure kukopa",
            depositNote = "Deposit KSh 500",
            ctaLabel = "Omba",
            termsLabel = "Siku 3",
            routeLabel = null,
            seatLabel = null,
            departureLabel = null,
            isAvailable = true,
            iRequested = false,
        )

    private val sampleFund =
        HarambeeFund(
            id = "fund-1",
            name = "Mfuko wa mtaa",
            subtitle = "Kujenga zahanati",
            auditLabel = "Kaguzi na CAG",
            targetKes = 1_000_000.0,
            raisedKes = 350_000.0,
            progressPct = 35,
            purposeLabel = "Ujenzi wa zahanati ya mtaa",
            remainingLabel = "KSh 650,000 imebaki",
        )

    private val sampleContent =
        UjamaaContent(
            stats = UjamaaStats(neighborsHelped = 42, activeBorrows = 7),
            needs = listOf(sampleNeed),
            resources = listOf(sampleResource),
            fund = sampleFund,
        )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        ujamaaRepository = mockk()
        realtimeService = mockk()
        userPreferencesRepository = mockk()
        realtimeEvents = MutableSharedFlow(extraBufferCapacity = 64)

        every { realtimeService.events } returns realtimeEvents
        every { realtimeService.isConnected } returns MutableStateFlow(false)
        coEvery { realtimeService.subscribeToNeighborhood(any()) } returns Unit
        coEvery { realtimeService.unsubscribeFromNeighborhood(any()) } returns Unit
        every { userPreferencesRepository.userPreferences } returns
            flowOf(
                UserPreferences(selectedNeighborhoodId = neighborhoodId),
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): UjamaaViewModel = UjamaaViewModel(ujamaaRepository, realtimeService, userPreferencesRepository)

    @Test
    fun `loads content on init when neighborhood is set`() =
        runTest {
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)

            val vm = createViewModel()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals(neighborhoodId, state.neighborhoodId)
            assertEquals(1, state.content.needs.size)
            assertEquals(1, state.content.resources.size)
            assertEquals(42, state.content.stats.neighborsHelped)
        }

    @Test
    fun `offer help updates optimistically and shows toast`() =
        runTest {
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { ujamaaRepository.offerHelp("need-1") } returns Resource.Success(Unit)

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onOfferHelp("need-1", offer = true)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(
                state.content.needs
                    .first()
                    .iOfferedHelp,
            )
            assertEquals(R.string.ujamaa_toast_help_on, state.toast?.messageId)
        }

    @Test
    fun `offer help rolls back on error`() =
        runTest {
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { ujamaaRepository.offerHelp("need-1") } returns Resource.Error("Network error")

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onOfferHelp("need-1", offer = true)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(
                state.content.needs
                    .first()
                    .iOfferedHelp,
            )
            assertEquals("Network error", state.error)
        }

    @Test
    fun `withdraw help shows correct toast`() =
        runTest {
            val contentWithHelp =
                sampleContent.copy(
                    needs = listOf(sampleNeed.copy(iOfferedHelp = true)),
                )
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(contentWithHelp)
            coEvery { ujamaaRepository.withdrawHelp("need-1") } returns Resource.Success(Unit)

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onOfferHelp("need-1", offer = false)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(
                state.content.needs
                    .first()
                    .iOfferedHelp,
            )
            assertEquals(R.string.ujamaa_toast_help_off, state.toast?.messageId)
        }

    @Test
    fun `request resource shows toast on success`() =
        runTest {
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { ujamaaRepository.requestResource("res-1") } returns Resource.Success(Unit)

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onRequestResource("res-1")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(
                state.content.resources
                    .first()
                    .iRequested,
            )
            assertEquals(R.string.ujamaa_toast_resource_requested, state.toast?.messageId)
        }

    @Test
    fun `request resource shows error on failure`() =
        runTest {
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { ujamaaRepository.requestResource("res-1") } returns Resource.Error("Haipatikani")

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onRequestResource("res-1")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(
                state.content.resources
                    .first()
                    .iRequested,
            )
            assertEquals("Haipatikani", state.error)
        }

    @Test
    fun `filter change updates selected filter`() =
        runTest {
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onFilterSelected(UjamaaFilter.TOOLS)
            assertEquals(UjamaaFilter.TOOLS, vm.uiState.value.selectedFilter)

            vm.onFilterSelected(UjamaaFilter.BOOKS)
            assertEquals(UjamaaFilter.BOOKS, vm.uiState.value.selectedFilter)
        }

    @Test
    fun `offer and ask launchers show toasts`() =
        runTest {
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onOfferItemClicked()
            assertEquals(
                R.string.ujamaa_toast_offer_opening,
                vm.uiState.value.toast
                    ?.messageId,
            )

            vm.onAskHelpClicked()
            assertEquals(
                R.string.ujamaa_toast_ask_opening,
                vm.uiState.value.toast
                    ?.messageId,
            )
        }

    @Test
    fun `visible resources filters by category`() =
        runTest {
            val toolResource = sampleResource.copy(id = "res-tool", category = "TOOL")
            val bookResource = sampleResource.copy(id = "res-book", category = "BOOK")
            val content = sampleContent.copy(resources = listOf(toolResource, bookResource))
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(content)

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onFilterSelected(UjamaaFilter.TOOLS)
            val toolsVisible = vm.uiState.value.visibleResources
            assertEquals(1, toolsVisible.size)
            assertEquals("res-tool", toolsVisible.first().id)

            vm.onFilterSelected(UjamaaFilter.BOOKS)
            val booksVisible = vm.uiState.value.visibleResources
            assertEquals(1, booksVisible.size)
            assertEquals("res-book", booksVisible.first().id)
        }

    @Test
    fun `contribute with settled result shows toast`() =
        runTest {
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { ujamaaRepository.contribute("fund-1", 200.0) } returns
                Resource.Success(
                    ContributionResult(amountKes = 200.0, reference = "REF-1", checkoutUrl = null),
                )

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onContribute(200.0)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isContributing)
            assertEquals(R.string.ujamaa_toast_contribution_settled, state.toast?.messageId)
            assertEquals(listOf(200L), state.toast?.args)
        }

    @Test
    fun `contribute error clears contributing state`() =
        runTest {
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { ujamaaRepository.contribute("fund-1", 100.0) } returns Resource.Error("STK error")

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onContribute(100.0)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isContributing)
            assertEquals("STK error", state.error)
        }

    @Test
    fun `realtime UrgentNeedChanged triggers refresh`() =
        runTest {
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)

            val vm = createViewModel()
            advanceUntilIdle()

            val updatedContent = sampleContent.copy(needs = emptyList())
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(updatedContent)

            realtimeEvents.emit(RealtimeEvent.UrgentNeedChanged(neighborhoodId))
            advanceUntilIdle()

            assertEquals(0, vm.uiState.value.content.needs.size)
        }

    @Test
    fun `dismissError clears error`() =
        runTest {
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Error("Fail")

            val vm = createViewModel()
            advanceUntilIdle()

            assertEquals("Fail", vm.uiState.value.error)
            vm.dismissError()
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `dismissToast clears toast`() =
        runTest {
            coEvery { ujamaaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { ujamaaRepository.offerHelp("need-1") } returns Resource.Success(Unit)

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onOfferHelp("need-1", true)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.toast != null)

            vm.dismissToast()
            assertNull(vm.uiState.value.toast)
        }
}
