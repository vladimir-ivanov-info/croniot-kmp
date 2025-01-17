package com.croniot.android.features.login.usecase

import com.croniot.android.features.login.domain.repository.LoginRepository
import croniot.messages.MessageLoginRequest
import croniot.models.LoginResult

class LoginUseCase(
    private val repository: LoginRepository
) {
    suspend operator fun invoke(email: String, password: String): LoginResult {
        lateinit var loginResult : LoginResult

            val deviceUuid = repository.getDeviceUuid()
            val deviceToken = repository.getDeviceToken()

            //TODO
            /*if (deviceUuid == null) {
                return Result.failure(IllegalArgumentException("Device UUID is missing"))
            }*/

        if(deviceUuid != null){
            val request = MessageLoginRequest(
                email = email,
                password = password,
                deviceUuid = deviceUuid,
                deviceToken = deviceToken,
                deviceProperties = mapOf("deviceType" to "Android") // Example property
            )

            loginResult = repository.login(request)
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
                deviceProperties = repository.getDeviceProperties()
            )

            val loginResult = repository.login(request)

            Result.success(loginResult.result.success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}
