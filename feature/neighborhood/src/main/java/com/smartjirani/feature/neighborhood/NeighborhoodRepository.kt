package com.smartjirani.feature.neighborhood

import com.smartjirani.core.common.result.Resource

data class Neighborhood(
    val id: String,
    val name: String,
    val city: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class NeighborhoodMembership(
    val id: String,
    val userId: String,
    val neighborhoodId: String,
    val role: String = "member",
    val createdAt: Long = System.currentTimeMillis(),
)

interface NeighborhoodRepository {
    suspend fun getNeighborhoods(): Resource<List<Neighborhood>>

    suspend fun getNearbyNeighborhoods(
        latitude: Double,
        longitude: Double,
    ): Resource<List<Neighborhood>>

    suspend fun joinNeighborhood(neighborhoodId: String): Resource<NeighborhoodMembership>

    suspend fun getMyNeighborhoods(): Resource<List<Neighborhood>>
}
