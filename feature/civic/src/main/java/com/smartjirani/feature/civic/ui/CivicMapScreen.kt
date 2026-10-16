package com.smartjirani.feature.civic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mapbox.common.MapboxOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartEmptyState
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.feature.civic.BuildConfig
import com.smartjirani.feature.civic.CivicReport
import com.smartjirani.feature.civic.R
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

private const val STYLE_URI = "mapbox://styles/mapbox/streets-v12"
private const val SOURCE_ID = "civic-reports-geo"
private const val POINT_LAYER = "civic-report-points"

private val DefaultCenter =
    com.mapbox.geojson.Point
        .fromLngLat(39.2083, -6.7924)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicMapRoute(
    onNavigateBack: () -> Unit,
    viewModel: CivicMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    CivicMapScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetry = viewModel::load,
        onDismissError = viewModel::dismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicMapScreen(
    uiState: CivicMapUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.civic_map_title)) },
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
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    SmartLoading()
                }

                uiState.reports.isEmpty() -> {
                    SmartEmptyState(
                        title = stringResource(R.string.civic_map_empty),
                        message = stringResource(R.string.civic_map_hint),
                    )
                }

                else -> {
                    CivicMapContent(
                        uiState = uiState,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            uiState.error?.let { message ->
                ErrorBanner(
                    message = message,
                    onDismiss = onDismissError,
                    onRetry = onRetry,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (uiState.reports.isNotEmpty()) {
                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text =
                                stringResource(
                                    R.string.civic_map_stats_format,
                                    uiState.reports.size,
                                    uiState.overdue,
                                    uiState.escalated,
                                ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.civic_map_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CivicMapContent(
    uiState: CivicMapUiState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val camera =
        remember(uiState.reports) {
            val focus = uiState.reports.firstOrNull { it.latitude != null && it.longitude != null }
            CameraOptions
                .Builder()
                .center(
                    if (focus != null) {
                        com.mapbox.geojson.Point
                            .fromLngLat(focus.longitude!!, focus.latitude!!)
                    } else {
                        DefaultCenter
                    },
                ).zoom(if (uiState.reports.size == 1) 15.0 else 12.0)
                .build()
        }

    val geoJson =
        remember(uiState.reports) {
            buildCivicPointsGeoJson(uiState.reports)
        }

    val destroyed = remember { AtomicBoolean(false) }
    val mapView =
        remember {
            if (BuildConfig.MAPBOX_ACCESS_TOKEN.isNotEmpty()) {
                MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
            }
            MapView(context, MapInitOptions(context, cameraOptions = camera))
        }

    LaunchedEffect(camera) {
        mapView.getMapboxMap().setCamera(camera)
    }

    LaunchedEffect(geoJson) {
        if (BuildConfig.MAPBOX_ACCESS_TOKEN.isEmpty()) return@LaunchedEffect
        mapView.getMapboxMap().loadStyle(
            style(STYLE_URI) {
                +geoJsonSource(SOURCE_ID) {
                    data(geoJson)
                }
                +circleLayer(POINT_LAYER, SOURCE_ID) {
                    circleColor(Expression.get("color"))
                    circleRadius(7.0)
                    circleStrokeColor(android.graphics.Color.WHITE)
                    circleStrokeWidth(2.0)
                    circleOpacity(0.95)
                }
            },
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> destroyMap(mapView, destroyed)
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            destroyMap(mapView, destroyed)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

private fun destroyMap(
    mapView: MapView,
    destroyed: AtomicBoolean,
) {
    if (destroyed.compareAndSet(false, true)) {
        mapView.onStop()
        mapView.onDestroy()
    }
}

/** One point per report; color carries the civic status palette hex. */
private fun buildCivicPointsGeoJson(reports: List<CivicReport>): String {
    val features =
        reports.mapNotNull { report ->
            val lat = report.latitude ?: return@mapNotNull null
            val lng = report.longitude ?: return@mapNotNull null
            val color = String.format(Locale.US, "#%06X", 0xFFFFFF and civicStatusColor(report.status).toArgb())
            val ref = report.publicReference.replace("\\", "\\\\").replace("\"", "\\\"")
            val lon = String.format(Locale.US, "%.6f", lng)
            val latStr = String.format(Locale.US, "%.6f", lat)
            """{"type":"Feature","properties":{"color":"$color","ref":"$ref"},"geometry":{"type":"Point","coordinates":[$lon, $latStr]}}"""
        }
    return """{"type":"FeatureCollection","features":[${features.joinToString(separator = ",")}]}"""
}
