package com.smartjirani.feature.ujamaa.ui

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
import androidx.compose.ui.graphics.Color
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
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartEmptyState
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.feature.ujamaa.BuildConfig
import com.smartjirani.feature.ujamaa.MtaaPin
import com.smartjirani.feature.ujamaa.R
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.cos
import kotlin.math.sin

private const val STYLE_URI = "mapbox://styles/mapbox/streets-v12"
private const val SOURCE_ID = "mtaa-geo"
private const val FILL_SELECTED = "mtaa-fill-selected"
private const val FILL_OTHER = "mtaa-fill-other"
private const val LABEL_LAYER = "mtaa-label"

private val SelectedFill = Color(0xFF1E8E5A)
private val OtherFill = Color(0xFF8FA39B)
private val LabelColor = Color(0xFF14231D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MtaaMapRoute(
    onNavigateBack: () -> Unit,
    viewModel: MtaaMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    MtaaMapScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetry = viewModel::load,
        onDismissError = viewModel::dismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MtaaMapScreen(
    uiState: MtaaMapUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ujamaa_map_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.ujamaa_map_back),
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

                uiState.pins.isEmpty() -> {
                    SmartEmptyState(
                        title = stringResource(R.string.ujamaa_map_empty_title),
                        message = stringResource(R.string.ujamaa_map_empty_message),
                    )
                }

                else -> {
                    MtaaMapContent(
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

            if (uiState.pins.isNotEmpty()) {
                MtaaMapInfoCard(
                    uiState = uiState,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun MtaaMapInfoCard(
    uiState: MtaaMapUiState,
    modifier: Modifier = Modifier,
) {
    val focus = uiState.selectedPin ?: uiState.pins.firstOrNull() ?: return
    val label =
        uiState.selectedPin?.name
            ?: uiState.pins.firstOrNull()?.name
            ?: uiState.locationLabel

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.ujamaa_map_selected_format, label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text =
                    stringResource(
                        R.string.ujamaa_map_radius_format,
                        focus.radiusMeters.toInt(),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MtaaMapContent(
    uiState: MtaaMapUiState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val camera =
        remember(uiState.selectedNeighborhoodId, uiState.pins) {
            val focus = uiState.selectedPin ?: uiState.pins.first()
            CameraOptions
                .Builder()
                .center(
                    com.mapbox.geojson.Point
                        .fromLngLat(focus.longitude, focus.latitude),
                ).zoom(if (uiState.selectedPin != null) 14.0 else 12.5)
                .build()
        }

    val geoJson =
        remember(uiState.pins, uiState.selectedNeighborhoodId) {
            buildMtaaGeoJson(uiState.pins, uiState.selectedNeighborhoodId)
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
                +fillLayer(FILL_SELECTED, SOURCE_ID) {
                    filter(
                        Expression.eq(
                            Expression.get("sel"),
                            Expression.literal("yes"),
                        ),
                    )
                    fillAntialias(true)
                    fillColor(SelectedFill.toArgb())
                    fillOpacity(0.25)
                    fillOutlineColor(SelectedFill.toArgb())
                }
                +fillLayer(FILL_OTHER, SOURCE_ID) {
                    filter(
                        Expression.eq(
                            Expression.get("sel"),
                            Expression.literal("no"),
                        ),
                    )
                    fillAntialias(true)
                    fillColor(OtherFill.toArgb())
                    fillOpacity(0.18)
                    fillOutlineColor(OtherFill.toArgb())
                }
                +symbolLayer(LABEL_LAYER, SOURCE_ID) {
                    filter(
                        Expression.eq(
                            Expression.get("kind"),
                            Expression.literal("label"),
                        ),
                    )
                    textField(Expression.get("name"))
                    textSize(12.5)
                    textColor(LabelColor.toArgb())
                    textHaloColor(Color.White.toArgb())
                    textHaloWidth(1.8)
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

/**
 * GeoJSON with two feature kinds per neighborhood: a boundary polygon
 * ("area") and a centroid label point ("label"). Built as raw JSON to
 * avoid pulling Gson into the module.
 */
private fun buildMtaaGeoJson(
    pins: List<MtaaPin>,
    selectedId: String?,
): String {
    val features =
        pins.joinToString(separator = ",") { pin ->
            val selected = if (pin.id == selectedId) "yes" else "no"
            val name = escapeJson(pin.name)
            val area = """{"type":"Feature","properties":{"sel":"$selected","kind":"area","name":"$name"},"geometry":{"type":"Polygon","coordinates":[${boundaryRing(pin)}]}}"""
            val lon = String.format(Locale.US, "%.6f", pin.longitude)
            val lat = String.format(Locale.US, "%.6f", pin.latitude)
            val label = """{"type":"Feature","properties":{"sel":"$selected","kind":"label","name":"$name"},"geometry":{"type":"Point","coordinates":[$lon, $lat]}}"""
            "$area,$label"
        }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

/** Closed ring of 64 points approximating the mtaa radius circle. */
private fun boundaryRing(pin: MtaaPin): String {
    val segments = 64
    val latRad = Math.toRadians(pin.latitude)
    val cosLat = cos(latRad).coerceAtLeast(0.01)
    val dLat = pin.radiusMeters / 111_320.0
    val dLon = pin.radiusMeters / (111_320.0 * cosLat)

    val points = ArrayList<String>(segments + 1)
    for (i in 0 until segments) {
        val theta = 2.0 * Math.PI * i / segments
        val pLat = pin.latitude + dLat * sin(theta)
        val pLon = pin.longitude + dLon * cos(theta)
        points += String.format(Locale.US, "[%.6f, %.6f]", pLon, pLat)
    }
    points += points.first()
    return "[" + points.joinToString(separator = ",") + "]"
}

private fun escapeJson(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
