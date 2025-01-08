package com.croniot.android.features.login.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.core.util.DevicePropertiesController
import com.croniot.android.app.GlobalViewModel
import com.croniot.android.core.data.source.local.SharedPreferences
import com.croniot.android.features.deviceslist.DevicesListViewModel
import com.croniot.android.core.data.source.remote.retrofit.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import croniot.messages.MessageLogin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

class LoginViewModel() : ViewModel(), KoinComponent {

    val context: Context by inject()


    private val globalViewModel: GlobalViewModel = get()

    private val devicesListViewModel: DevicesListViewModel = get()

    private val _email = MutableStateFlow("email1@gmail.com")
    val email : StateFlow<String> get() = _email

    private val _isLoading = MutableStateFlow(false)
    val isLoading : StateFlow<Boolean> get() = _isLoading

    private val _loggedIn= MutableStateFlow(false)
    val loggedIn : StateFlow<Boolean> get() = _loggedIn

    private val _password= MutableStateFlow("password1")
    val password : StateFlow<String> get() = _password

    fun setLoggedIn(loggedInState: Boolean){
        viewModelScope.launch {
            _loggedIn.emit(loggedInState)
        }
    }

    fun updatePassword(password: String){
        viewModelScope.launch {
            _password.emit(password) // Update the counter value by emitting a new value
        }
    }

    fun updateEmail(email: String){
        viewModelScope.launch {
            _email.emit(email)
        }
    }

    suspend fun tryLogin() : Boolean {
        val deviceUuid = SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_UUID)
        val deviceToken = SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_TOKEN)

        val accountEmail = email.value
        val accountPassword = password.value

        return withContext(Dispatchers.IO) {
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

                    globalViewModel.updateAccount(account)

                    SharedPreferences.saveData(SharedPreferences.KEY_ACCOUNT_EMAIL, accountEmail)
                    SharedPreferences.saveData(SharedPreferences.KEY_ACCOUNT_PASSWORD, accountPassword)

                    //withContext(Dispatchers.Default) { //TODO do this in the GlobalViewModel's "updateAccount"
                    devicesListViewModel.updateDevices(account.devices.filter{ it.name.isNotEmpty() }.toList()) //TODO for now we leave this filter
                    //  }
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
}