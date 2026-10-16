package com.smartjirani.feature.civic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.feature.civic.CivicAdminData
import com.smartjirani.feature.civic.CivicDirectoryUser
import com.smartjirani.feature.civic.CivicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CivicAdminUiState(
    val isLoading: Boolean = true,
    val data: CivicAdminData = CivicAdminData(),
    val isMutating: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
)

@HiltViewModel
class CivicAdminViewModel
    @Inject
    constructor(
        private val civicRepository: CivicRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CivicAdminUiState())
        val uiState: StateFlow<CivicAdminUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = it.data == CivicAdminData(), error = null) }
                when (val result = civicRepository.getAdminData()) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isLoading = false, data = result.data, error = null)
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

        fun searchDirectory(
            query: String,
            onResult: (List<CivicDirectoryUser>) -> Unit,
        ) {
            viewModelScope.launch {
                when (val result = civicRepository.searchDirectory(query)) {
                    is Resource.Success -> onResult(result.data)
                    else -> onResult(emptyList())
                }
            }
        }

        fun createUnit(
            unitTypeId: String,
            name: String,
            parentId: String?,
            code: String?,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isMutating = true, error = null) }
                when (val result = civicRepository.createUnit(unitTypeId, name.trim(), parentId, code)) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isMutating = false, toast = "Kitengo kimeongezwa") }
                        loadAdminData()
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isMutating = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun createCategory(
            nameEn: String,
            nameSw: String,
            description: String?,
            requiresVerification: Boolean,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isMutating = true, error = null) }
                when (
                    val result =
                        civicRepository.createCategory(
                            nameEn = nameEn.trim(),
                            nameSw = nameSw.trim(),
                            description = description?.trim()?.takeIf { it.isNotBlank() },
                            requiresVerification = requiresVerification,
                        )
                ) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isMutating = false, toast = "Kundi jipya limeongezwa") }
                        loadAdminData()
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isMutating = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun createSlaRule(
            categoryId: String?,
            severity: String?,
            priority: String?,
            responseHours: Double,
            resolutionHours: Double,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isMutating = true, error = null) }
                when (
                    val result =
                        civicRepository.createSlaRule(
                            categoryId = categoryId,
                            severity = severity,
                            priority = priority,
                            responseHours = responseHours,
                            resolutionHours = resolutionHours,
                        )
                ) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isMutating = false, toast = "Kanuni ya muda imehifadhiwa") }
                        loadAdminData()
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isMutating = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun assignRole(
            userId: String,
            role: String,
            orgUnitId: String,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isMutating = true, error = null) }
                when (val result = civicRepository.assignRole(userId, role, orgUnitId)) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isMutating = false, toast = "Wadhifa umepewa") }
                        loadAdminData()
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isMutating = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun revokeRole(roleRowId: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isMutating = true, error = null) }
                when (val result = civicRepository.revokeRole(roleRowId)) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isMutating = false, toast = "Wadhifa umeondolewa") }
                        loadAdminData()
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isMutating = false, error = result.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        private suspend fun loadAdminData() {
            when (val result = civicRepository.getAdminData()) {
                is Resource.Success -> _uiState.update { it.copy(data = result.data) }
                else -> Unit
            }
        }

        fun dismissError() = _uiState.update { it.copy(error = null) }

        fun dismissToast() = _uiState.update { it.copy(toast = null) }
    }
