package com.croniot.android.app

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.croniot.android.core.presentation.splash.SplashScreen
import com.croniot.android.features.configuration.ConfigurationScreen
import com.croniot.android.features.device.presentation.DeviceScreen
import com.croniot.android.features.devicelist.DeviceListScreen
import com.croniot.android.features.registeraccount.presentation.ScreenRegisterAccount
import com.croniot.client.features.blediscovery.presentation.BleDiscoveryScreen
import com.croniot.client.features.login.presentation.LoginScreen
import com.croniot.client.features.tasktypes.presentation.create_task.CreateTaskScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun CurrentScreen(viewModel: AppViewModel = koinViewModel()) {
    val navController = rememberNavController()

    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _: NavController, dest: NavDestination, _: Bundle? ->
            val route = dest.route ?: navController.currentBackStackEntry?.destination?.route
            if (route != null) {
                viewModel.onScreenChanged(route)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash,
    ) {
        composable<AppRoute.Test> {
            CroniotDashboardPreview8()
        }

        composable<AppRoute.Splash> {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(AppRoute.Login) { popUpTo(0) { inclusive = true } }
                },
                onNavigateToDeviceList = { error ->
                    navController.navigate(AppRoute.Devices(errorJson = error?.encode())) { popUpTo(0) { inclusive = true } }
                },
                onNavigateToDevice = { deviceUuid, error ->
                    navController.navigate(AppRoute.Device(deviceUuid, errorJson = error?.encode())) { popUpTo(0) { inclusive = true } }
                },
            )
        }

        composable<AppRoute.CreateAccount> {
            ScreenRegisterAccount(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(AppRoute.Login)
                    }
                },
            )
        }

        composable<AppRoute.Login> {
            LoginScreen(
                onNavigateToDeviceList = {
                    navController.navigate(AppRoute.Devices()) {
                        popUpTo<AppRoute.Login> { inclusive = true }
                    }
                },
                onNavigateToRegisterAccount = {
                    navController.navigate(AppRoute.CreateAccount) {
                        popUpTo<AppRoute.Login> { inclusive = true }
                    }
                },
                onNavigateToConfiguration = {
                    navController.navigate(AppRoute.Configuration) {
                        popUpTo<AppRoute.Login> { inclusive = true }
                    }
                },
                onNavigateToBleDiscovery = {
                    navController.navigate(AppRoute.BleDiscovery)
                },
            )
        }

        composable<AppRoute.BleDiscovery> {
            BleDiscoveryScreen(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(AppRoute.Login)
                    }
                },
                onNavigateToDevice = { deviceUuid ->
                    navController.navigate(AppRoute.Device(deviceUuid)) {
                        popUpTo<AppRoute.Login> { inclusive = true }
                    }
                },
            )
        }

        composable<AppRoute.Configuration> {
            ConfigurationScreen(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(AppRoute.Login)
                    }
                },
            )
        }

        composable<AppRoute.Device> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.Device>()
            DeviceScreen(
                selectedDeviceUuid = route.deviceUuid,
                appError = AppError.decode(route.errorJson),
                onNavigateBack = { navController.navigate(AppRoute.Devices()) },
                onTaskTypeClicked = { deviceUuid, taskUid ->
                    navController.navigate(AppRoute.CreateTask(deviceUuid, taskUid))
                },
            )
        }

        composable<AppRoute.Devices> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.Devices>()
            DeviceListScreen(
                appError = AppError.decode(route.errorJson),
                onLogOut = {
                    navController.navigate(AppRoute.Splash) {
                        popUpTo<AppRoute.Devices> { inclusive = true }
                    }
                },
                onDeviceClicked = { deviceUuid ->
                    navController.navigate(AppRoute.Device(deviceUuid))
                },
                onNavigateToBleDiscovery = {
                    navController.navigate(AppRoute.BleDiscovery)
                },
            )
        }

        composable<AppRoute.CreateTask> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.CreateTask>()
            CreateTaskScreen(
                deviceUuid = route.deviceUuid,
                taskTypeUid = route.taskUid,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
