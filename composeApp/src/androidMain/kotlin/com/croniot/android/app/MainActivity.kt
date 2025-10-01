package com.croniot.android.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.croniot.android.core.presentation.theme.IoTClientTheme
import com.croniot.client.data.repositories.LocalDataRepository
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val REQUEST_NOTIFICATION_PERMISSION = 1

    val context: Context by inject()
    val localDataRepository: LocalDataRepository by inject() // or koinInject?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermissionIfNecessary(); // TODO move to Configuration

        // MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)
        // MapLibre.getInstance(this, null)
        // MapLibre.getInstance(this) //TODO see if we can move this to the corresponding composable

        setContent {
            IoTClientTheme {
                CurrentScreen()
            }
        }
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
