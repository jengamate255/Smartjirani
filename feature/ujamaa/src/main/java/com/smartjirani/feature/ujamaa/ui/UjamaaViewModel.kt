package com.smartjirani.feature.ujamaa.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.realtime.RealtimeEvent
import com.smartjirani.core.realtime.RealtimeService
import com.smartjirani.feature.ujamaa.ContributionResult
import com.smartjirani.feature.ujamaa.R
import com.smartjirani.feature.ujamaa.UjamaaContent
import com.smartjirani.feature.ujamaa.UjamaaFilter
import com.smartjirani.feature.ujamaa.UjamaaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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

data class UjamaaUiState(
    val isLoading: Boolean = true,
    val neighborhoodId: String? = null,
    val content: UjamaaContent = UjamaaContent(),
    val selectedFilter: UjamaaFilter = UjamaaFilter.ALL,
    val error: String? = null,
    val toast: UiToast? = null,
    /** Label for the shell's location chip. */
    val locationLabel: String = "Mtaa",
    /** Set when PesaPal checkout has to be finished in a browser; the screen opens it. */
    val pendingCheckoutUrl: String? = null,
    val isContributing: Boolean = false,
) {
    val visibleResources: List<com.smartjirani.feature.ujamaa.SharedResource>
        get() =
            when (selectedFilter) {
                UjamaaFilter.ALL -> content.resources
                else -> content.resources.filter { matches(it.category, selectedFilter) }
            }

    private fun matches(
        category: String,
        filter: UjamaaFilter,
    ): Boolean =
        when (filter) {
            UjamaaFilter.TOOLS -> category == "TOOL" || category == "GEAR"
            UjamaaFilter.RIDES -> category == "CARPOOL"
            UjamaaFilter.BOOKS -> category == "BOOK"
            UjamaaFilter.HARVEST -> category == "HARVEST"
            UjamaaFilter.SKILLS -> category == "SKILL"
            UjamaaFilter.ALL -> true
        }
}

@HiltViewModel
class UjamaaViewModel
    @Inject
    constructor(
        private val ujamaaRepository: UjamaaRepository,
        private val realtimeService: RealtimeService,
        userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(UjamaaUiState())
        val uiState: StateFlow<UjamaaUiState> = _uiState.asStateFlow()

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
                            is RealtimeEvent.UrgentNeedChanged -> event.neighborhoodId
                            is RealtimeEvent.SharedResourceChanged -> event.neighborhoodId
                            is RealtimeEvent.HarambeeFundChanged -> event.neighborhoodId
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

        fun onFilterSelected(filter: UjamaaFilter) {
            _uiState.update { it.copy(selectedFilter = filter) }
        }

        fun onOfferItemClicked() {
            _uiState.update { it.copy(toast = UiToast(R.string.ujamaa_toast_offer_opening)) }
        }

        fun onAskHelpClicked() {
            _uiState.update { it.copy(toast = UiToast(R.string.ujamaa_toast_ask_opening)) }
        }

        fun onOpenMapClicked() {
            _uiState.update { it.copy(toast = UiToast(R.string.ujamaa_toast_map_coming)) }
        }

        fun dismissError() = _uiState.update { it.copy(error = null) }

        fun dismissToast() = _uiState.update { it.copy(toast = null) }

        fun onCheckoutHandled() {
            _uiState.update { it.copy(pendingCheckoutUrl = null) }
        }

        fun onOfferHelp(
            needId: String,
            offer: Boolean,
        ) {
            updateNeedHelp(needId, offer)
            viewModelScope.launch {
                val result =
                    if (offer) {
                        ujamaaRepository.offerHelp(needId)
                    } else {
                        ujamaaRepository.withdrawHelp(needId)
                    }
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(toast = if (offer) UiToast(R.string.ujamaa_toast_help_on) else UiToast(R.string.ujamaa_toast_help_off))
                        }
                    }

                    is Resource.Error -> {
                        updateNeedHelp(needId, !offer)
                        _uiState.update { it.copy(error = result.message) }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun onRequestResource(resourceId: String) {
            viewModelScope.launch {
                when (val result = ujamaaRepository.requestResource(resourceId)) {
                    is Resource.Success -> {
                        updateResourceRequest(resourceId, requested = true)
                        _uiState.update { it.copy(toast = UiToast(R.string.ujamaa_toast_resource_requested)) }
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

        fun onContribute(amountKes: Double) {
            val fundId =
                _uiState.value.content.fund
                    ?.id ?: return
            val neighborhoodId = _uiState.value.neighborhoodId ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isContributing = true) }
                when (val result = ujamaaRepository.contribute(fundId, amountKes)) {
                    is Resource.Success -> {
                        val contribution = result.data
                        if (contribution.isSettled) {
                            _uiState.update {
                                it.copy(
                                    isContributing = false,
                                    toast = UiToast(R.string.ujamaa_toast_contribution_settled, listOf(contribution.amountKes.toLong())),
                                )
                            }
                            refresh(neighborhoodId)
                        } else {
                            _uiState.update {
                                it.copy(
                                    isContributing = false,
                                    pendingCheckoutUrl = contribution.checkoutUrl,
                                )
                            }
                            pollPayment(contribution, neighborhoodId)
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isContributing = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        private suspend fun pollPayment(
            contribution: ContributionResult,
            neighborhoodId: String,
        ) {
            repeat(10) {
                delay(3_000)
                val settled = ujamaaRepository.settleContribution(contribution.reference, neighborhoodId)
                if (settled is Resource.Success && settled.data == true) {
                    _uiState.update {
                        it.copy(
                            toast = UiToast(R.string.ujamaa_toast_payment_complete, listOf(contribution.amountKes.toLong())),
                        )
                    }
                    refresh(neighborhoodId)
                    return
                }
            }
        }

        private suspend fun refresh(neighborhoodId: String) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = ujamaaRepository.getContent(neighborhoodId)) {
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

        private fun updateNeedHelp(
            needId: String,
            offered: Boolean,
        ) {
            _uiState.update { state ->
                state.copy(
                    content =
                        state.content.copy(
                            needs =
                                state.content.needs.map { need ->
                                    if (need.id != needId) need else need.copy(iOfferedHelp = offered)
                                },
                        ),
                )
            }
        }

        private fun updateResourceRequest(
            resourceId: String,
            requested: Boolean,
        ) {
            _uiState.update { state ->
                state.copy(
                    content =
                        state.content.copy(
                            resources =
                                state.content.resources.map { resource ->
                                    if (resource.id != resourceId) {
                                        resource
                                    } else {
                                        resource.copy(iRequested = requested)
                                    }
                                },
                        ),
                )
            }
        }
    }
