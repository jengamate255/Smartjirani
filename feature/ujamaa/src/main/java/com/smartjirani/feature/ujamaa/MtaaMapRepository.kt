package com.smartjirani.feature.ujamaa

import com.smartjirani.core.common.result.Resource

/**
 * A neighborhood as the map needs it: enough to drop a pin and draw the
 * mtaa boundary circle.
 */
data class MtaaPin(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
)

interface MtaaMapRepository {
    suspend fun getNeighborhoods(): Resource<List<MtaaPin>>
}
