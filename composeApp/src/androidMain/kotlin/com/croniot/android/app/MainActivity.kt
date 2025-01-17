package com.croniot.android.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.croniot.android.core.presentation.theme.IoTClientTheme
import org.koin.android.ext.android.inject
import org.maplibre.android.MapLibre
import android.Manifest
import androidx.core.app.ActivityCompat
import com.croniot.android.core.data.source.local.SharedPreferences

class MainActivity : ComponentActivity() {

    private val REQUEST_NOTIFICATION_PERMISSION = 1;

    val context: Context by inject()

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted; proceed with showing notifications
        } else {
            // Permission is denied; handle accordingly
        }
    }

    private fun askNotificationPermissionIfNecessary() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askNotificationPermissionIfNecessary(); //TODO move to Configuration

        val selectedDevice = SharedPreferences.getSelectedDevice()
        selectedDevice?.let {
            Global.selectedDevice = selectedDevice
        }

        //MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)
        // MapLibre.getInstance(this, null)
        MapLibre.getInstance(this) //TODO see if we can move this to the corresponding composable

        setContent {
            IoTClientTheme {
                CurrentScreen();
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val selectedDevice = SharedPreferences.getSelectedDevice()
        selectedDevice?.let{
            Global.selectedDevice = selectedDevice
        }
    }
}