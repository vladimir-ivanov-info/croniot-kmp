package com.croniot.android.app

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.croniot.android.core.presentation.composables.map.MapScreen
import com.croniot.android.core.presentation.splash.SplashScreen
import com.croniot.android.features.configuration.ConfigurationScreen
import com.croniot.android.features.device.presentation.DeviceScreen
import com.croniot.android.features.devicelist.DeviceListScreen
import com.croniot.android.features.devicelist.DeviceListScreenRoot
import com.croniot.android.features.registeraccount.presentation.ScreenRegisterAccountRoot
import com.croniot.client.presentation.constants.UiConstants
import com.croniot.client.data.repositories.LocalDataRepository
import com.croniot.client.features.login.ui.LoginScreen
import com.croniot.client.features.login.ui.LoginScreenRoot
import com.croniot.client.features.tasktypes.presentation.create_task.CreateTaskScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun CurrentScreen() {

    val navController = rememberNavController()

    val coroutineScope = rememberCoroutineScope()


    /*LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { route ->
                saveCurrentScreenAsync(route)
            }
        }

    }*/

    val localDataRepository : LocalDataRepository = koinInject()

    LaunchedEffect(navController) {

        localDataRepository.generateAndSaveDeviceUuidIfNotExists()

        navController.addOnDestinationChangedListener { _: NavController, dest: NavDestination, _: Bundle? ->

            val route =
                try {
                    dest.route
                } catch (_: Throwable) {
                    null
                } ?: navController.currentBackStackEntry?.destination?.route
            if (route != null) {
                coroutineScope.launch {
                    localDataRepository.saveCurrentScreen(route)
                }
            }
        }
    }

    val serverMode = localDataRepository.getServerMode().collectAsState(initial = "local").value ?: "local" //TODO

    NavHost(
        navController = navController,
       // startDestination = startDestination,
        startDestination = UiConstants.ROUTE_SPLASH,
        // enterTransition = { EnterTransition.None },
        // exitTransition = { ExitTransition.None }
    ) {//TODO
        // composable(UiConstants.ROUTE_MAPS) { ScreenMaps(navController) }

        composable(UiConstants.ROUTE_TEST) {
            CroniotDashboardPreview8()
        }

        composable(UiConstants.ROUTE_SPLASH) {
            SplashScreen(
                navController = navController
            )
        }

        composable("MAPS") { MapScreen() }

        composable(UiConstants.ROUTE_CREATE_ACCOUNT) {
            ScreenRegisterAccountRoot(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(UiConstants.ROUTE_LOGIN)
                    }
                }
            )
        }

        composable(UiConstants.ROUTE_LOGIN) {
            //LoginScreen(navController, serverMode = serverMode)
            LoginScreenRoot(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(UiConstants.ROUTE_LOGIN) { inclusive = true }
                        // launchSingleTop = true
                    }
                },
            )
        }

        composable(UiConstants.ROUTE_CONFIGURATION) { ConfigurationScreen(navController) }

        composable("${UiConstants.ROUTE_DEVICE}/{deviceUuid}") { backStackEntry ->

            val selectedDeviceUuid = backStackEntry.arguments?.getString("deviceUuid")

            if(selectedDeviceUuid != null){
                DeviceScreen(
                    selectedDeviceUuid = selectedDeviceUuid,
                    navController = navController,
                    onTaskTypeClicked = { deviceUuid, taskUid ->
                        navController.navigate("${UiConstants.ROUTE_CREATE_TASK}/$deviceUuid/$taskUid")
                    }
                )
            } //TODO else
        }

        composable(UiConstants.ROUTE_DEVICES) {
            DeviceListScreenRoot(
                //navController = navController,
                onLogOut = {
                    navController.navigate(UiConstants.ROUTE_LOGIN) {
                        popUpTo(UiConstants.ROUTE_DEVICES) { inclusive = true }
                        // launchSingleTop = true
                    }
                },
                onDeviceClicked = { deviceUuid ->
                    navController.navigate("${UiConstants.ROUTE_DEVICE}/$deviceUuid")
                }
            )
        }

        composable(
            route = "${UiConstants.ROUTE_CREATE_TASK}/{deviceUuid}/{taskUid}",
            arguments = listOf(
                navArgument("deviceUuid") { type = NavType.StringType },
                navArgument("taskUid")    { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val deviceUuid = backStackEntry.arguments?.getString("deviceUuid")
            val taskUid = backStackEntry.arguments?.getLong("taskUid")

            if(deviceUuid != null && taskUid != null){
                CreateTaskScreen(
                    deviceUuid,
                    taskUid,
                    navController
                )
            }
        }
    }
}

