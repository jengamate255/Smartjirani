package com.smartjirani.feature.posts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.feature.posts.Post
import com.smartjirani.feature.posts.PostType
import com.smartjirani.feature.posts.PostsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PostsUiState(
    val isLoading: Boolean = false,
    val posts: List<Post> = emptyList(),
    val selectedPost: Post? = null,
    val isPostLoading: Boolean = false,
    val error: String? = null,
    val isPostCreated: Boolean = false,
)

/**
 * Owns the post composer, a post list, and the single post shown on the detail screen.
 *
 * The neighborhood is never chosen in the UI: posts are always scoped to the neighborhood the
 * resident selected during onboarding, read from [UserPreferencesRepository].
 */
@HiltViewModel
class PostsViewModel
    @Inject
    constructor(
        private val postsRepository: PostsRepository,
        userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PostsUiState())
        val uiState: StateFlow<PostsUiState> = _uiState.asStateFlow()

        val neighborhoodId: StateFlow<String?> =
            userPreferencesRepository.userPreferences
                .map { it.selectedNeighborhoodId }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        fun loadPost(postId: String) {
            if (postId.isBlank()) {
                _uiState.update { it.copy(isPostLoading = false, error = "Posti hazieleweki.") }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isPostLoading = it.selectedPost == null, error = null) }
                when (val result = postsRepository.getPostById(postId)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isPostLoading = false, selectedPost = result.data, error = null)
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isPostLoading = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun createPost(
            content: String,
            postType: PostType = PostType.GENERAL,
            imageUrl: String? = null,
        ) {
            val neighborhood = neighborhoodId.value
            if (neighborhood.isNullOrBlank()) {
                _uiState.update { it.copy(error = "Chagua mtaa wako kwanza kabla ya kuandika posti.") }
                return
            }
            val trimmed = content.trim()
            if (trimmed.isEmpty()) {
                _uiState.update { it.copy(error = "Andika kitu kwenye posti kabla ya kuchapisha.") }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val post =
                    Post(
                        id = "",
                        // Resolved from the authenticated session inside the repository, never trusted here.
                        authorId = "",
                        neighborhoodId = neighborhood,
                        content = trimmed,
                        imageUrl = imageUrl,
                        postType = postType,
                    )
                when (val result = postsRepository.createPost(post)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isLoading = false, isPostCreated = true, error = null)
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun deletePost(postId: String) {
            viewModelScope.launch {
                when (val result = postsRepository.deletePost(postId)) {
                    is Resource.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                posts = state.posts.filterNot { it.id == postId },
                                selectedPost = state.selectedPost?.takeIf { it.id != postId },
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        /** Optimistic like that covers both the list and the detail screen's post. */
        fun toggleLike(postId: String) {
            val current =
                _uiState.value.posts.firstOrNull { it.id == postId }
                    ?: _uiState.value.selectedPost?.takeIf { it.id == postId }
                    ?: return
            val willLike = !current.isLikedByUser
            setLiked(postId, willLike)
            viewModelScope.launch {
                val result = if (willLike) postsRepository.likePost(postId) else postsRepository.unlikePost(postId)
                if (result is Resource.Error) {
                    // Roll back so a failed like never leaves a phantom heart behind.
                    setLiked(postId, !willLike)
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }

        /** Resident flags a post as inappropriate; reflects the "reported" state locally. */
        fun flagPost(
            postId: String,
            reason: String? = null,
        ) {
            viewModelScope.launch {
                when (val result = postsRepository.flagPost(postId, reason)) {
                    is Resource.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                posts = state.posts.map { it.withReported(postId) },
                                selectedPost = state.selectedPost?.withReported(postId),
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun clearCreated() {
            _uiState.update { it.copy(isPostCreated = false) }
        }

        fun dismissError() {
            _uiState.update { it.copy(error = null) }
        }

        private fun setLiked(
            postId: String,
            isLiked: Boolean,
        ) {
            val delta = if (isLiked) 1 else -1
            _uiState.update { state ->
                state.copy(
                    posts = state.posts.map { it.withLike(postId, isLiked, delta) },
                    selectedPost = state.selectedPost?.withLike(postId, isLiked, delta),
                )
            }
        }

        private fun Post.withLike(
            postId: String,
            isLiked: Boolean,
            delta: Int,
        ): Post =
            if (id != postId) {
                this
            } else {
                copy(
                    isLikedByUser = isLiked,
                    likesCount = (likesCount + delta).coerceAtLeast(0),
                )
            }

        private fun Post.withReported(postId: String): Post = if (id != postId) this else copy(isReported = true)
    }
