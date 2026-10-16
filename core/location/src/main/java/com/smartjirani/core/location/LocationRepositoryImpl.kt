package com.smartjirani.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LocationRepository {
        private val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        private val _currentLocation = MutableStateFlow<LocationData?>(null)
        override val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()

        override fun hasLocationPermission(): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

        override suspend fun requestLocationPermission(): Boolean = hasLocationPermission()

        override suspend fun getCurrentLocation(): LocationData? {
            if (!hasLocationPermission()) return null

            return try {
                suspendCancellableCoroutine { cont ->
                    val locationRequest =
                        LocationRequest
                            .Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000L)
                            .setMaxUpdates(1)
                            .build()

                    val callback =
                        object : LocationCallback() {
                            override fun onLocationResult(result: LocationResult) {
                                val location = result.lastLocation
                                if (location != null) {
                                    val data = LocationData(location.latitude, location.longitude)
                                    _currentLocation.value = data
                                    cont.resume(data)
                                } else {
                                    cont.resume(null)
                                }
                                fusedLocationClient.removeLocationUpdates(this)
                            }
                        }

                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        callback,
                        Looper.getMainLooper(),
                    )

                    cont.invokeOnCancellation {
                        fusedLocationClient.removeLocationUpdates(callback)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }
