package com.smartjirani.feature.civic.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.feature.civic.CivicSubmitResult
import com.smartjirani.feature.civic.R
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicNewReportRoute(
    onNavigateBack: () -> Unit,
    viewModel: CivicNewReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var successResult by remember { mutableStateOf<CivicSubmitResult?>(null) }

    LaunchedEffect(uiState.result) {
        uiState.result?.let {
            successResult = it
            viewModel.consumeResult()
        }
    }

    successResult?.let { result ->
        AlertDialog(
            onDismissRequest = {
                successResult = null
                onNavigateBack()
            },
            title = { Text(stringResource(R.string.civic_new_success_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.civic_new_success_ref, result.publicReference))
                    if (uiState.aiTriaged) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.civic_ai_triage_done),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (result.merged && result.canonicalReference != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.civic_new_merged_notice, result.canonicalReference),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    successResult = null
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.civic_new_done))
                }
            },
        )
        return
    }

    // Show duplicate warning dialog
    uiState.duplicateCheck?.let { dedup ->
        if (dedup.matches.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDuplicates,
                title = { Text(stringResource(R.string.civic_dedup_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.civic_dedup_message))
                        Spacer(modifier = Modifier.height(12.dp))
                        dedup.matches.take(3).forEach { match ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = match.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = match.publicReference,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = stringResource(R.string.civic_dedup_similarity, (match.similarity * 100).toInt()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = viewModel::proceedWithSubmission) {
                        Text(stringResource(R.string.civic_dedup_proceed))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDuplicates) {
                        Text(stringResource(R.string.civic_dedup_cancel))
                    }
                },
            )
            return
        }
    }

    CivicNewReportScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onImagePicked = viewModel::onImagePicked,
        onCaptureLocation = viewModel::captureLocation,
        onSubmit = viewModel::submit,
        onDismissError = viewModel::dismissError,
        onClearFieldErrors = viewModel::clearFieldErrors,
        onStartRecording = viewModel::startVoiceRecording,
        onStopRecording = viewModel::stopVoiceRecording,
        onCancelRecording = viewModel::cancelVoiceRecording,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CivicNewReportScreen(
    uiState: CivicNewReportUiState,
    onNavigateBack: () -> Unit,
    onImagePicked: (ByteArray) -> Unit,
    onCaptureLocation: () -> Unit,
    onSubmit: (String?, String, String, String?, String, Boolean) -> Unit,
    onDismissError: () -> Unit,
    onClearFieldErrors: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: ((String) -> Unit) -> Unit,
    onCancelRecording: () -> Unit,
) {
    val context = LocalContext.current
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var locationDescription by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("MEDIUM") }
    var isAnonymous by remember { mutableStateOf(false) }

    val photoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri: Uri? ->
            if (uri != null) {
                readAndCompressImage(context, uri)?.let(onImagePicked)
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.civic_new_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.civic_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            uiState.error?.let { message ->
                ErrorBanner(
                    message = civicFriendlyError(message),
                    onDismiss = onDismissError,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Text(
                text = stringResource(R.string.civic_new_category),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.isLoadingCategories) {
                SmartLoading()
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = {
                                selectedCategoryId = category.id
                                onClearFieldErrors()
                            },
                            label = { Text(category.nameSw.ifBlank { category.nameEn }) },
                        )
                    }
                }
            }
            if (uiState.categoryError) {
                Text(
                    text = stringResource(R.string.civic_new_error_category),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    onClearFieldErrors()
                },
                label = { Text(stringResource(R.string.civic_new_title_hint)) },
                isError = uiState.titleError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (uiState.titleError) {
                Text(
                    text = stringResource(R.string.civic_new_error_title),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    onClearFieldErrors()
                },
                label = { Text(stringResource(R.string.civic_new_description_hint)) },
                isError = uiState.descriptionError,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            if (uiState.descriptionError) {
                Text(
                    text = stringResource(R.string.civic_new_error_description),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        if (uiState.isRecording) {
                            onStopRecording { transcript ->
                                description =
                                    if (description.isBlank()) {
                                        transcript
                                    } else {
                                        "$description\n$transcript"
                                    }
                            }
                        } else {
                            onStartRecording()
                        }
                    },
                    enabled = !uiState.isTranscribing,
                ) {
                    if (uiState.isTranscribing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.civic_voice_transcribing))
                    } else if (uiState.isRecording) {
                        Text(stringResource(R.string.civic_voice_stop))
                    } else {
                        Text(stringResource(R.string.civic_voice_record))
                    }
                }
                if (uiState.isRecording) {
                    Spacer(modifier = Modifier.size(8.dp))
                    TextButton(onClick = onCancelRecording) {
                        Text(stringResource(R.string.civic_voice_cancel))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    enabled = !uiState.isUploadingImage,
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) {
                    Text(stringResource(R.string.civic_new_add_photo))
                }
                Spacer(modifier = Modifier.size(12.dp))
                if (uiState.isUploadingImage) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else if (uiState.imageUrl != null) {
                    Text(
                        text = stringResource(R.string.civic_new_photo_added),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            uiState.imageUrl?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = url,
                    contentDescription = stringResource(R.string.civic_detail_photo),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onCaptureLocation) {
                    Text(stringResource(R.string.civic_new_use_gps))
                }
                Spacer(modifier = Modifier.size(12.dp))
                if (uiState.locationSet) {
                    Text(
                        text = stringResource(R.string.civic_new_location_set),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = locationDescription,
                onValueChange = { locationDescription = it },
                label = { Text(stringResource(R.string.civic_new_location_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.civic_new_severity),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "LOW" to R.string.civic_severity_low,
                    "MEDIUM" to R.string.civic_severity_medium,
                    "HIGH" to R.string.civic_severity_high,
                    "CRITICAL" to R.string.civic_severity_critical,
                ).forEach { (value, labelRes) ->
                    FilterChip(
                        selected = severity == value,
                        onClick = { severity = value },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.civic_new_anonymous),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = isAnonymous, onCheckedChange = { isAnonymous = it })
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                enabled = !uiState.isSubmitting && !uiState.isUploadingImage,
                onClick = {
                    onSubmit(selectedCategoryId, title, description, locationDescription, severity, isAnonymous)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.civic_new_submit))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Maps server/validation error tokens to Swahili copy; unknown messages pass through. */
internal fun civicFriendlyError(raw: String): String =
    when (raw) {
        "location_permission_denied" -> "Ruhusa ya mahali imekataliwa"
        "location_unavailable" -> "Mahali hapapatikani kwa sasa"
        "rate_limited" -> "Umetuma ripoti nyingi leo — jaribu kesho"
        "invalid_category" -> "Aina ya tatizo si sahihi"
        "invalid_severity" -> "Umuhimu si sahihi"
        "organization_unresolved" -> "Mtaa wako haujapangwa kwa serikali — wasiliana na admin"
        "unauthenticated" -> "Tafadhali ingia kwanza"
        else -> raw
    }

/** Decodes, downscales (max ~1600px) and JPEG-compresses a picked image for upload. */
private fun readAndCompressImage(
    context: Context,
    uri: Uri,
): ByteArray? =
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap =
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
        bitmap.recycle()
        output.toByteArray()
    }.getOrNull()
