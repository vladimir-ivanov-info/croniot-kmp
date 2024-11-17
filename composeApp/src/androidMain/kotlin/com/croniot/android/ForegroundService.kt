package com.croniot.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log

class ForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d("ForegroundService", "Service onCreate called")
        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId = "my_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Croniot background service",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val lastScreen = getLastScreen()
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("START_DESTINATION", lastScreen)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
       // val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE // Add FLAG_IMMUTABLE here
        )

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Croniot running")
            .setContentText("Croniot app is running in the background")
           // .setSmallIcon(R.drawable.ic_notification)
            .setSmallIcon(R.drawable.logo_cockroach_test_mode)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle your background task here
        //Log.d("ForegroundService", "Service onStartCommand called")

        //startMQTTConnection()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun getLastScreen(): String {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("LAST_SCREEN", UiConstants.ROUTE_LOGIN) ?: UiConstants.ROUTE_LOGIN
    }
}
