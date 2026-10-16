package com.smartjirani.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private object Keys {
            val USER_ID = stringPreferencesKey("user_id")
            val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
            val SELECTED_NEIGHBORHOOD_ID = stringPreferencesKey("selected_neighborhood_id")
            val SELECTED_NEIGHBORHOOD_NAME = stringPreferencesKey("selected_neighborhood_name")
            val LANGUAGE = stringPreferencesKey("language")
            val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
            val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        }

        val userPreferences: Flow<UserPreferences> =
            context.dataStore.data.map { prefs ->
                UserPreferences(
                    userId = prefs[Keys.USER_ID],
                    isOnboarded = prefs[Keys.IS_ONBOARDED] ?: false,
                    selectedNeighborhoodId = prefs[Keys.SELECTED_NEIGHBORHOOD_ID],
                    selectedNeighborhoodName = prefs[Keys.SELECTED_NEIGHBORHOOD_NAME],
                    language = prefs[Keys.LANGUAGE] ?: "en",
                    isDarkMode = prefs[Keys.IS_DARK_MODE] ?: false,
                    appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
                )
            }

        suspend fun setUserId(userId: String) {
            context.dataStore.edit { it[Keys.USER_ID] = userId }
        }

        suspend fun setOnboarded(onboarded: Boolean) {
            context.dataStore.edit { it[Keys.IS_ONBOARDED] = onboarded }
        }

        suspend fun setSelectedNeighborhood(
            neighborhoodId: String,
            neighborhoodName: String? = null,
        ) {
            context.dataStore.edit {
                it[Keys.SELECTED_NEIGHBORHOOD_ID] = neighborhoodId
                if (neighborhoodName != null) {
                    it[Keys.SELECTED_NEIGHBORHOOD_NAME] = neighborhoodName
                }
            }
        }

        suspend fun clearSelectedNeighborhood() {
            context.dataStore.edit {
                it.remove(Keys.SELECTED_NEIGHBORHOOD_ID)
                it.remove(Keys.SELECTED_NEIGHBORHOOD_NAME)
            }
        }

        suspend fun setLanguage(language: String) {
            context.dataStore.edit { it[Keys.LANGUAGE] = language }
        }

        suspend fun setAppLockEnabled(enabled: Boolean) {
            context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
        }

        suspend fun clearAll() {
            context.dataStore.edit { it.clear() }
        }
    }
