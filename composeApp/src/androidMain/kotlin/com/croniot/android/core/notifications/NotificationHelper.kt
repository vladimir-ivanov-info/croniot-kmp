package com.croniot.android.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.croniot.android.R
import java.util.concurrent.atomic.AtomicInteger

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val idCounter = AtomicInteger(1000)

    private fun renderLogoBitmap(redId: Int, sizeDp: Int): Bitmap {
        val drawable = AppCompatResources.getDrawable(context, redId)!!
        val size = (sizeDp * context.resources.displayMetrics.density).toInt()
        val scale = minOf(
            size.toFloat() / drawable.intrinsicWidth,
            size.toFloat() / drawable.intrinsicHeight,
        )
        val drawW = (drawable.intrinsicWidth * scale).toInt()
        val drawH = (drawable.intrinsicHeight * scale).toInt()
        val left = (size - drawW) / 2
        val top = (size - drawH) / 2
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(left, top, left + drawW, top + drawH)
        drawable.draw(canvas)
        return bitmap
    }

    private val smallIcon: IconCompat by lazy {
        IconCompat.createWithBitmap(renderLogoBitmap(R.drawable.logo_cockroach, 24))
    }

    private val largeIcon: Bitmap by lazy {
        renderLogoBitmap(R.drawable.logo_croniot_1, 64)
    }

    companion object {
        const val CHANNEL_ID_GENERAL = "croniot_general"
        const val CHANNEL_ID_SERVICE = "croniot_service_channel"
        const val CHANNEL_ID_TASK_PROGRESS = "croniot_task_progress"
        private val BRAND_COLOR = Color.WHITE
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        val general = NotificationChannel(
            CHANNEL_ID_GENERAL,
            "General",
            NotificationManager.IMPORTANCE_HIGH,
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

        val taskProgress = NotificationChannel(
            CHANNEL_ID_TASK_PROGRESS,
            "Task progress",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Live task progress updates"
            setSound(null, null)
        }

        notificationManager.createNotificationChannels(listOf(general, service, taskProgress))
    }

    fun show(
        title: String,
        text: String,
        channelId: String = CHANNEL_ID_GENERAL,
        notificationId: Int = idCounter.getAndIncrement(),
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setLargeIcon(largeIcon)
            .setColor(BRAND_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showProgress(
        title: String,
        text: String,
        progress: Int,
        maxProgress: Int = 100,
        ongoing: Boolean = true,
        notificationId: Int = idCounter.getAndIncrement(),
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TASK_PROGRESS)
            .setSmallIcon(smallIcon)
            .setLargeIcon(largeIcon)
            .setColor(BRAND_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(maxProgress, progress, false)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showInboxStyle(
        title: String,
        lines: List<String>,
        ongoing: Boolean = true,
        notificationId: Int = idCounter.getAndIncrement(),
    ) {
        val style = NotificationCompat.InboxStyle()
        lines.forEach { style.addLine(it) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TASK_PROGRESS)
            .setSmallIcon(smallIcon)
            .setLargeIcon(largeIcon)
            .setColor(BRAND_COLOR)
            .setContentTitle(title)
            .setContentText(lines.firstOrNull().orEmpty())
            .setStyle(style)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}