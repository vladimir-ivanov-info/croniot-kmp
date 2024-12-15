package com.croniot.android

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.croniot.android.presentation.device.taskTypes.TaskTypeScreen
import com.croniot.android.presentation.device.DeviceScreen
import com.croniot.android.presentation.login.LoginScreen
import com.croniot.android.presentation.registerAccount.ScreenRegisterAccount
import com.croniot.android.ui.theme.IoTClientTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.croniot.android.presentation.configuration.ConfigurationScreen
import com.croniot.android.presentation.device.sensors.ViewModelSensors
import com.croniot.android.presentation.devices.DevicesScreen
import com.croniot.android.presentation.devices.DevicesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.MapLibre
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.croniot.android.presentation.login.LoginController


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
        // Check if the permission is already granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

        // Retrieve and set selected device if available
        val selectedDevice = SharedPreferences.getSelectedDevice()
        selectedDevice?.let{
            Global.selectedDevice = selectedDevice
        }
    }

}

private fun saveCurrentScreenAsync(currentScreen: String) {
    CoroutineScope(Dispatchers.IO).launch {
        SharedPreferences.saveData(SharedPreferences.KEY_CURRENT_SCREEN, currentScreen)
    }
}

@Composable
fun CurrentScreen(){
    SharedPreferences.generateAndSaveDeviceUuidIfNotExists()
    LaunchedEffect(Unit) {
        NetworkUtil.resolveServerAddressIfNotExists()
    }

    val globalViewModel: GlobalViewModel = koinViewModel()

    val viewModelSensors: ViewModelSensors = koinViewModel()
    val devicesViewModel: DevicesViewModel = koinViewModel()

    val navController = rememberNavController()

    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { route ->
                saveCurrentScreenAsync(route)
            }
        }

        val currentScreen = SharedPreferences.loadData(SharedPreferences.KEY_CURRENT_SCREEN)
        if(currentScreen != UiConstants.ROUTE_LOGIN && currentScreen != UiConstants.ROUTE_CONFIGURATION){ // or configuration screen

            CoroutineScope(Dispatchers.IO).launch {
                val latestLoggedInEmail = SharedPreferences.loadData(SharedPreferences.KEY_ACCOUNT_EMAIL)
                val latestLoggedInPassword = SharedPreferences.loadData(SharedPreferences.KEY_ACCOUNT_PASSWORD)

                if(latestLoggedInEmail != null && latestLoggedInPassword != null){
                    val success = LoginController.checkedLoginState(latestLoggedInEmail, latestLoggedInPassword)
                    if(!success){
                        clearSessionCacheAndMoveToLoginScreen(navController)
                    }
                } else {
                    clearSessionCacheAndMoveToLoginScreen(navController)
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = SharedPreferences.loadData(SharedPreferences.KEY_CURRENT_SCREEN) ?: UiConstants.ROUTE_LOGIN,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        //composable(UiConstants.ROUTE_MAPS) { ScreenMaps(navController) }
        composable("MAPS"){ MapScreen() }
        composable(UiConstants.ROUTE_REGISTER_ACCOUNT) { ScreenRegisterAccount(navController) }

        composable(UiConstants.ROUTE_LOGIN) { LoginScreen(navController) }
        composable(UiConstants.ROUTE_CONFIGURATION) { ConfigurationScreen(navController) }

        composable(UiConstants.ROUTE_DEVICE) { DeviceScreen(navController, Modifier, viewModelSensors) }
        composable(UiConstants.ROUTE_DEVICES) { DevicesScreen(navController, Modifier, devicesViewModel, viewModelSensors, globalViewModel) }
        composable(UiConstants.ROUTE_TASK) { TaskTypeScreen(navController) }
    }
}

fun clearSessionCacheAndMoveToLoginScreen(navController: NavController){
    SharedPreferences.clearCache()
    CoroutineScope(Dispatchers.Main).launch {
        navController.navigate(UiConstants.ROUTE_LOGIN) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }
}