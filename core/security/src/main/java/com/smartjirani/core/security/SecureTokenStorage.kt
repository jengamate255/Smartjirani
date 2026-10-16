package com.smartjirani.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenStorage
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        private val prefs =
            EncryptedSharedPreferences.create(
                context,
                "smartjirani_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

        fun saveAccessToken(token: String) {
            prefs.edit().putString("access_token", token).apply()
        }

        fun getAccessToken(): String? = prefs.getString("access_token", null)

        fun saveRefreshToken(token: String) {
            prefs.edit().putString("refresh_token", token).apply()
        }

        fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

        fun saveUserId(userId: String) {
            prefs.edit().putString("user_id", userId).apply()
        }

        fun getUserId(): String? = prefs.getString("user_id", null)

        fun clearAll() {
            prefs.edit().clear().apply()
        }
    }
