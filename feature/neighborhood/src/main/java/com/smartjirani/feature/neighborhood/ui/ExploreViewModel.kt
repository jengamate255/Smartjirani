package com.smartjirani.feature.neighborhood.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.feature.neighborhood.Neighborhood
import com.smartjirani.feature.neighborhood.NeighborhoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExploreUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val neighborhoods: List<Neighborhood> = emptyList(),
    val joinedNeighborhoodIds: Set<String> = emptySet(),
    val joiningId: String? = null,
    val error: String? = null,
) {
    /**
     * Client-side search. The dataset is a country's worth of neighborhoods, so filtering
     * locally keeps typing instant instead of round-tripping on every keystroke.
     */
    val results: List<Neighborhood>
        get() {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return neighborhoods
            return neighborhoods.filter { neighborhood ->
                neighborhood.name.contains(trimmed, ignoreCase = true) ||
                    neighborhood.city.contains(trimmed, ignoreCase = true) ||
                    neighborhood.region.contains(trimmed, ignoreCase = true)
            }
        }

    val isEmpty: Boolean get() = !isLoading && neighborhoods.isEmpty()
    val hasNoMatches: Boolean get() = !isLoading && neighborhoods.isNotEmpty() && results.isEmpty()
}

@HiltViewModel
class ExploreViewModel
    @Inject
    constructor(
        private val neighborhoodRepository: NeighborhoodRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExploreUiState())
        val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                when (val result = neighborhoodRepository.getNeighborhoods()) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false, neighborhoods = result.data) }
                        // Memberships decide the "Joined" badge; a failure here is non-fatal.
                        when (val mine = neighborhoodRepository.getMyNeighborhoods()) {
                            is Resource.Success -> {
                                _uiState.update {
                                    it.copy(joinedNeighborhoodIds = mine.data.map(Neighborhood::id).toSet())
                                }
                            }

                            else -> {
                                Unit
                            }
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

        fun onQueryChange(query: String) {
            _uiState.update { it.copy(query = query) }
        }

        fun join(neighborhoodId: String) {
            if (_uiState.value.joiningId != null) return
            viewModelScope.launch {
                _uiState.update { it.copy(joiningId = neighborhoodId, error = null) }
                when (val result = neighborhoodRepository.joinNeighborhood(neighborhoodId)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                joiningId = null,
                                joinedNeighborhoodIds = it.joinedNeighborhoodIds + neighborhoodId,
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(joiningId = null, error = result.message) }
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
