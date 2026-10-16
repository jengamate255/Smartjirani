package com.smartjirani.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartjirani.core.database.dao.NotificationDao
import com.smartjirani.core.database.entity.NotificationEntity
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.notifications.FcmTokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmartJiraniFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var notificationDao: NotificationDao

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "SmartJirani"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        val type = remoteMessage.data["type"] ?: "general"
        val referenceId = remoteMessage.data["reference_id"]

        // Save to local database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // DataStore holds the signed-in uid even when this service starts
                // a fresh process before the auth SDK finishes restoring its session.
                val userId = userPreferencesRepository.userPreferences.first().userId
                if (userId.isNullOrBlank()) return@launch
                val notification =
                    NotificationEntity(
                        id = referenceId ?: System.currentTimeMillis().toString(),
                        userId = userId,
                        type = type,
                        title = title,
                        body = body,
                        referenceId = referenceId,
                        isRead = false,
                        createdAt = System.currentTimeMillis(),
                    )
                notificationDao.insertNotifications(listOf(notification))
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }

        // Show system notification
        showNotification(title, body, type, referenceId)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Push token rotated: re-upload it for the signed-in user, otherwise
        // the backend keeps sending to the stale token and notifications die.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = userPreferencesRepository.userPreferences.first().userId
                if (!userId.isNullOrBlank()) fcmTokenManager.registerToken(userId)
            } catch (e: Exception) {
                // Best effort; next app session re-registers via AuthViewModel.
            }
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        referenceId: String?,
    ) {
        val channelId = "smartjirani_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    channelId,
                    "SmartJirani Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat
                .Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
