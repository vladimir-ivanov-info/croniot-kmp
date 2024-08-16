package com.croniot.android.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.SharedPreferences
import com.croniot.android.presentation.devices.DevicesViewModel
import com.croniot.android.data.source.remote.retrofit.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import croniot.messages.MessageLogin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class LoginViewModel() : ViewModel(), KoinComponent {

    private val devicesViewModel: DevicesViewModel = get()

    private val _email = MutableStateFlow("email1@gmail.com")
    val email : StateFlow<String> get() = _email

    private val _isLoading = MutableStateFlow(false)
    val isLoading : StateFlow<Boolean> get() = _isLoading

    private val _loggedIn= MutableStateFlow(false)
    val loggedIn : StateFlow<Boolean> get() = _loggedIn

    private val _password= MutableStateFlow("password1")
    val password : StateFlow<String> get() = _password

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
        val millisStart = System.currentTimeMillis()
        val deviceUuid = SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_UUID)
        val deviceToken = SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_TOKEN)

       // if(deviceToken == null){
            //login with email and password and deviceuuid
       // } else {
            //login with deviceUuid and token
       // }

        val accountEmail = email.value
        val accountPassword = password.value

        return withContext(Dispatchers.IO) { try {
                val messageLogin = MessageLogin(accountEmail, accountPassword, deviceUuid!!, deviceToken)
                val response = RetrofitClient.loginApiService.login(messageLogin)
            val millisFinish = System.currentTimeMillis();
            println("Millis for login: ${millisFinish - millisStart} ms")
                val account = response.body()
                if (response.isSuccessful && account != null) {
                    com.croniot.android.Global.account = account// Log or use the UUID as needed

                    withContext(Dispatchers.Default) {
                        devicesViewModel.updateDevices(account.devices.toList())
                    }
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