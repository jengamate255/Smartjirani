package com.smartjirani.feature.civic.ui

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.audio.AudioRecorder
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.location.LocationRepository
import com.smartjirani.feature.civic.CivicAiDedupResult
import com.smartjirani.feature.civic.CivicAiProvider
import com.smartjirani.feature.civic.CivicCategory
import com.smartjirani.feature.civic.CivicRepository
import com.smartjirani.feature.civic.CivicSubmitResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CivicNewReportUiState(
    val categories: List<CivicCategory> = emptyList(),
    val isLoadingCategories: Boolean = true,
    val isUploadingImage: Boolean = false,
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isSubmitting: Boolean = false,
    val categoryError: Boolean = false,
    val titleError: Boolean = false,
    val descriptionError: Boolean = false,
    val error: String? = null,
    val result: CivicSubmitResult? = null,
    val aiTriaged: Boolean = false,
    val duplicateCheck: CivicAiDedupResult? = null,
    val isCheckingDuplicates: Boolean = false,
    val pendingSubmission: PendingSubmission? = null,
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val recordingDurationMs: Long = 0L,
) {
    val locationSet: Boolean get() = latitude != null && longitude != null
}

data class PendingSubmission(
    val categoryId: String,
    val title: String,
    val description: String,
    val locationDescription: String?,
    val severity: String,
    val isAnonymous: Boolean,
)

@HiltViewModel
class CivicNewReportViewModel
    @Inject
    constructor(
        private val civicRepository: CivicRepository,
        private val civicAiProvider: CivicAiProvider,
        private val locationRepository: LocationRepository,
        private val audioRecorder: AudioRecorder,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CivicNewReportUiState())
        val uiState: StateFlow<CivicNewReportUiState> = _uiState.asStateFlow()

        init {
            loadCategories()
        }

        fun loadCategories() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingCategories = true, error = null) }
                when (val res = civicRepository.getCategories()) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isLoadingCategories = false, categories = res.data, error = null)
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoadingCategories = false, error = res.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun onImagePicked(imageBytes: ByteArray) {
            viewModelScope.launch {
                _uiState.update { it.copy(isUploadingImage = true, error = null) }
                when (val res = civicRepository.uploadReportImage(imageBytes)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isUploadingImage = false, imageUrl = res.data, error = null)
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isUploadingImage = false, error = res.message)
                        }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun captureLocation() {
            viewModelScope.launch {
                val granted = locationRepository.requestLocationPermission()
                if (!granted) {
                    _uiState.update { it.copy(error = LOCATION_PERMISSION_ERROR) }
                    return@launch
                }
                val location = locationRepository.getCurrentLocation()
                if (location == null) {
                    _uiState.update { it.copy(error = LOCATION_UNAVAILABLE_ERROR) }
                    return@launch
                }
                _uiState.update {
                    it.copy(latitude = location.latitude, longitude = location.longitude, error = null)
                }
            }
        }

        fun submit(
            categoryId: String?,
            title: String,
            description: String,
            locationDescription: String?,
            severity: String,
            isAnonymous: Boolean,
        ) {
            val categoryMissing = categoryId.isNullOrBlank()
            val titleInvalid = title.isBlank()
            val descriptionInvalid = description.isBlank()
            if (categoryMissing || titleInvalid || descriptionInvalid) {
                _uiState.update {
                    it.copy(categoryError = categoryMissing, titleError = titleInvalid, descriptionError = descriptionInvalid)
                }
                return
            }

            // Check for duplicates before submitting
            viewModelScope.launch {
                _uiState.update { it.copy(isCheckingDuplicates = true, error = null) }
                when (val res = civicAiProvider.checkDuplicates(title.trim(), description.trim())) {
                    is Resource.Success -> {
                        val duplicates = res.data
                        if (duplicates.matches.isNotEmpty()) {
                            // Show duplicates to user and store pending submission
                            _uiState.update {
                                it.copy(
                                    isCheckingDuplicates = false,
                                    duplicateCheck = duplicates,
                                    pendingSubmission =
                                        PendingSubmission(
                                            categoryId = categoryId!!,
                                            title = title.trim(),
                                            description = description.trim(),
                                            locationDescription = locationDescription?.trim()?.takeIf { it.isNotEmpty() },
                                            severity = severity,
                                            isAnonymous = isAnonymous,
                                        ),
                                )
                            }
                        } else {
                            // No duplicates, proceed with submission
                            _uiState.update { it.copy(isCheckingDuplicates = false, duplicateCheck = null) }
                            doSubmit(categoryId!!, title.trim(), description.trim(), locationDescription, severity, isAnonymous)
                        }
                    }

                    is Resource.Error -> {
                        // On error, proceed with submission anyway (dedup is optional)
                        _uiState.update { it.copy(isCheckingDuplicates = false, error = null) }
                        doSubmit(categoryId!!, title.trim(), description.trim(), locationDescription, severity, isAnonymous)
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun proceedWithSubmission() {
            val pending = _uiState.value.pendingSubmission ?: return
            _uiState.update { it.copy(duplicateCheck = null, pendingSubmission = null) }
            doSubmit(
                pending.categoryId,
                pending.title,
                pending.description,
                pending.locationDescription,
                pending.severity,
                pending.isAnonymous,
            )
        }

        fun dismissDuplicates() {
            _uiState.update { it.copy(duplicateCheck = null, pendingSubmission = null) }
        }

        private fun doSubmit(
            categoryId: String,
            title: String,
            description: String,
            locationDescription: String?,
            severity: String,
            isAnonymous: Boolean,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true, error = null) }
                val state = _uiState.value
                when (
                    val res =
                        civicRepository.submitReport(
                            categoryId = categoryId,
                            title = title,
                            description = description,
                            latitude = state.latitude,
                            longitude = state.longitude,
                            locationDescription = locationDescription?.takeIf { it.isNotEmpty() },
                            severity = severity,
                            isAnonymous = isAnonymous,
                            imageUrl = state.imageUrl,
                        )
                ) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isSubmitting = false, result = res.data, error = null) }
                        requestTriage(res.data.reportId)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isSubmitting = false, error = res.message) }
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        private fun requestTriage(reportId: String) {
            viewModelScope.launch {
                when (val res = civicAiProvider.triageReport(reportId)) {
                    is Resource.Success -> _uiState.update { it.copy(aiTriaged = res.data.transitioned) }
                    else -> Unit
                }
            }
        }

        fun clearFieldErrors() =
            _uiState.update {
                it.copy(categoryError = false, titleError = false, descriptionError = false)
            }

        fun dismissError() = _uiState.update { it.copy(error = null) }

        fun consumeResult() = _uiState.update { it.copy(result = null) }

        fun startVoiceRecording() {
            viewModelScope.launch {
                try {
                    audioRecorder.startRecording()
                    _uiState.update { it.copy(isRecording = true, error = null) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "audio_permission_denied") }
                }
            }
        }

        fun stopVoiceRecording(onTranscript: (String) -> Unit) {
            viewModelScope.launch {
                val result = audioRecorder.stopRecording()
                _uiState.update { it.copy(isRecording = false) }

                if (result != null) {
                    transcribeAudio(result.filePath, onTranscript)
                } else {
                    _uiState.update { it.copy(error = "audio_recording_failed") }
                }
            }
        }

        fun cancelVoiceRecording() {
            viewModelScope.launch {
                audioRecorder.cancelRecording()
                _uiState.update { it.copy(isRecording = false) }
            }
        }

        private suspend fun transcribeAudio(
            filePath: String,
            onTranscript: (String) -> Unit,
        ) {
            _uiState.update { it.copy(isTranscribing = true, error = null) }

            try {
                val file = File(filePath)
                val audioBytes = file.readBytes()
                val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
                val filename = file.name

                when (val res = civicAiProvider.transcribeAudio(audioBase64, filename)) {
                    is Resource.Success -> {
                        val transcript = res.data.transcript
                        onTranscript(transcript)
                        _uiState.update { it.copy(isTranscribing = false) }
                        file.delete()
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isTranscribing = false, error = res.message) }
                        file.delete()
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isTranscribing = false, error = "transcription_failed") }
                File(filePath).delete()
            }
        }

        private companion object {
            const val LOCATION_PERMISSION_ERROR = "location_permission_denied"
            const val LOCATION_UNAVAILABLE_ERROR = "location_unavailable"
        }
    }
