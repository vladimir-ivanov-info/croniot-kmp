package com.croniot.android.features.login.controller

import androidx.navigation.NavController
import com.croniot.android.core.data.source.local.DataStoreController
import com.croniot.android.core.data.source.repository.AccountRepository
import com.croniot.android.core.data.source.repository.SensorDataRepository
import com.croniot.android.core.presentation.UiConstants
import com.croniot.android.features.device.features.sensors.presentation.ViewModelSensors
import com.croniot.android.features.device.features.tasks.ViewModelTasks
import com.croniot.android.features.deviceslist.DevicesListViewModel
import com.croniot.android.features.login.usecase.LoginUseCase
import croniot.models.dto.AccountDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object LoginController : KoinComponent {

    val accountRepository: AccountRepository = get()
    val sensorDataRepository: SensorDataRepository = get()

    fun logOut(navController: NavController) {
        val devicesListViewModel: DevicesListViewModel = get()
        val viewModelSensors: ViewModelSensors = get()
        val viewModelTasks: ViewModelTasks = get()

        // Clean up all session data
        viewModelTasks.uninit()
        devicesListViewModel.uninit()
// TODO        viewModelSensors.uninit()
        //  SharedPreferences.clearCache()

        // TODO
        runBlocking {
            DataStoreController.clearAllCacheExceptDeviceUuid()
        }

        accountRepository.clearAccount()

        // Navigate to login screen
        navController.navigate(UiConstants.ROUTE_LOGIN) {
            popUpTo(0) { inclusive = true } // Clears entire backstack
        }
    }

    fun forceLogOut(navController: NavController) {
        val devicesListViewModel: DevicesListViewModel = get()
        val viewModelSensors: ViewModelSensors = get()
        val viewModelTasks: ViewModelTasks = get()

        // Clean up all session data
        viewModelTasks.uninit()
        devicesListViewModel.uninit()
        // TODO     viewModelSensors.uninit()

        // Navigate to login screen
        navController.navigate(UiConstants.ROUTE_LOGIN) {
            popUpTo(0) { inclusive = true } // Clears entire backstack
        }
    }

    fun processLoginOnAppEntered(loginUseCase: LoginUseCase, navController: NavController) {
        val currentScreen = runBlocking {
            DataStoreController.loadData(DataStoreController.KEY_CURRENT_SCREEN).first()
        }

        if (currentScreen != UiConstants.ROUTE_LOGIN && currentScreen != UiConstants.ROUTE_CONFIGURATION) {
            val account = accountRepository.account.value
            account?.let {
                for (device in account.devices) {
                    CoroutineScope(Dispatchers.IO).launch {
                        sensorDataRepository.listenToDeviceSensors(device)
                    }
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                val latestLoggedInEmail = DataStoreController.loadData(DataStoreController.KEY_ACCOUNT_EMAIL).first()
                val latestLoggedInPassword = DataStoreController.loadData(DataStoreController.KEY_ACCOUNT_PASSWORD).first()

                if (latestLoggedInEmail != null && latestLoggedInPassword != null) {
                    val result = loginUseCase.checkedLoginState(
                        latestLoggedInEmail,
                        latestLoggedInPassword,
                    ) // TODO remove parameters

                    if (!result.isSuccess) {
                        clearSessionCacheAndMoveToLoginScreen(navController)
                    }
                } else {
                    clearSessionCacheAndMoveToLoginScreen(navController)
                }
            }
        }
    }

    private fun clearSessionCacheAndMoveToLoginScreen(navController: NavController) {
        // SharedPreferences.clearCache()

        CoroutineScope(Dispatchers.IO).launch {
            DataStoreController.clearAllCacheExceptDeviceUuid()
        }

        CoroutineScope(Dispatchers.Main).launch {
            navController.navigate(UiConstants.ROUTE_LOGIN) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    suspend fun processLoginSuccess(account: AccountDto) {
        accountRepository.updateAccount(account)

        for (device in account.devices) {
            CoroutineScope(Dispatchers.IO).launch {
                sensorDataRepository.listenToDeviceSensors(device)
            }
        }
    }
}
