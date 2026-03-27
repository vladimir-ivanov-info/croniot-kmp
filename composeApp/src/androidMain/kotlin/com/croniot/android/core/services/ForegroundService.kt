package com.croniot.android.core.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.croniot.android.R
import com.croniot.android.app.MainActivity
import com.croniot.client.presentation.constants.UiConstants

class ForegroundService : Service() { // TODO refactor later

    private companion object {
        const val TAG = "ForegroundService"
        const val CHANNEL_ID = "croniot_service_channel"
        const val EXTRA_START_DESTINATION = "START_DESTINATION"
        const val PREFS_NAME = "AppPrefs"
        const val KEY_LAST_SCREEN = "LAST_SCREEN"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate called")
        startForegroundService()
    }

    private fun startForegroundService() {
        val lastScreen = getLastScreen()
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_START_DESTINATION, lastScreen)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Croniot running")
            .setContentText("Croniot app is running in the background")
            .setSmallIcon(R.drawable.logo_cockroach_test_mode)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun getLastScreen(): String {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(
            KEY_LAST_SCREEN,
            UiConstants.ROUTE_LOGIN,
        ) ?: UiConstants.ROUTE_LOGIN
    }
}
