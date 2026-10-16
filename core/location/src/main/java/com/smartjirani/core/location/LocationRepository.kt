package com.smartjirani.core.location

import kotlinx.coroutines.flow.StateFlow

data class LocationData(
    val latitude: Double,
    val longitude: Double,
)

interface LocationRepository {
    val currentLocation: StateFlow<LocationData?>

    suspend fun getCurrentLocation(): LocationData?

    suspend fun requestLocationPermission(): Boolean

    fun hasLocationPermission(): Boolean
}
