package com.smartjirani.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        companion object {
            const val CHANNEL_GENERAL = "smartjirani_general"
            const val CHANNEL_CALLS = "smartjirani_calls"
            const val CHANNEL_MESSAGES = "smartjirani_messages"
        }

        init {
            createChannels()
        }

        private fun createChannels() {
            val generalChannel =
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            val callsChannel =
                NotificationChannel(
                    CHANNEL_CALLS,
                    "Calls",
                    NotificationManager.IMPORTANCE_HIGH,
                )
            val messagesChannel =
                NotificationChannel(
                    CHANNEL_MESSAGES,
                    "Messages",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            notificationManager.createNotificationChannels(listOf(generalChannel, callsChannel, messagesChannel))
        }

        fun showNotification(
            title: String,
            body: String,
            channelId: String = CHANNEL_GENERAL,
        ) {
            val notification =
                NotificationCompat
                    .Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
