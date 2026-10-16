package com.smartjirani.core.datastore

data class UserPreferences(
    val userId: String? = null,
    val isOnboarded: Boolean = false,
    val selectedNeighborhoodId: String? = null,
    /** Display name of the selected mtaa, used by the location chip in the app shell. */
    val selectedNeighborhoodName: String? = null,
    val language: String = "en",
    val isDarkMode: Boolean = false,
    /** Opt-in: require biometric unlock whenever the app returns to the foreground. */
    val appLockEnabled: Boolean = false,
)
