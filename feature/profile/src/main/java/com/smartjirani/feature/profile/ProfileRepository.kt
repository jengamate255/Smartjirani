package com.smartjirani.feature.profile

import com.smartjirani.core.common.result.Resource

data class UserProfile(
    val id: String,
    val displayName: String,
    val username: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val neighborhoodId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

interface ProfileRepository {
    suspend fun getProfile(userId: String): Resource<UserProfile>

    suspend fun getCurrentProfile(): Resource<UserProfile>

    suspend fun createProfile(profile: UserProfile): Resource<UserProfile>

    suspend fun updateProfile(profile: UserProfile): Resource<UserProfile>

    suspend fun uploadAvatar(
        userId: String,
        imageBytes: ByteArray,
    ): Resource<String>
}
