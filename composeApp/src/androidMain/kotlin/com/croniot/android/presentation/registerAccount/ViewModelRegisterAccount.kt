package com.croniot.android.presentation.registerAccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.croniot.android.SharedPreferences
import com.croniot.android.Global
import croniot.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import croniot.messages.MessageRegisterAccount

class ViewModelRegisterAccount() : ViewModel() {

    private val _nickname= MutableStateFlow("vladimiriot")  //default for testing
    val nickname : StateFlow<String> get() = _nickname

    private val _email = MutableStateFlow("email1@gmail.com")  //default for testing
    val email : StateFlow<String> get() = _email

    private val _password = MutableStateFlow("password1") //default for testing
    val password : StateFlow<String> get() = _password

    fun updateNickname(nickname: String){
        viewModelScope.launch {
            _nickname.emit(nickname)
        }
    }

    fun updateEmail(email: String){
        viewModelScope.launch {
            _email.emit(email)
        }
    }

    fun updatePassword(password: String){
        viewModelScope.launch {
            _password.emit(password)
        }
    }

    suspend fun registerAccount(): Result {
        return withContext(Dispatchers.IO){
            val nickname = nickname.value
            val email = email.value
            val password = password.value
            val accountUuid = "account1Uuid" //TODO for testing
            //val deviceUuid = "android1Uuid"
           // val deviceUuid = StringUtil.generateRandomString(3)
            //val deviceName = deviceName.value
            //val deviceDescription = "Vladimir's Android device used to monitor and control his IoT devices."

            val message = MessageRegisterAccount(accountUuid, nickname, email, password)

            val gson = GsonBuilder().setPrettyPrinting().create()
            val messageRegisterAccountJson = gson.toJson(message)

            val result = Global.performPostRequestToEndpoint("/api/register_account", messageRegisterAccountJson)

            if(result.success){
                SharedPreferences.saveData(SharedPreferences.KEY_ACCOUNT_EMAIL, email)
                SharedPreferences.saveData(SharedPreferences.KEY_ACCOUNT_PASSWORD, password)
                //SharedPreferences.saveData(SharedPreferences.KEY_DEVICE_TOKEN, token)
                //SharedPreferences.saveData(SharedPreferences.KEY_DEVICE_UUID, deviceUuid)
              //  true
            } else {
                //TODO show error
               // false
            }
            result
        }
    }
}