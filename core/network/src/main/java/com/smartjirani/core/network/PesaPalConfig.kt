package com.smartjirani.core.network

/**
 * PesaPal v3 checkout credentials.
 *
 * Populated from `local.properties` (`PESAPAL_CONSUMER_KEY`, `PESAPAL_CONSUMER_SECRET`,
 * `PESAPAL_ENV`, `PESAPAL_CALLBACK_URL`) via BuildConfig in the app module. When the key
 * is blank the gateway falls back to a simulated confirmation so the harambee flow stays
 * usable in development.
 */
data class PesaPalConfig(
    val consumerKey: String,
    val consumerSecret: String,
    val environment: String = "sandbox",
    val callbackUrl: String = "",
) {
    val isConfigured: Boolean
        get() = consumerKey.isNotBlank() && consumerSecret.isNotBlank()

    val baseUrl: String
        get() =
            if (environment.equals("live", ignoreCase = true)) {
                "https://secure.pesapal.com/v3/api"
            } else {
                "https://sandbox.pesapal.com/v3/api"
            }
}
