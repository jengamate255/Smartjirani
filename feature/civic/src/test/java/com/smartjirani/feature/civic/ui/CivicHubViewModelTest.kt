package com.smartjirani.feature.civic.ui

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferences
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.feature.civic.CivicAnalytics
import com.smartjirani.feature.civic.CivicAnnouncement
import com.smartjirani.feature.civic.CivicQueueData
import com.smartjirani.feature.civic.CivicReport
import com.smartjirani.feature.civic.CivicRepository
import com.smartjirani.feature.civic.CivicRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class CivicHubViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var civicRepository: CivicRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        civicRepository = mockk()
        userPreferencesRepository =
            mockk {
                every { userPreferences } returns
                    MutableStateFlow(
                        UserPreferences(selectedNeighborhoodName = "Kariakoo"),
                    )
            }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun report(
        id: String,
        status: String,
        reportCount: Int = 1,
    ) = CivicReport(
        id = id,
        publicReference = "ilala-mc-2026-00$reportCount",
        title = "Tatua la $id",
        description = "Maelezo ya tatuo la $id.",
        status = status,
        severity = "MEDIUM",
        priority = "NORMAL",
        reportCount = reportCount,
        createdAt = "2026-10-01T08:00:00Z",
        updatedAt = "2026-10-02T09:00:00Z",
    )

    @Test
    fun `refresh loads queue announcements and location label`() =
        runTest {
            val queueData =
                CivicQueueData(
                    reports = listOf(report("r-1", "NEW"), report("r-2", "RESOLVED")),
                    roles = listOf(CivicRole("MTAA_OFFICER", "org-1")),
                    categories = emptyList(),
                    units = emptyList(),
                )
            coEvery { civicRepository.getQueue() } returns Resource.Success(queueData)
            coEvery { civicRepository.getAnnouncements() } returns
                Resource.Success(
                    listOf(
                        CivicAnnouncement(
                            id = "a-1",
                            organizationId = "org-1",
                            title = "Uchafu wa mazingira",
                            body = "Tafadhali weka taka mahali pake kuanzia Jumatatu.",
                            category = "HEALTH",
                            pinned = true,
                            publishedAt = "2026-10-03T07:30:00Z",
                        ),
                    ),
                )
            coEvery { civicRepository.getAnalytics() } returns Resource.Success(CivicAnalytics(total = 2, slaMet = 1))

            val vm = CivicHubViewModel(civicRepository, userPreferencesRepository)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals("Kariakoo", state.locationLabel)
            assertEquals(2, state.data.reports.size)
            assertEquals(1, state.announcements.size)
            assertTrue(state.data.isOfficial)
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(1, state.active)
            assertEquals(0, state.overdue)
        }

    @Test
    fun `publish success shows toast and reloads announcements`() =
        runTest {
            coEvery { civicRepository.getQueue() } returns
                Resource.Success(
                    CivicQueueData(emptyList(), emptyList(), emptyList(), emptyList()),
                )
            coEvery { civicRepository.getAnnouncements() } returns Resource.Success(emptyList())
            coEvery {
                civicRepository.publishAnnouncement("Kichwa", "Maelezo marefu ya tangazo.", "NOTICE", false)
            } returns Resource.Success(Unit)
            coEvery { civicRepository.getAnalytics() } returns Resource.Success(CivicAnalytics())

            val vm = CivicHubViewModel(civicRepository, userPreferencesRepository)
            advanceUntilIdle()

            vm.publishAnnouncement("Kichwa", "Maelezo marefu ya tangazo.", "NOTICE", false)
            advanceUntilIdle()

            assertEquals("Tangazo limetangazwa kwa wakazi", vm.uiState.value.toast)
            assertFalse(vm.uiState.value.isPublishing)
            // Initial refresh + reload after publish.
            coVerify(exactly = 2) { civicRepository.getAnnouncements() }
        }

    @Test
    fun `publish failure sets error and shows no toast`() =
        runTest {
            coEvery { civicRepository.getQueue() } returns
                Resource.Success(
                    CivicQueueData(emptyList(), emptyList(), emptyList(), emptyList()),
                )
            coEvery { civicRepository.getAnnouncements() } returns Resource.Success(emptyList())
            coEvery {
                civicRepository.publishAnnouncement(any(), any(), any(), any())
            } returns Resource.Error("forbidden")
            coEvery { civicRepository.getAnalytics() } returns Resource.Success(CivicAnalytics())

            val vm = CivicHubViewModel(civicRepository, userPreferencesRepository)
            advanceUntilIdle()

            vm.publishAnnouncement("Kichwa", "Maelezo marefu ya tangazo.", "NOTICE", true)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals("forbidden", state.error)
            assertNull(state.toast)
            assertFalse(state.isPublishing)
        }

    @Test
    fun `setStatusFilter filters visible reports`() =
        runTest {
            coEvery { civicRepository.getQueue() } returns
                Resource.Success(
                    CivicQueueData(
                        reports =
                            listOf(
                                report("r-1", "NEW"),
                                report("r-2", "NEW"),
                                report("r-3", "RESOLVED", reportCount = 3),
                            ),
                        roles = emptyList(),
                        categories = emptyList(),
                        units = emptyList(),
                    ),
                )
            coEvery { civicRepository.getAnnouncements() } returns Resource.Success(emptyList())
            coEvery { civicRepository.getAnalytics() } returns Resource.Success(CivicAnalytics())

            val vm = CivicHubViewModel(civicRepository, userPreferencesRepository)
            advanceUntilIdle()

            assertEquals(3, vm.uiState.value.visibleReports.size)
            assertEquals(3, vm.uiState.value.total)

            vm.setStatusFilter("NEW")
            assertEquals(2, vm.uiState.value.visibleReports.size)
            assertTrue(
                vm.uiState.value.visibleReports
                    .all { it.status == "NEW" },
            )

            vm.setStatusFilter(null)
            assertEquals(3, vm.uiState.value.visibleReports.size)
        }

    @Test
    fun `analytics success populates uiState analytics`() =
        runTest {
            coEvery { civicRepository.getQueue() } returns
                Resource.Success(
                    CivicQueueData(emptyList(), emptyList(), emptyList(), emptyList()),
                )
            coEvery { civicRepository.getAnnouncements() } returns Resource.Success(emptyList())
            coEvery { civicRepository.getAnalytics() } returns
                Resource.Success(
                    CivicAnalytics(
                        total = 5,
                        active = 2,
                        overdue = 1,
                        escalated = 1,
                        duplicatesFolded = 3,
                        avgResolutionHours = 12.4,
                        slaMet = 3,
                        slaMissed = 1,
                    ),
                )

            val vm = CivicHubViewModel(civicRepository, userPreferencesRepository)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(5, state.analytics?.total)
            assertEquals(1, state.analytics?.escalated)
            assertEquals(12.4, state.analytics?.avgResolutionHours)
            assertEquals(3, state.analytics?.slaMet)
            assertEquals(0.75, state.analytics?.slaRate)
        }

    @Test
    fun `analytics failure keeps analytics null`() =
        runTest {
            coEvery { civicRepository.getQueue() } returns
                Resource.Success(
                    CivicQueueData(emptyList(), emptyList(), emptyList(), emptyList()),
                )
            coEvery { civicRepository.getAnnouncements() } returns Resource.Success(emptyList())
            coEvery { civicRepository.getAnalytics() } returns Resource.Error("boom")

            val vm = CivicHubViewModel(civicRepository, userPreferencesRepository)
            advanceUntilIdle()

            assertNull(vm.uiState.value.analytics)
            assertNull(vm.uiState.value.error)
        }
}
