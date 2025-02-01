package com.croniot.android.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.croniot.android.core.data.source.local.DataStoreController
import com.croniot.android.core.presentation.UiConstants
import com.croniot.android.core.presentation.composables.map.MapScreen
import com.croniot.android.core.util.NetworkUtil
import com.croniot.android.features.configuration.ConfigurationScreen
import com.croniot.android.features.device.features.sensors.presentation.ViewModelSensors
import com.croniot.android.features.device.features.tasktypes.CreateTaskScreen
import com.croniot.android.features.device.presentation.DeviceScreen
import com.croniot.android.features.deviceslist.DevicesListViewModel
import com.croniot.android.features.deviceslist.DevicesScreen
import com.croniot.android.features.login.controller.LoginController
import com.croniot.android.features.login.presentation.LoginScreen
import com.croniot.android.features.login.usecase.LoginUseCase
import com.croniot.android.features.registeraccount.presentation.ScreenRegisterAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun CurrentScreen() {
    LaunchedEffect(Unit) {
        NetworkUtil.resolveServerAddressIfNotExists()
    }

    val loginUseCase: LoginUseCase = koinInject()

    val devicesListViewModel: DevicesListViewModel = koinViewModel()
    val viewModelSensors: ViewModelSensors = koinViewModel()

    val navController = rememberNavController()

    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { route ->
                saveCurrentScreenAsync(route)
            }
        }

        LoginController.processLoginOnAppEntered(loginUseCase, navController)
    }

    val startDestination = runBlocking {
        DataStoreController.loadData(DataStoreController.KEY_CURRENT_SCREEN).first() ?: UiConstants.ROUTE_LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        // enterTransition = { EnterTransition.None },
        // exitTransition = { ExitTransition.None }
    ) {
        // composable(UiConstants.ROUTE_MAPS) { ScreenMaps(navController) }
        composable("MAPS") { MapScreen() }
        composable(UiConstants.ROUTE_REGISTER_ACCOUNT) { ScreenRegisterAccount(navController) }

        composable(UiConstants.ROUTE_LOGIN) { LoginScreen(navController) }
        composable(UiConstants.ROUTE_CONFIGURATION) { ConfigurationScreen(navController) }

        composable(UiConstants.ROUTE_DEVICE) { DeviceScreen(navController, viewModelSensors) }
        composable(UiConstants.ROUTE_DEVICES) { DevicesScreen(navController, devicesListViewModel) }
        composable(UiConstants.ROUTE_CREATE_TASK) { CreateTaskScreen(navController) }
    }
}

fun saveCurrentScreenAsync(currentScreen: String) {
    CoroutineScope(Dispatchers.IO).launch {
        DataStoreController.saveData(DataStoreController.KEY_CURRENT_SCREEN, currentScreen)
    }
}
