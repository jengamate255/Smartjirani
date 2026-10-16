package com.smartjirani.core.notifications

import com.google.firebase.messaging.FirebaseMessaging
import com.smartjirani.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
    ) {
        suspend fun registerToken(userId: String) {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                supabaseClient.postgrest.from("profiles").update({
                    set("fcm_token", token)
                }) {
                    filter { eq("id", userId) }
                }
            } catch (_: Exception) {
            }
        }

        suspend fun clearToken(userId: String) {
            try {
                supabaseClient.postgrest.from("profiles").update({
                    setToNull("fcm_token")
                }) {
                    filter { eq("id", userId) }
                }
            } catch (_: Exception) {
            }
        }
    }
