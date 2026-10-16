package com.smartjirani.feature.ujamaa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.feature.ujamaa.MtaaMapRepository
import com.smartjirani.feature.ujamaa.MtaaPin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MtaaMapUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Label for the shell's location chip. */
    val locationLabel: String = "Mtaa",
    val selectedNeighborhoodId: String? = null,
    val pins: List<MtaaPin> = emptyList(),
) {
    val selectedPin: MtaaPin?
        get() = pins.firstOrNull { it.id == selectedNeighborhoodId }

    val isEmpty: Boolean get() = !isLoading && pins.isEmpty()
}

@HiltViewModel
class MtaaMapViewModel
    @Inject
    constructor(
        private val mapRepository: MtaaMapRepository,
        userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MtaaMapUiState())
        val uiState: StateFlow<MtaaMapUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                userPreferencesRepository.userPreferences
                    .map { it.selectedNeighborhoodId to it.selectedNeighborhoodName }
                    .distinctUntilChanged()
                    .collect { (neighborhoodId, neighborhoodName) ->
                        _uiState.update {
                            it.copy(
                                selectedNeighborhoodId = neighborhoodId,
                                locationLabel =
                                    neighborhoodName
                                        ?: if (neighborhoodId == null) "Mtaa" else it.locationLabel,
                            )
                        }
                    }
            }
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                when (val result = mapRepository.getNeighborhoods()) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isLoading = false, pins = result.data)
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

        fun dismissError() = _uiState.update { it.copy(error = null) }
    }
