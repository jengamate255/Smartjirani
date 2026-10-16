package com.smartjirani.feature.civic.ui

import com.smartjirani.core.common.result.Resource
import com.smartjirani.feature.civic.CivicAdminCategory
import com.smartjirani.feature.civic.CivicAdminData
import com.smartjirani.feature.civic.CivicAdminUnitType
import com.smartjirani.feature.civic.CivicDirectoryUser
import com.smartjirani.feature.civic.CivicRepository
import com.smartjirani.feature.civic.CivicRole
import com.smartjirani.feature.civic.CivicRoleRow
import com.smartjirani.feature.civic.CivicSlaRule
import io.mockk.coEvery
import io.mockk.coVerify
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
class CivicAdminViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var civicRepository: CivicRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        civicRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun adminData(
        roles: List<CivicRoleRow> = emptyList(),
        unitTypes: List<CivicAdminUnitType> = emptyList(),
        slaRules: List<CivicSlaRule> = emptyList(),
        categories: List<CivicAdminCategory> = emptyList(),
    ) = CivicAdminData(
        unitTypes = unitTypes,
        units = emptyList(),
        slaRules = slaRules,
        roles = roles,
        categories = categories,
    )

    @Test
    fun `refresh success populates admin data`() =
        runTest {
            val data =
                adminData(
                    unitTypes = listOf(CivicAdminUnitType("t-1", "Mtaa", level = 3, isDepartment = false)),
                    slaRules =
                        listOf(
                            CivicSlaRule("s-1", responseHours = 24.0, resolutionHours = 72.0),
                        ),
                    categories =
                        listOf(
                            CivicAdminCategory("c-1", nameEn = "Roads", nameSw = "Barabara"),
                        ),
                    roles =
                        listOf(
                            CivicRoleRow(
                                id = "r-1",
                                userId = "u-1",
                                role = CivicRole.ROLE_MTAA_OFFICER,
                                orgUnitId = "unit-1",
                                unitName = "Mtaa Mwenge",
                                displayName = "Amina",
                            ),
                        ),
                )
            coEvery { civicRepository.getAdminData() } returns Resource.Success(data)

            val vm = CivicAdminViewModel(civicRepository)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(1, state.data.unitTypes.size)
            assertEquals(1, state.data.slaRules.size)
            assertEquals(
                "Barabara",
                state.data.categories
                    .first()
                    .nameSw,
            )
            assertEquals(
                "Amina",
                state.data.roles
                    .first()
                    .displayName,
            )
        }

    @Test
    fun `refresh failure sets error`() =
        runTest {
            coEvery { civicRepository.getAdminData() } returns Resource.Error("not_admin")

            val vm = CivicAdminViewModel(civicRepository)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals("not_admin", state.error)
        }

    @Test
    fun `assignRole success shows toast and reloads data`() =
        runTest {
            coEvery { civicRepository.getAdminData() } returnsMany
                listOf(
                    Resource.Success(adminData()),
                    Resource.Success(
                        adminData(
                            roles =
                                listOf(
                                    CivicRoleRow(
                                        id = "r-9",
                                        userId = "u-2",
                                        role = CivicRole.ROLE_MJUMBE,
                                        orgUnitId = "unit-1",
                                        unitName = "Mtaa Mwenge",
                                    ),
                                ),
                        ),
                    ),
                )
            coEvery {
                civicRepository.assignRole("u-2", CivicRole.ROLE_MJUMBE, "unit-1")
            } returns Resource.Success(Unit)

            val vm = CivicAdminViewModel(civicRepository)
            advanceUntilIdle()

            vm.assignRole("u-2", CivicRole.ROLE_MJUMBE, "unit-1")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals("Wadhifa umepewa", state.toast)
            assertFalse(state.isMutating)
            assertEquals(1, state.data.roles.size)
            coVerify(exactly = 2) { civicRepository.getAdminData() }
        }

    @Test
    fun `assignRole failure sets error without toast`() =
        runTest {
            coEvery { civicRepository.getAdminData() } returns Resource.Success(adminData())
            coEvery {
                civicRepository.assignRole(any(), any(), any())
            } returns Resource.Error("forbidden_role")

            val vm = CivicAdminViewModel(civicRepository)
            advanceUntilIdle()

            vm.assignRole("u-2", CivicRole.ROLE_SYSTEM_ADMIN, "unit-1")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals("forbidden_role", state.error)
            assertNull(state.toast)
            assertFalse(state.isMutating)
        }

    @Test
    fun `revokeRole success shows toast and reloads`() =
        runTest {
            coEvery { civicRepository.getAdminData() } returns Resource.Success(adminData())
            coEvery { civicRepository.revokeRole("r-1") } returns Resource.Success(Unit)

            val vm = CivicAdminViewModel(civicRepository)
            advanceUntilIdle()

            vm.revokeRole("r-1")
            advanceUntilIdle()

            assertEquals("Wadhifa umeondolewa", vm.uiState.value.toast)
            coVerify(exactly = 2) { civicRepository.getAdminData() }
        }

    @Test
    fun `searchDirectory returns results via callback`() =
        runTest {
            coEvery { civicRepository.getAdminData() } returns Resource.Success(adminData())
            coEvery { civicRepository.searchDirectory("amina") } returns
                Resource.Success(
                    listOf(CivicDirectoryUser(id = "u-1", displayName = "Amina", email = "a@x.tz")),
                )

            val vm = CivicAdminViewModel(civicRepository)
            advanceUntilIdle()

            var found: List<CivicDirectoryUser> = emptyList()
            vm.searchDirectory("amina") { found = it }
            advanceUntilIdle()

            assertEquals(1, found.size)
            assertEquals("Amina", found.first().displayName)
        }

    @Test
    fun `searchDirectory failure returns empty list`() =
        runTest {
            coEvery { civicRepository.getAdminData() } returns Resource.Success(adminData())
            coEvery { civicRepository.searchDirectory(any()) } returns Resource.Error("boom")

            val vm = CivicAdminViewModel(civicRepository)
            advanceUntilIdle()

            var found: List<CivicDirectoryUser>? = null
            vm.searchDirectory("amina") { found = it }
            advanceUntilIdle()

            assertTrue(found?.isEmpty() == true)
        }

    @Test
    fun `createSlaRule success shows toast and reloads`() =
        runTest {
            coEvery { civicRepository.getAdminData() } returns Resource.Success(adminData())
            coEvery {
                civicRepository.createSlaRule(null, "HIGH", null, 12.0, 48.0)
            } returns Resource.Success(Unit)

            val vm = CivicAdminViewModel(civicRepository)
            advanceUntilIdle()

            vm.createSlaRule(null, "HIGH", null, 12.0, 48.0)
            advanceUntilIdle()

            assertEquals("Kanuni ya muda imehifadhiwa", vm.uiState.value.toast)
            assertFalse(vm.uiState.value.isMutating)
            coVerify(exactly = 2) { civicRepository.getAdminData() }
        }
}
