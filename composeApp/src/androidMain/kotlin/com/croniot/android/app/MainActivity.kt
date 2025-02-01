package com.croniot.android.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import com.croniot.android.core.presentation.theme.IoTClientTheme
import org.koin.android.ext.android.inject
import android.Manifest
import androidx.core.app.ActivityCompat
import com.croniot.android.core.data.source.local.DataStoreController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val REQUEST_NOTIFICATION_PERMISSION = 1;

    val context: Context by inject()

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

        CoroutineScope(Dispatchers.IO).launch {
            Global.selectedDevice = DataStoreController.getSelectedDevice().first()
            DataStoreController.generateAndSaveDeviceUuidIfNotExists()
        }

        //MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)
        //MapLibre.getInstance(this, null)
        //MapLibre.getInstance(this) //TODO see if we can move this to the corresponding composable

        setContent {
            IoTClientTheme {
                CurrentScreen();
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO).launch {
            Global.selectedDevice = DataStoreController.getSelectedDevice().first()
        }
    }
}