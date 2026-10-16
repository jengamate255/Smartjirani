package com.smartjirani.feature.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.feature.feed.FeedExtras
import com.smartjirani.feature.feed.FeedExtrasRepository
import com.smartjirani.feature.posts.Post
import com.smartjirani.feature.posts.PostType
import com.smartjirani.feature.posts.PostsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Tanzania-first feed filters. [postType] is null for the unfiltered [FeedFilter.ALL] case. */
enum class FeedFilter(
    val label: String,
    val postType: PostType?,
) {
    ALL("Zote", null),
    ALERT("Tahadhari", PostType.ALERT),
    SERVICE("Huduma", PostType.SERVICE),
    BUY_SELL("Biashara", PostType.BUY_SELL),
    ANNOUNCEMENT("Matangazo", PostType.ANNOUNCEMENT),
}

data class FeedUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val neighborhoodId: String? = null,
    val posts: List<Post> = emptyList(),
    val visiblePosts: List<Post> = emptyList(),
    val selectedFilter: FeedFilter = FeedFilter.ALL,
    val extras: FeedExtras = FeedExtras(),
    /** Post ids this user flagged or the community flagged (3+ distinct flaggers). */
    val reportedPostIds: Set<String> = emptySet(),
    /** Label shown in the shell's location chip, e.g. "Kariakoo". */
    val locationLabel: String = "Mtaa",
    val toast: String? = null,
    val error: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && visiblePosts.isEmpty()
}

/**
 * Feed state owner.
 *
 * Room is the single source of truth for the UI: the cache is observed per neighborhood so a
 * previously loaded feed paints instantly and keeps working offline, while a network refresh
 * replaces that neighborhood's cache atomically. Likes only reach the cache after the server
 * round-trip succeeds, so a failed like rolls back instead of lying.
 */
@HiltViewModel
class FeedViewModel
    @Inject
    constructor(
        private val postsRepository: PostsRepository,
        private val feedExtrasRepository: FeedExtrasRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FeedUiState())
        val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                userPreferencesRepository.userPreferences
                    .map { it.selectedNeighborhoodId to it.selectedNeighborhoodName }
                    .distinctUntilChanged()
                    .collectLatest { (neighborhoodId, neighborhoodName) ->
                        _uiState.update {
                            it.copy(
                                neighborhoodId = neighborhoodId,
                                locationLabel =
                                    neighborhoodName
                                        ?: if (neighborhoodId == null) "Mtaa" else it.locationLabel,
                            )
                        }
                        if (neighborhoodId == null) {
                            _uiState.update {
                                it.copy(
                                    neighborhoodId = null,
                                    isLoading = false,
                                    isRefreshing = false,
                                    posts = emptyList(),
                                    visiblePosts = emptyList(),
                                    extras = FeedExtras(),
                                )
                            }
                            return@collectLatest
                        }
                        // Scoped children: switching neighborhoods cancels the previous cache
                        // collector instead of leaking it.
                        coroutineScope {
                            launch { observeCache(neighborhoodId) }
                            launch { refreshExtras(neighborhoodId) }
                            refreshNow(neighborhoodId)
                        }
                    }
            }
        }

        fun onFilterSelected(filter: FeedFilter) {
            _uiState.update { state -> state.copy(selectedFilter = filter).applyPosts(state.posts) }
        }

        fun refresh() {
            val neighborhoodId = _uiState.value.neighborhoodId ?: return
            viewModelScope.launch { refreshNow(neighborhoodId) }
        }

        fun dismissError() {
            _uiState.update { it.copy(error = null) }
        }

        fun dismissToast() {
            _uiState.update { it.copy(toast = null) }
        }

        /** One vote per poll: re-voting moves the vote, so the extras are refreshed from source. */
        fun onVote(
            pollId: String,
            optionId: String,
        ) {
            viewModelScope.launch {
                when (val result = feedExtrasRepository.vote(pollId, optionId)) {
                    is Resource.Success -> {
                        val neighborhoodId = _uiState.value.neighborhoodId ?: return@launch
                        refreshExtras(neighborhoodId)
                        _uiState.update { it.copy(toast = "Kura yako imesajiliwa") }
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

        fun toggleLike(postId: String) {
            val current = _uiState.value.posts.firstOrNull { it.id == postId } ?: return
            val willLike = !current.isLikedByUser
            applyOptimisticLike(postId, willLike)
            viewModelScope.launch {
                val result = if (willLike) postsRepository.likePost(postId) else postsRepository.unlikePost(postId)
                if (result is Resource.Error) {
                    applyOptimisticLike(postId, !willLike)
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }

        private suspend fun observeCache(neighborhoodId: String) {
            postsRepository.observePosts(neighborhoodId).collect { cached ->
                _uiState.update { it.applyPosts(cached) }
            }
        }

        private suspend fun refreshExtras(neighborhoodId: String) {
            when (val result = feedExtrasRepository.getFeedExtras(neighborhoodId)) {
                is Resource.Success -> _uiState.update { it.copy(extras = result.data) }

                // The post list still works without the street strip; fail soft.
                is Resource.Error -> Unit

                is Resource.Loading -> Unit
            }
        }

        private suspend fun refreshNow(neighborhoodId: String) {
            _uiState.update {
                it.copy(
                    isLoading = it.posts.isEmpty(),
                    isRefreshing = it.posts.isNotEmpty(),
                    error = null,
                )
            }
            when (val result = postsRepository.getPosts(neighborhoodId)) {
                is Resource.Success -> {
                    // The "reported" badge is a transparency overlay; a failed read must not
                    // block the feed, so it fails soft to whatever ids we already have.
                    val reported = postsRepository.getReportedPostIds()
                    _uiState.update { state ->
                        val withReported =
                            if (reported is Resource.Success) {
                                state.copy(reportedPostIds = reported.data)
                            } else {
                                state
                            }
                        withReported
                            .applyPosts(result.data)
                            .copy(isLoading = false, isRefreshing = false, error = null)
                    }
                }

                // Keep whatever is cached on screen; surface the failure as a dismissible banner.
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, error = result.message)
                    }
                }

                is Resource.Loading -> {
                    Unit
                }
            }
        }

        private fun applyOptimisticLike(
            postId: String,
            isLiked: Boolean,
        ) {
            _uiState.update { state ->
                val delta = if (isLiked) 1 else -1
                state.applyPosts(
                    state.posts.map { post ->
                        if (post.id != postId) {
                            post
                        } else {
                            post.copy(
                                isLikedByUser = isLiked,
                                likesCount = (post.likesCount + delta).coerceAtLeast(0),
                            )
                        }
                    },
                )
            }
        }

        private fun FeedUiState.applyPosts(posts: List<Post>): FeedUiState {
            val overlaid =
                posts.map { post ->
                    if (post.id in reportedPostIds) post.copy(isReported = true) else post
                }
            return copy(
                posts = overlaid,
                visiblePosts = overlaid.filteredBy(selectedFilter),
                isLoading = false,
            )
        }

        private fun List<Post>.filteredBy(filter: FeedFilter): List<Post> {
            val postType = filter.postType ?: return this
            return filter { it.postType == postType }
        }
    }
