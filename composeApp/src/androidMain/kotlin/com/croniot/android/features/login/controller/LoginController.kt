package com.croniot.android.features.login.controller

import androidx.navigation.NavController
import com.croniot.android.core.data.source.local.SharedPreferences
import com.croniot.android.core.presentation.UiConstants
import com.croniot.android.core.data.source.repository.AccountRepository
import com.croniot.android.features.device.features.sensors.presentation.ViewModelSensors
import com.croniot.android.features.deviceslist.DevicesListViewModel
import com.croniot.android.features.device.features.tasks.ViewModelTasks
import com.croniot.android.features.login.usecase.LoginUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/*
object LoginController : KoinComponent {

    val context: Context by inject()

    suspend fun checkedLoginState(accountEmail: String, accountPassword: String) : Boolean {
        val deviceUuid = SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_UUID)
        val deviceToken = SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_TOKEN)

        return withContext(Dispatchers.Main) {
            try {
                val details = DevicePropertiesController.getScreenDetails(context)
                val details2 = DevicePropertiesController.getDeviceDetails()
                val deviceProperties = details + details2

                val messageLogin = MessageLogin(accountEmail, accountPassword, deviceUuid!!, deviceToken, deviceProperties)
                val response = RetrofitClient.loginApiService.login(messageLogin)
                val loginResult = response.body()!!
                val result = loginResult.result
                val account = loginResult.account
                val token = loginResult.token

                if(token != null){
                    SharedPreferences.saveData(SharedPreferences.KEY_DEVICE_TOKEN, token)
                }

                if (response.isSuccessful && result.success && account != null) {
                    true  // Login successful
                } else {
                    println("Error: ${response.errorBody()?.string()}")
                    false  // Login failed
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
                false  // Network error or exception occurred
            }}
    }

    fun logOut(navController: NavController){
        val devicesListViewModel: DevicesListViewModel = get()
        val viewModelSensors: ViewModelSensors = get()
        val viewModelTasks: ViewModelTasks = get()

        viewModelTasks.uninit()
        devicesListViewModel.uninit()
        viewModelSensors.uninit()
        SharedPreferences.clearCache()

        navController.navigate(UiConstants.ROUTE_LOGIN) {
            popUpTo(0) { inclusive = true } // Clears entire backstack
        }
    }

    fun forceLogOut(navController: NavController){
        val devicesListViewModel: DevicesListViewModel = get()
        val viewModelSensors: ViewModelSensors = get()
        val viewModelTasks: ViewModelTasks = get()

        viewModelTasks.uninit()
        devicesListViewModel.uninit()
        viewModelSensors.uninit()

        navController.navigate(UiConstants.ROUTE_LOGIN) {
            popUpTo(0) { inclusive = true } // Clears entire backstack
        }
    }

}*/


object LoginController : KoinComponent {

    val accountRepository : AccountRepository = get()

    fun logOut(navController: NavController) {
        val devicesListViewModel: DevicesListViewModel = get()
        val viewModelSensors: ViewModelSensors = get()
        val viewModelTasks: ViewModelTasks = get()

        // Clean up all session data
        viewModelTasks.uninit()
        devicesListViewModel.uninit()
        viewModelSensors.uninit()
        SharedPreferences.clearCache()

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
        viewModelSensors.uninit()

        // Navigate to login screen
        navController.navigate(UiConstants.ROUTE_LOGIN) {
            popUpTo(0) { inclusive = true } // Clears entire backstack
        }
    }


    ///////////

    fun processLoginOnAppEntered(loginUseCase: LoginUseCase, navController: NavController){

        val currentScreen = SharedPreferences.loadData(SharedPreferences.KEY_CURRENT_SCREEN)
        if(currentScreen != UiConstants.ROUTE_LOGIN && currentScreen != UiConstants.ROUTE_CONFIGURATION){
            CoroutineScope(Dispatchers.IO).launch {
                val latestLoggedInEmail =
                    SharedPreferences.loadData(SharedPreferences.KEY_ACCOUNT_EMAIL)
                val latestLoggedInPassword =
                    SharedPreferences.loadData(SharedPreferences.KEY_ACCOUNT_PASSWORD)

                if (latestLoggedInEmail != null && latestLoggedInPassword != null) {
                    val result = loginUseCase.checkedLoginState(
                        latestLoggedInEmail,
                        latestLoggedInPassword
                    ) //TODO remove parameters

                    if (!result.isSuccess) {
                        clearSessionCacheAndMoveToLoginScreen(navController)
                    }
                } else {
                    clearSessionCacheAndMoveToLoginScreen(navController)
                }
            }
        }
    }

    private fun clearSessionCacheAndMoveToLoginScreen(navController: NavController){
        SharedPreferences.clearCache()
        CoroutineScope(Dispatchers.Main).launch {
            navController.navigate(UiConstants.ROUTE_LOGIN) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }


}