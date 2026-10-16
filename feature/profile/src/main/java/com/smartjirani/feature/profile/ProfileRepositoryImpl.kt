package com.smartjirani.feature.profile

import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.common.result.safeCall
import com.smartjirani.core.database.dao.ProfileDao
import com.smartjirani.core.database.entity.ProfileEntity
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ProfileDto(
    val id: String,
    val display_name: String,
    val username: String,
    val phone_number: String? = null,
    val email: String? = null,
    val avatar_url: String? = null,
    val bio: String? = null,
    val neighborhood_id: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val created_at: String,
    val updated_at: String,
)

@Singleton
class ProfileRepositoryImpl
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
        private val profileDao: ProfileDao,
        private val preferencesRepository: UserPreferencesRepository,
    ) : ProfileRepository {
        override suspend fun getProfile(userId: String): Resource<UserProfile> =
            safeCall {
                val dto =
                    supabaseClient.postgrest
                        .from("profiles")
                        .select { filter { eq("id", userId) } }
                        .decodeList<ProfileDto>()
                        .firstOrNull() ?: throw Exception("Profile not found")

                val profile = dto.toDomain()
                profileDao.insertProfile(profile.toEntity())
                profile
            }

        override suspend fun getCurrentProfile(): Resource<UserProfile> =
            safeCall {
                val userId =
                    supabaseClient.auth
                        .currentSessionOrNull()
                        ?.user
                        ?.id
                        ?: throw Exception("Not authenticated")

                val dto =
                    supabaseClient.postgrest
                        .from("profiles")
                        .select { filter { eq("id", userId) } }
                        .decodeList<ProfileDto>()
                        .firstOrNull()

                dto?.toDomain() ?: throw Exception("Profile not found")
            }

        override suspend fun createProfile(profile: UserProfile): Resource<UserProfile> =
            safeCall {
                val dto = profile.toDto()
                supabaseClient.postgrest.from("profiles").insert(dto)
                profile
            }

        override suspend fun updateProfile(profile: UserProfile): Resource<UserProfile> =
            safeCall {
                val dto = profile.toDto()
                supabaseClient.postgrest
                    .from("profiles")
                    .update(dto) { filter { eq("id", profile.id) } }
                profileDao.insertProfile(profile.toEntity())
                profile
            }

        override suspend fun uploadAvatar(
            userId: String,
            imageBytes: ByteArray,
        ): Resource<String> =
            safeCall {
                val path = "avatars/$userId/avatar.jpg"
                supabaseClient.storage.from("profiles").upload(path, imageBytes)
                supabaseClient.storage.from("profiles").publicUrl(path)
            }

        private fun ProfileDto.toDomain() =
            UserProfile(
                id = id,
                displayName = display_name,
                username = username,
                phoneNumber = phone_number,
                email = email,
                avatarUrl = avatar_url,
                bio = bio,
                neighborhoodId = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )

        private fun UserProfile.toDto() =
            ProfileDto(
                id = id,
                display_name = displayName,
                username = username,
                phone_number = phoneNumber,
                email = email,
                avatar_url = avatarUrl,
                bio = bio,
                neighborhood_id = neighborhoodId,
                created_at = "",
                updated_at = "",
            )

        private fun UserProfile.toEntity() =
            ProfileEntity(
                id = id,
                displayName = displayName,
                username = username,
                phoneNumber = phoneNumber,
                email = email,
                avatarUrl = avatarUrl,
                bio = bio,
                neighborhoodId = neighborhoodId,
                latitude = latitude,
                longitude = longitude,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
    }
