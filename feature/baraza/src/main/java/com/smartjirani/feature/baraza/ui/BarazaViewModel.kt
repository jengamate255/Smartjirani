package com.smartjirani.feature.baraza.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.realtime.RealtimeEvent
import com.smartjirani.core.realtime.RealtimeService
import com.smartjirani.feature.baraza.BarazaContent
import com.smartjirani.feature.baraza.BarazaRepository
import com.smartjirani.feature.baraza.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiToast(
    @StringRes val messageId: Int,
    val args: List<Any> = emptyList(),
)

data class BarazaUiState(
    val isLoading: Boolean = true,
    val neighborhoodId: String? = null,
    val content: BarazaContent = BarazaContent(),
    val error: String? = null,
    val toast: UiToast? = null,
    /** Label for the shell's location chip. */
    val locationLabel: String = "Mtaa",
)

@HiltViewModel
class BarazaViewModel
    @Inject
    constructor(
        private val barazaRepository: BarazaRepository,
        private val realtimeService: RealtimeService,
        userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BarazaUiState())
        val uiState: StateFlow<BarazaUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                userPreferencesRepository.userPreferences
                    .map { it.selectedNeighborhoodId to it.selectedNeighborhoodName }
                    .distinctUntilChanged()
                    .collect { (neighborhoodId, neighborhoodName) ->
                        _uiState.update {
                            it.copy(
                                neighborhoodId = neighborhoodId,
                                locationLabel =
                                    neighborhoodName
                                        ?: if (neighborhoodId == null) "Mtaa" else it.locationLabel,
                            )
                        }
                        if (neighborhoodId != null) {
                            realtimeService.subscribeToNeighborhood(neighborhoodId)
                            refresh(neighborhoodId)
                        } else {
                            // Nothing to load: stop the spinner so the "pick your mtaa" state shows.
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    }
            }
            viewModelScope.launch {
                realtimeService.events.collect { event ->
                    val currentNeighborhood = _uiState.value.neighborhoodId ?: return@collect
                    val changedNeighborhood =
                        when (event) {
                            is RealtimeEvent.BarazaEventChanged -> event.neighborhoodId
                            is RealtimeEvent.ForumThreadChanged -> event.neighborhoodId
                            is RealtimeEvent.HarambeeProjectChanged -> event.neighborhoodId
                            else -> null
                        }
                    if (changedNeighborhood != null && changedNeighborhood == currentNeighborhood) {
                        refresh(currentNeighborhood)
                    }
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            val neighborhoodId = _uiState.value.neighborhoodId
            if (neighborhoodId != null) {
                viewModelScope.launch { realtimeService.unsubscribeFromNeighborhood(neighborhoodId) }
            }
        }

        fun refresh() {
            val neighborhoodId = _uiState.value.neighborhoodId ?: return
            viewModelScope.launch { refresh(neighborhoodId) }
        }

        fun load(neighborhoodId: String) = viewModelScope.launch { refresh(neighborhoodId) }

        fun dismissError() = _uiState.update { it.copy(error = null) }

        fun dismissToast() = _uiState.update { it.copy(toast = null) }

        fun onRsvpToggled(
            eventId: String,
            attending: Boolean,
        ) {
            updateEventOptimistically(eventId, attending)
            viewModelScope.launch {
                val result = barazaRepository.toggleRsvp(eventId, attending)
                if (result is Resource.Error) {
                    updateEventOptimistically(eventId, !attending)
                    _uiState.update { it.copy(error = result.message, toast = null) }
                } else if (result is Resource.Success) {
                    _uiState.update {
                        it.copy(toast = if (attending) UiToast(R.string.baraza_toast_rsvp_on) else UiToast(R.string.baraza_toast_rsvp_off))
                    }
                }
            }
        }

        fun onAskQuestionClicked(eventId: String) {
            updateEventQuestions(eventId, increment = 1)
            _uiState.update { it.copy(toast = UiToast(R.string.baraza_toast_question_queued)) }
        }

        fun onVoteToggled(
            threadId: String,
            voted: Boolean,
        ) {
            updateThreadVote(threadId, voted)
            viewModelScope.launch {
                val result = barazaRepository.toggleThreadVote(threadId, voted)
                if (result is Resource.Error) {
                    updateThreadVote(threadId, !voted)
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }

        fun onVolunteerToggled(
            projectId: String,
            joined: Boolean,
        ) {
            updateProjectVolunteer(projectId, joined)
            viewModelScope.launch {
                val result = barazaRepository.toggleVolunteer(projectId, joined)
                if (result is Resource.Error) {
                    updateProjectVolunteer(projectId, !joined)
                    _uiState.update { it.copy(error = result.message) }
                } else if (result is Resource.Success) {
                    _uiState.update {
                        it.copy(toast = if (joined) UiToast(R.string.baraza_toast_volunteer_on) else UiToast(R.string.baraza_toast_volunteer_off))
                    }
                }
            }
        }

        private suspend fun refresh(neighborhoodId: String) {
            _uiState.update { it.copy(isLoading = it.content.isEmpty, error = null) }
            when (val result = barazaRepository.getContent(neighborhoodId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, content = result.data, error = null)
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

        private fun updateEventOptimistically(
            eventId: String,
            attending: Boolean,
        ) {
            _uiState.update { state ->
                state.copy(
                    content =
                        state.content.copy(
                            events =
                                state.content.events.map { event ->
                                    if (event.id != eventId) {
                                        event
                                    } else {
                                        event.copy(
                                            iAmAttending = attending,
                                            attendingCount =
                                                (event.attendingCount + if (attending) 1 else -1)
                                                    .coerceAtLeast(0),
                                        )
                                    }
                                },
                        ),
                )
            }
        }

        private fun updateEventQuestions(
            eventId: String,
            increment: Int,
        ) {
            _uiState.update { state ->
                state.copy(
                    content =
                        state.content.copy(
                            events =
                                state.content.events.map { event ->
                                    if (event.id != eventId) {
                                        event
                                    } else {
                                        event.copy(
                                            questionsQueued = (event.questionsQueued + increment).coerceAtLeast(0),
                                        )
                                    }
                                },
                        ),
                )
            }
        }

        private fun updateThreadVote(
            threadId: String,
            voted: Boolean,
        ) {
            _uiState.update { state ->
                state.copy(
                    content =
                        state.content.copy(
                            threads =
                                state.content.threads.map { thread ->
                                    if (thread.id != threadId) {
                                        thread
                                    } else {
                                        thread.copy(
                                            votedByMe = voted,
                                            votes = (thread.votes + if (voted) 1 else -1).coerceAtLeast(0),
                                        )
                                    }
                                },
                        ),
                )
            }
        }

        private fun updateProjectVolunteer(
            projectId: String,
            joined: Boolean,
        ) {
            _uiState.update { state ->
                state.copy(
                    content =
                        state.content.copy(
                            projects =
                                state.content.projects.map { project ->
                                    if (project.id != projectId) {
                                        project
                                    } else {
                                        project.copy(
                                            iAmVolunteering = joined,
                                            volunteersFilled =
                                                (project.volunteersFilled + if (joined) 1 else -1)
                                                    .coerceAtLeast(0),
                                        )
                                    }
                                },
                        ),
                )
            }
        }
    }
