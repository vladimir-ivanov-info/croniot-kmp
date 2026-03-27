package com.croniot.android.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.croniot.android.R
import java.util.concurrent.atomic.AtomicInteger

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val idCounter = AtomicInteger(1000)

    companion object {
        const val CHANNEL_ID_GENERAL = "croniot_general"
        const val CHANNEL_ID_SERVICE = "croniot_service_channel"
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        val general = NotificationChannel(
            CHANNEL_ID_GENERAL,
            "General",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "General notifications"
        }

        val service = NotificationChannel(
            CHANNEL_ID_SERVICE,
            "Background service",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Foreground service notification"
        }

        notificationManager.createNotificationChannels(listOf(general, service))
    }

    fun show(
        title: String,
        text: String,
        channelId: String = CHANNEL_ID_GENERAL,
        notificationId: Int = idCounter.getAndIncrement(),
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo_cockroach_test_mode)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}