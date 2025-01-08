package com.croniot.android.features.login.controller

import android.content.Context
import androidx.navigation.NavController
import com.croniot.android.core.util.DevicePropertiesController
import com.croniot.android.core.data.source.local.SharedPreferences
import com.croniot.android.core.presentation.UiConstants
import com.croniot.android.core.data.source.remote.retrofit.RetrofitClient
import com.croniot.android.features.device.features.sensors.presentation.ViewModelSensors
import com.croniot.android.features.deviceslist.DevicesListViewModel
import com.croniot.android.features.device.features.tasks.ViewModelTasks
import croniot.messages.MessageLogin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

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

}