package com.smartjirani.feature.neighborhood

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.database.dao.NeighborhoodDao
import com.smartjirani.core.database.entity.NeighborhoodEntity
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class NeighborhoodDto(
    val id: String,
    val name: String,
    val city: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val created_at: String,
    val updated_at: String,
)

@Serializable
data class NewMembershipDto(
    val user_id: String,
    val neighborhood_id: String,
)

@Serializable
data class MembershipRefDto(
    val id: String,
    val user_id: String,
    val neighborhood_id: String,
)

@Singleton
class NeighborhoodRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
        private val neighborhoodDao: NeighborhoodDao,
        private val preferencesRepository: UserPreferencesRepository,
    ) : NeighborhoodRepository {
        override suspend fun getNeighborhoods(): Resource<List<Neighborhood>> =
            safeCall {
                val dtos =
                    supabaseClient.postgrest
                        .from("neighborhoods")
                        .select()
                        .decodeList<NeighborhoodDto>()
                val neighborhoods = dtos.map { it.toDomain() }
                neighborhoodDao.insertNeighborhoods(neighborhoods.map { it.toEntity() })
                neighborhoods
            }

        override suspend fun getNearbyNeighborhoods(
            latitude: Double,
            longitude: Double,
        ): Resource<List<Neighborhood>> =
            safeCall {
                val dtos =
                    supabaseClient.postgrest
                        .from("neighborhoods")
                        .select()
                        .decodeList<NeighborhoodDto>()
                dtos.map { it.toDomain() }.sortedBy { n ->
                    val latDiff = n.latitude - latitude
                    val lonDiff = n.longitude - longitude
                    Math.sqrt(latDiff * latDiff + lonDiff * lonDiff)
                }
            }

        override suspend fun joinNeighborhood(neighborhoodId: String): Resource<NeighborhoodMembership> =
            safeCall {
                val userId =
                    supabaseClient.auth
                        .currentSessionOrNull()
                        ?.user
                        ?.id
                        ?: error("Not authenticated")
                // Every feed/hub query is RLS-gated on this membership row, so the join
                // must land in the database — not just in local preferences.
                val insert =
                    runCatching {
                        supabaseClient.postgrest
                            .from("neighborhood_members")
                            .insert(NewMembershipDto(user_id = userId, neighborhood_id = neighborhoodId))
                    }
                val alreadyMember =
                    insert
                        .exceptionOrNull()
                        ?.message
                        ?.contains(Regex("(?i)duplicate key|23505")) == true
                if (insert.isFailure && !alreadyMember) throw insert.exceptionOrNull()!!
                // The shell's location chip shows this name, so resolve it while we are joining.
                val cachedName = neighborhoodDao.getNeighborhoodById(neighborhoodId).first()?.name
                val neighborhoodName =
                    cachedName ?: runCatching {
                        supabaseClient.postgrest
                            .from("neighborhoods")
                            .select { filter { eq("id", neighborhoodId) } }
                            .decodeList<NeighborhoodDto>()
                            .firstOrNull()
                            ?.name
                    }.getOrNull()
                preferencesRepository.setSelectedNeighborhood(neighborhoodId, neighborhoodName)
                val membershipId =
                    runCatching {
                        supabaseClient.postgrest
                            .from("neighborhood_members")
                            .select(Columns.raw("id,user_id,neighborhood_id")) {
                                filter {
                                    eq("user_id", userId)
                                    eq("neighborhood_id", neighborhoodId)
                                }
                            }.decodeList<MembershipRefDto>()
                            .firstOrNull()
                            ?.id
                    }.getOrNull()
                NeighborhoodMembership(
                    id = membershipId ?: "mem_${System.currentTimeMillis()}",
                    userId = userId,
                    neighborhoodId = neighborhoodId,
                )
            }

        override suspend fun getMyNeighborhoods(): Resource<List<Neighborhood>> =
            safeCall {
                val userId =
                    supabaseClient.auth
                        .currentSessionOrNull()
                        ?.user
                        ?.id
                        ?: return@safeCall emptyList()
                val memberships =
                    supabaseClient.postgrest
                        .from("neighborhood_members")
                        .select(Columns.raw("id,user_id,neighborhood_id")) {
                            filter { eq("user_id", userId) }
                        }.decodeList<MembershipRefDto>()
                if (memberships.isEmpty()) return@safeCall emptyList()
                supabaseClient.postgrest
                    .from("neighborhoods")
                    .select { filter { isIn("id", memberships.map { it.neighborhood_id }) } }
                    .decodeList<NeighborhoodDto>()
                    .map { it.toDomain() }
            }

        private fun NeighborhoodDto.toDomain() =
            Neighborhood(
                id = id,
                name = name,
                city = city,
                region = region,
                country = country,
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )

        private fun Neighborhood.toEntity() =
            NeighborhoodEntity(
                id = id,
                name = name,
                city = city,
                region = region,
                country = country,
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
    }
