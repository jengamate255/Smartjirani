package com.smartjirani.feature.ujamaa

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class NeighborhoodGeoDto(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
)

@Singleton
class MtaaMapRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
    ) : MtaaMapRepository {
        override suspend fun getNeighborhoods(): Resource<List<MtaaPin>> =
            safeCall {
                supabaseClient.postgrest
                    .from("neighborhoods")
                    .select(Columns.raw("id,name,latitude,longitude,radius"))
                    .decodeList<NeighborhoodGeoDto>()
                    .map {
                        MtaaPin(
                            id = it.id,
                            name = it.name,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            radiusMeters = it.radius,
                        )
                    }
            }
    }
