package com.croniot.android.presentation.login

import androidx.navigation.NavController
import com.croniot.android.SharedPreferences
import com.croniot.android.UiConstants
import com.croniot.android.data.source.remote.retrofit.RetrofitClient
import com.croniot.android.presentation.device.sensors.ViewModelSensors
import com.croniot.android.presentation.devices.DevicesViewModel
import com.croniot.android.ui.task.ViewModelTasks
import croniot.messages.MessageLogin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object LoginController : KoinComponent {

    suspend fun checkedLoginState(accountEmail: String, accountPassword: String) : Boolean {
        val deviceUuid = SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_UUID)
        val deviceToken = SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_TOKEN)

        return withContext(Dispatchers.Main) {
            try {
                val messageLogin = MessageLogin(accountEmail, accountPassword, deviceUuid!!, deviceToken)
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
        val devicesViewModel: DevicesViewModel = get()
        val viewModelSensors: ViewModelSensors = get()
        val viewModelTasks: ViewModelTasks = get()

        viewModelTasks.uninit()
        devicesViewModel.uninit()
        viewModelSensors.uninit()

        navController.navigate(UiConstants.ROUTE_LOGIN) {
            popUpTo(0) { inclusive = true } // Clears entire backstack
        }
    }

    fun forceLogOut(navController: NavController){
        val devicesViewModel: DevicesViewModel = get()
        val viewModelSensors: ViewModelSensors = get()
        val viewModelTasks: ViewModelTasks = get()

        viewModelTasks.uninit()
        devicesViewModel.uninit()
        viewModelSensors.uninit()

        navController.navigate(UiConstants.ROUTE_LOGIN) {
            popUpTo(0) { inclusive = true } // Clears entire backstack
        }
    }

}