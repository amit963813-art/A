package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

/**
 * Foreground Service providing a supported lock-screen assistant activation button.
 * Respects Android lock-screen security (never bypasses PIN/biometrics) and gives the user
 * an explicit "Stop Service" button directly in the notification.
 */
class AssistantForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "assistant_lockscreen_channel"
        const val NOTIFICATION_ID = 4102
        const val ACTION_START_SERVICE = "com.example.action.START_ASSISTANT_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_ASSISTANT_SERVICE"
        const val EXTRA_ASSISTANT_NAME = "extra_assistant_name"
        const val EXTRA_WAKE_PHRASE = "extra_wake_phrase"
        const val EXTRA_SHOW_ON_LOCKSCREEN = "extra_show_on_lockscreen"
        const val EXTRA_TRIGGER_VOICE_ON_LAUNCH = "extra_trigger_voice_on_launch"

        fun startService(
            context: Context,
            assistantName: String,
            wakePhrase: String,
            showOnLockScreen: Boolean
        ) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_START_SERVICE
                putExtra(EXTRA_ASSISTANT_NAME, assistantName)
                putExtra(EXTRA_WAKE_PHRASE, wakePhrase)
                putExtra(EXTRA_SHOW_ON_LOCKSCREEN, showOnLockScreen)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val assistantName = intent?.getStringExtra(EXTRA_ASSISTANT_NAME)?.ifBlank { "Aarya" } ?: "Aarya"
        val wakePhrase = intent?.getStringExtra(EXTRA_WAKE_PHRASE)?.ifBlank { "Hey $assistantName" } ?: "Hey $assistantName"
        val showOnLockScreen = intent?.getBooleanExtra(EXTRA_SHOW_ON_LOCKSCREEN, true) ?: true

        createNotificationChannel(showOnLockScreen)
        val notification = buildNotification(assistantName, wakePhrase, showOnLockScreen)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            // If microphone foreground service permission is restricted on Android 14+ before mic grant,
            // stop gracefully without crashing.
            stopSelf()
        }

        return START_STICKY
    }

    private fun createNotificationChannel(showOnLockScreen: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                lockscreenVisibility = if (showOnLockScreen) {
                    Notification.VISIBILITY_PUBLIC
                } else {
                    Notification.VISIBILITY_SECRET
                }
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        assistantName: String,
        wakePhrase: String,
        showOnLockScreen: Boolean
    ): Notification {
        val talkIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRIGGER_VOICE_ON_LAUNCH, true)
        }
        val talkPendingIntent = PendingIntent.getActivity(
            this,
            101,
            talkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val visibility = if (showOnLockScreen) {
            NotificationCompat.VISIBILITY_PUBLIC
        } else {
            NotificationCompat.VISIBILITY_SECRET
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("$assistantName Assistant Ready")
            .setContentText("Tap \"Talk to $assistantName\" or say \"$wakePhrase\" inside the app")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(visibility)
            .setContentIntent(talkPendingIntent)
            .addAction(
                android.R.drawable.ic_btn_speak_now,
                "Talk to $assistantName",
                talkPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.stop_listening),
                stopPendingIntent
            )
            .build()
    }
}
