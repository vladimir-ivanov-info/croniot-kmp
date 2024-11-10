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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.croniot.android.presentation.device.taskTypes.TaskTypeScreen
import com.croniot.android.presentation.device.DeviceScreen
import com.croniot.android.presentation.login.LoginScreen
import com.croniot.android.presentation.registerAccount.ScreenRegisterAccount
import com.croniot.android.ui.theme.IoTClientTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.croniot.android.domain.util.StringUtil
import com.croniot.android.presentation.configuration.ConfigurationScreen
import com.croniot.android.presentation.device.sensors.ViewModelSensors
import com.croniot.android.presentation.devices.DevicesScreen
import com.croniot.android.presentation.devices.DevicesViewModel
import com.croniot.android.ui.task.ViewModelTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.MapLibre

import java.net.InetAddress
import java.net.URI
import android.Manifest
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {

    private val REQUEST_NOTIFICATION_PERMISSION = 1;

    val context: Context by inject()


    var activityState = ""

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted; proceed with showing notifications
        } else {
            // Permission is denied; handle accordingly
        }
    }

    private fun askNotificationPermission() {
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

        askNotificationPermission(); //TODO move to Configuration


        val selectedDevice = SharedPreferences.getSelectedDevice()
        if(selectedDevice != null){
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
        if (selectedDevice != null) {
            Global.selectedDevice = selectedDevice
        }
    }

}

fun generateDeviceUuidIfNotExists(){
    val deviceUuid = SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_UUID)
    if(deviceUuid == null){
        val newDeviceUuid = "android_" + StringUtil.generateRandomString(4);
        SharedPreferences.saveData(SharedPreferences.KEY_DEVICE_UUID, newDeviceUuid)
    }
}

fun resolveServerAddressIfNotExists(){
    val serverAddress = SharedPreferences.loadData(SharedPreferences.KEY_SERVER_ADDRESS)
    if(serverAddress == null){
        resolveAndFollowRedirects("vladimiriot.com") //TODO make constant in Global. Catch error if can't be resolved
    } else {
        Global.SERVER_ADDRESS_REMOTE = serverAddress
        Global.mqttBrokerUrl = "tcp://${Global.SERVER_ADDRESS_REMOTE}:1883"
    }
}

private fun saveCurrentScreenAsync(currentScreen: String) {
    CoroutineScope(Dispatchers.IO).launch {
        SharedPreferences.saveData(SharedPreferences.KEY_CURRENT_SCREEN, currentScreen)
    }
}

@Composable
fun CurrentScreen(){
    generateDeviceUuidIfNotExists()
    LaunchedEffect(Unit) {
        resolveServerAddressIfNotExists()
    }

    val globalViewModel: GlobalViewModel = koinViewModel()

    val viewModelSensors: ViewModelSensors = koinViewModel()
    val devicesViewModel: DevicesViewModel = koinViewModel()
    val viewModelTasks: ViewModelTasks = koinViewModel()

    val navController = rememberNavController()

    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { route ->
                saveCurrentScreenAsync(route)
            }
        }
    }

    NavHost(
        navController = navController,

        startDestination = SharedPreferences.loadData(SharedPreferences.KEY_CURRENT_SCREEN) ?: UiConstants.ROUTE_LOGIN,

        enterTransition = {
            // you can change whatever you want transition
            EnterTransition.None
        },
        exitTransition = {
            // you can change whatever you want transition
            ExitTransition.None
        }

    ) {
         //composable(UiConstants.ROUTE_MAPS) { ScreenMaps(navController) }
        composable("MAPS"){ MapScreen() }
        composable(UiConstants.ROUTE_REGISTER_ACCOUNT) { ScreenRegisterAccount(navController) }

        composable(UiConstants.ROUTE_LOGIN) { LoginScreen(navController) }
        composable(UiConstants.ROUTE_CONFIGURATION) { ConfigurationScreen(navController) }

        composable(UiConstants.ROUTE_DEVICE) { DeviceScreen(navController, Modifier, viewModelSensors, viewModelTasks, globalViewModel) }
        composable(UiConstants.ROUTE_DEVICES) { DevicesScreen(navController, Modifier, devicesViewModel, viewModelSensors, globalViewModel) }
        composable(UiConstants.ROUTE_TASK) { TaskTypeScreen(navController) }
    }
}

private val client = OkHttpClient.Builder().followRedirects(false).build()

private fun resolveAndFollowRedirects(domain: String) {
    CoroutineScope(Dispatchers.IO).launch {
        val initialUrl = "http://$domain"
        //val initialUrl = domain
        val finalUrl = followRedirects(initialUrl)
        val ipAddress = resolveIpAddress(finalUrl)

        val addresses = ipAddress.split("\n")

        if(addresses.size > 0){
            val ipv4Address = addresses[0]
            SharedPreferences.saveData(SharedPreferences.KEY_SERVER_ADDRESS, ipv4Address)
            Global.SERVER_ADDRESS = ipv4Address
            Global.mqttBrokerUrl = "tcp://${Global.SERVER_ADDRESS_REMOTE}:1883"
        }
    }
}

private suspend fun followRedirects(url: String): String = withContext(Dispatchers.IO) {
    var currentUrl = url
    var redirect = true
    var previousResponse: Response? = null

    while (redirect) {
        val request = Request.Builder().url(currentUrl).build()
        val response = client.newCall(request).execute()
        previousResponse?.close()

        if (response.isRedirect) {
            currentUrl = response.header("Location") ?: currentUrl
            if (!currentUrl.startsWith("http")) {
                val uri = URI(url)
                currentUrl = uri.resolve(currentUrl).toString()
            }
        } else {
            redirect = false
        }

        previousResponse = response
    }

    currentUrl
}

private suspend fun resolveIpAddress(url: String): String = withContext(Dispatchers.IO) {
    try {
        val uri = URI(url)
        val host = uri.host ?: return@withContext "Invalid URL"
        val addresses = InetAddress.getAllByName(host)
        addresses.joinToString(separator = "\n") { it.hostAddress }
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown host"
    }
}