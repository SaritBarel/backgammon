package com.example.backgammon.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.backgammon.R
import com.example.backgammon.ui.GameListActivity
import com.example.backgammon.ui.OnlineGameActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class GameMessagingService : FirebaseMessagingService() {
    private val tag = "GameMessagingService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(tag, "From: ${remoteMessage.from}")

        // Check if message contains data payload
        remoteMessage.data.isNotEmpty().let {
            Log.d(tag, "Message data payload: ${remoteMessage.data}")
            
            val gameId = remoteMessage.data["gameId"]
            val title = remoteMessage.data["title"] ?: "Backgammon"
            val message = remoteMessage.data["message"] ?: "Your turn"
            
            // Handle notification differently based on type
            if (gameId != null) {
                // Game-specific notification - open the game
                sendGameNotification(title, message, gameId)
            } else {
                // General notification - open game list
                sendGeneralNotification(title, message)
            }
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d(tag, "Message Notification Body: ${it.body}")
            it.body?.let { body -> sendGeneralNotification(it.title ?: "Backgammon", body) }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(tag, "Refreshed token: $token")

        // Here you would send the new token to your server
        // For example, storing it in Firestore for the current user
    }

    private fun sendGameNotification(title: String, messageBody: String, gameId: String) {
        val intent = Intent(this, OnlineGameActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(OnlineGameActivity.EXTRA_GAME_ID, gameId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = getString(R.string.game_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Game Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun sendGeneralNotification(title: String, messageBody: String) {
        val intent = Intent(this, GameListActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
} 