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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NeighborhoodUiState(
    val isLoading: Boolean = false,
    val neighborhoods: List<Neighborhood> = emptyList(),
    val selectedNeighborhood: Neighborhood? = null,
    val isSelecting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class NeighborhoodViewModel
    @Inject
    constructor(
        private val neighborhoodRepository: NeighborhoodRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NeighborhoodUiState())
        val uiState: StateFlow<NeighborhoodUiState> = _uiState.asStateFlow()

        init {
            loadNeighborhoods()
        }

        fun loadNeighborhoods() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                when (val result = neighborhoodRepository.getNeighborhoods()) {
                    is Resource.Success -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                neighborhoods = result.data,
                            )
                    }

                    is Resource.Error -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                error = result.message,
                            )
                    }

                    is Resource.Loading -> {}
                }
            }
        }

        fun selectNeighborhood(neighborhood: Neighborhood) {
            if (_uiState.value.isSelecting) return
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isSelecting = true)
                when (val result = neighborhoodRepository.joinNeighborhood(neighborhood.id)) {
                    is Resource.Success -> {
                        _uiState.value =
                            _uiState.value.copy(
                                selectedNeighborhood = neighborhood,
                                isSelecting = false,
                            )
                    }

                    is Resource.Error -> {
                        _uiState.value =
                            _uiState.value.copy(
                                error = result.message,
                                isSelecting = false,
                            )
                    }

                    is Resource.Loading -> {}
                }
            }
        }
    }
