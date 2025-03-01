package com.croniot.android.features.login.usecase

import com.croniot.android.core.data.mappers.toAndroidModel
import com.croniot.android.core.data.source.local.DataStoreController
import com.croniot.android.core.util.DevicePropertiesController
import com.croniot.android.features.login.controller.LoginController.accountRepository
import com.croniot.android.features.login.controller.LoginController.sensorDataRepository
import com.croniot.android.features.login.domain.repository.LoginRepository
import croniot.messages.MessageLoginRequest
import croniot.models.LoginResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginUseCase(
    private val repository: LoginRepository,
) {
    suspend operator fun invoke(email: String, password: String): LoginResult {
        lateinit var loginResult: LoginResult

        val deviceUuid = repository.getDeviceUuid()
        val deviceToken = repository.getDeviceToken()

        // TODO check fields are not empty

        if (deviceUuid != null) {
            val request = MessageLoginRequest(
                email = email,
                password = password,
                deviceUuid = deviceUuid,
                deviceToken = deviceToken,
                deviceProperties = DevicePropertiesController.getDeviceDetails()
            )

            loginResult = repository.login(request)

            val token = loginResult.token

            if (token != null) {
                DataStoreController.saveData(DataStoreController.KEY_DEVICE_TOKEN, token)
            }

            if (loginResult.result.success) {
                val accountDto = loginResult.account

                accountDto?.let {
                    val account = accountDto.toAndroidModel()

                    accountRepository.updateAccount(account)

                    for (device in account.devices) {
                        CoroutineScope(Dispatchers.IO).launch {
                            sensorDataRepository.listenToDeviceSensors(device)
                        }
                    }
                } //TODO else?
                //    _uiState.value = _uiState.value.copy(isLoading = false, loggedIn = true)
            } else {
                /*_uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.result.message,
                )*/
            }
        } else {
            loginResult = LoginResult(croniot.models.Result(false, "Device uuid not found"), null, null)
        }
        return loginResult
    }

    suspend fun checkedLoginState(accountEmail: String, accountPassword: String): Result<Boolean> {
        return try {
            val deviceUuid = repository.getDeviceUuid()
            val deviceToken = repository.getDeviceToken()

            if (deviceUuid == null) {
                return Result.failure(IllegalArgumentException("Device UUID is missing"))
            }

            val request = MessageLoginRequest(
                email = accountEmail,
                password = accountPassword,
                deviceUuid = deviceUuid,
                deviceToken = deviceToken,
                deviceProperties = repository.getDeviceProperties(),
            )

            val loginResult = repository.login(request)

            if (loginResult.result.success) {
                Result.success(loginResult.result.success)
            } else {
                Result.failure(IllegalArgumentException("Could not log in")) // TODO
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
