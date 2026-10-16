package com.smartjirani.feature.baraza.ui

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferences
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.realtime.RealtimeEvent
import com.smartjirani.core.realtime.RealtimeService
import com.smartjirani.feature.baraza.BarazaContent
import com.smartjirani.feature.baraza.BarazaEvent
import com.smartjirani.feature.baraza.BarazaRepository
import com.smartjirani.feature.baraza.ForumThread
import com.smartjirani.feature.baraza.HarambeeProject
import com.smartjirani.feature.baraza.R
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BarazaViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var barazaRepository: BarazaRepository
    private lateinit var realtimeService: RealtimeService
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var realtimeEvents: MutableSharedFlow<RealtimeEvent>

    private val neighborhoodId = "mtaa-1"

    private val sampleEvent =
        BarazaEvent(
            id = "evt-1",
            title = "Kikao cha mtaa",
            mode = "IN_PERSON",
            councilLabel = "Baraza kuu",
            committeeLabel = "Kamati ya usalama",
            districtLabel = "Mtaa A",
            householdsLabel = "120",
            quorumLabel = "60%",
            dayLabel = "JUMAMOSI",
            dateLabel = "15",
            timeLabel = "10:00",
            venue = "Uwanja wa mtaa",
            streamNote = "",
            agenda = listOf("Usalama", "Usafi"),
            eventAt = 0L,
            attendingCount = 45,
            iAmAttending = false,
        )

    private val sampleThread =
        ForumThread(
            id = "thread-1",
            authorName = "Juma",
            tag = "Pendekezo",
            title = "Tunahitaji mabasi zaidi",
            body = "Usafiri ni tatizo",
            solved = false,
            votes = 12,
            replyCount = 3,
            createdAt = 0L,
            votedByMe = false,
        )

    private val sampleProject =
        HarambeeProject(
            id = "proj-1",
            status = "ACTIVE",
            statusLabel = "Inaendelea",
            phaseLabel = "Ujenzi",
            categoryLabel = "Miundombinu",
            title = "Ujenzi wa barabara",
            subtitle = "Barabara ya mtaa",
            iconLabel = "RB",
            photoUrl = null,
            targetKes = 500_000.0,
            raisedKes = 200_000.0,
            progressPct = 40,
            progressLabel = "40%",
            volunteerSlots = 20,
            volunteersFilled = 8,
            volunteerNote = "12 wamebaki",
            iAmVolunteering = false,
        )

    private val sampleContent =
        BarazaContent(
            events = listOf(sampleEvent),
            threads = listOf(sampleThread),
            projects = listOf(sampleProject),
        )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        barazaRepository = mockk()
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

    private fun createViewModel(): BarazaViewModel = BarazaViewModel(barazaRepository, realtimeService, userPreferencesRepository)

    @Test
    fun `loads content on init when neighborhood is set`() =
        runTest {
            coEvery { barazaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)

            val vm = createViewModel()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(false, state.isLoading)
            assertEquals(neighborhoodId, state.neighborhoodId)
            assertEquals(1, state.content.events.size)
            assertEquals(1, state.content.threads.size)
            assertEquals(1, state.content.projects.size)
        }

    @Test
    fun `rsvp toggle updates optimistically and shows toast on success`() =
        runTest {
            coEvery { barazaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { barazaRepository.toggleRsvp("evt-1", true) } returns Resource.Success(Unit)

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onRsvpToggled("evt-1", attending = true)
            advanceUntilIdle()

            val state = vm.uiState.value
            val updatedEvent = state.content.events.first()
            assertTrue(updatedEvent.iAmAttending)
            assertEquals(46, updatedEvent.attendingCount)
            assertEquals(R.string.baraza_toast_rsvp_on, state.toast?.messageId)
        }

    @Test
    fun `ask question increments queued questions and shows toast`() =
        runTest {
            coEvery { barazaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onAskQuestionClicked("evt-1")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(
                1,
                state.content.events
                    .first()
                    .questionsQueued,
            )
            assertEquals(R.string.baraza_toast_question_queued, state.toast?.messageId)
        }

    @Test
    fun `rsvp toggle rolls back on error`() =
        runTest {
            coEvery { barazaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { barazaRepository.toggleRsvp("evt-1", true) } returns Resource.Error("Network error")

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onRsvpToggled("evt-1", attending = true)
            advanceUntilIdle()

            val state = vm.uiState.value
            val rolledBack = state.content.events.first()
            assertEquals(false, rolledBack.iAmAttending)
            assertEquals(45, rolledBack.attendingCount)
            assertEquals("Network error", state.error)
        }

    @Test
    fun `vote toggle updates optimistically and rolls back on error`() =
        runTest {
            coEvery { barazaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { barazaRepository.toggleThreadVote("thread-1", true) } returns Resource.Error("Fail")

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onVoteToggled("thread-1", voted = true)
            advanceUntilIdle()

            val state = vm.uiState.value
            val rolledBack = state.content.threads.first()
            assertEquals(false, rolledBack.votedByMe)
            assertEquals(12, rolledBack.votes)
        }

    @Test
    fun `volunteer toggle shows toast on success`() =
        runTest {
            coEvery { barazaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { barazaRepository.toggleVolunteer("proj-1", true) } returns Resource.Success(Unit)

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onVolunteerToggled("proj-1", joined = true)
            advanceUntilIdle()

            val state = vm.uiState.value
            val updated = state.content.projects.first()
            assertTrue(updated.iAmVolunteering)
            assertEquals(9, updated.volunteersFilled)
            assertEquals(R.string.baraza_toast_volunteer_on, state.toast?.messageId)
        }

    @Test
    fun `volunteer toggle rolls back on error`() =
        runTest {
            coEvery { barazaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { barazaRepository.toggleVolunteer("proj-1", true) } returns Resource.Error("Fail")

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onVolunteerToggled("proj-1", joined = true)
            advanceUntilIdle()

            val state = vm.uiState.value
            val rolledBack = state.content.projects.first()
            assertEquals(false, rolledBack.iAmVolunteering)
            assertEquals(8, rolledBack.volunteersFilled)
        }

    @Test
    fun `realtime event triggers refresh`() =
        runTest {
            coEvery { barazaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)

            val vm = createViewModel()
            advanceUntilIdle()

            val updatedContent = sampleContent.copy(events = emptyList())
            coEvery { barazaRepository.getContent(neighborhoodId) } returns Resource.Success(updatedContent)

            realtimeEvents.emit(RealtimeEvent.BarazaEventChanged(neighborhoodId))
            advanceUntilIdle()

            assertEquals(0, vm.uiState.value.content.events.size)
        }

    @Test
    fun `dismissError clears error`() =
        runTest {
            coEvery { barazaRepository.getContent(neighborhoodId) } returns Resource.Error("Oops")

            val vm = createViewModel()
            advanceUntilIdle()

            assertEquals("Oops", vm.uiState.value.error)
            vm.dismissError()
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `dismissToast clears toast`() =
        runTest {
            coEvery { barazaRepository.getContent(neighborhoodId) } returns Resource.Success(sampleContent)
            coEvery { barazaRepository.toggleRsvp("evt-1", true) } returns Resource.Success(Unit)

            val vm = createViewModel()
            advanceUntilIdle()

            vm.onRsvpToggled("evt-1", true)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.toast != null)

            vm.dismissToast()
            assertNull(vm.uiState.value.toast)
        }
}
