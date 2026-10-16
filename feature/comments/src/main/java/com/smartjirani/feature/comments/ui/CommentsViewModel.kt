package com.smartjirani.feature.comments.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.feature.comments.Comment
import com.smartjirani.feature.comments.CommentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommentsUiState(
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val error: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && comments.isEmpty()
}

/**
 * Owns the comment thread for a single post.
 *
 * The Room cache is the UI source of truth, so an already-viewed thread renders instantly and
 * survives a dropped connection; [refresh] then reconciles it against the server.
 */
@HiltViewModel
class CommentsViewModel
    @Inject
    constructor(
        private val commentsRepository: CommentsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CommentsUiState())
        val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

        private var observeJob: Job? = null
        private var activePostId: String? = null

        /** Idempotent: re-entering with the same post does not restart the cache collector. */
        fun start(postId: String) {
            if (postId.isBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Posti hazieleweki.") }
                return
            }
            if (activePostId == postId && observeJob?.isActive == true) {
                refresh(postId)
                return
            }
            activePostId = postId
            observeJob?.cancel()
            observeJob =
                viewModelScope.launch {
                    commentsRepository.observeComments(postId).collect { cached ->
                        _uiState.update { it.copy(comments = cached, isLoading = false) }
                    }
                }
            refresh(postId)
        }

        fun refresh(postId: String = activePostId.orEmpty()) {
            if (postId.isBlank()) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = it.comments.isEmpty(), error = null) }
                when (val result = commentsRepository.getComments(postId)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isLoading = false, comments = result.data, error = null)
                        }
                    }

                    // Keep the cached thread visible; surface the failure as a banner.
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

        fun addComment(content: String) {
            val postId = activePostId ?: return
            val trimmed = content.trim()
            if (trimmed.isEmpty()) {
                _uiState.update { it.copy(error = "Andika ujumbe wako kwanza.") }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isSending = true, error = null) }
                val comment =
                    Comment(
                        id = "",
                        postId = postId,
                        // Resolved from the authenticated session inside the repository.
                        authorId = "",
                        content = trimmed,
                    )
                when (val result = commentsRepository.createComment(comment)) {
                    is Resource.Success -> {
                        _uiState.update {
                            // The repository already wrote the row to Room, but append defensively so
                            // the new comment appears even if the cache write is coalesced away.
                            if (it.comments.any { commentItem -> commentItem.id == result.data.id }) {
                                it
                            } else {
                                it.copy(comments = it.comments + result.data)
                            }
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
                _uiState.update { it.copy(isSending = false) }
            }
        }

        fun deleteComment(commentId: String) {
            viewModelScope.launch {
                when (val result = commentsRepository.deleteComment(commentId)) {
                    is Resource.Success -> {
                        _uiState.update { state ->
                            state.copy(comments = state.comments.filterNot { it.id == commentId })
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

        fun dismissError() {
            _uiState.update { it.copy(error = null) }
        }
    }
