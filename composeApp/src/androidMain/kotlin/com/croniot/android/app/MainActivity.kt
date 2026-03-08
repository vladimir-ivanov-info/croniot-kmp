package com.croniot.android.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.croniot.android.core.presentation.theme.IoTClientTheme
import com.croniot.android.BuildConfig
import com.croniot.client.core.config.AppConfig

class MainActivity : ComponentActivity() {

    private val REQUEST_NOTIFICATION_PERMISSION = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppConfig.isDemo = BuildConfig.IS_DEMO

        setContent {
            IoTClientTheme(dynamicColor = false) {
                CurrentScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        AppConfig.isDemo = BuildConfig.IS_DEMO

        askNotificationPermissionIfNecessary()
    }

    private fun askNotificationPermissionIfNecessary() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION,
            )
        }
    }
}
