package com.smartjirani.feature.posts.ui

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferences
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.feature.posts.Post
import com.smartjirani.feature.posts.PostsRepository
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
class PostsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var postsRepository: PostsRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        postsRepository = mockk()
        userPreferencesRepository =
            mockk {
                every { userPreferences } returns
                    MutableStateFlow(
                        UserPreferences(selectedNeighborhoodId = "nb-1"),
                    )
            }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun post(id: String) =
        Post(
            id = id,
            authorId = "author-1",
            neighborhoodId = "nb-1",
            content = "Habari za mtaa",
        )

    @Test
    fun `flag success marks loaded post reported`() =
        runTest {
            coEvery { postsRepository.getPostById("p-1") } returns Resource.Success(post("p-1"))
            coEvery { postsRepository.flagPost("p-1", "udhaliti") } returns Resource.Success(Unit)

            val vm = PostsViewModel(postsRepository, userPreferencesRepository)
            advanceUntilIdle()

            vm.loadPost("p-1")
            advanceUntilIdle()
            assertFalse(
                vm.uiState.value.selectedPost!!
                    .isReported,
            )

            vm.flagPost("p-1", "udhaliti")
            advanceUntilIdle()

            assertTrue(
                vm.uiState.value.selectedPost!!
                    .isReported,
            )
            assertNull(vm.uiState.value.error)
            coVerify(exactly = 1) { postsRepository.flagPost("p-1", "udhaliti") }
        }

    @Test
    fun `flag failure sets error and leaves post unreported`() =
        runTest {
            coEvery { postsRepository.getPostById("p-2") } returns Resource.Success(post("p-2"))
            coEvery { postsRepository.flagPost("p-2", null) } returns Resource.Error("rate_limited")

            val vm = PostsViewModel(postsRepository, userPreferencesRepository)
            advanceUntilIdle()

            vm.loadPost("p-2")
            advanceUntilIdle()

            vm.flagPost("p-2")
            advanceUntilIdle()

            assertEquals("rate_limited", vm.uiState.value.error)
            assertFalse(
                vm.uiState.value.selectedPost!!
                    .isReported,
            )
        }

    @Test
    fun `flag with reason forwards it to repository`() =
        runTest {
            coEvery { postsRepository.getPostById("p-3") } returns Resource.Success(post("p-3"))
            coEvery { postsRepository.flagPost("p-3", "Uchochezi") } returns Resource.Success(Unit)

            val vm = PostsViewModel(postsRepository, userPreferencesRepository)
            advanceUntilIdle()

            vm.flagPost("p-3", "Uchochezi")
            advanceUntilIdle()

            coVerify(exactly = 1) { postsRepository.flagPost("p-3", "Uchochezi") }
        }
}
