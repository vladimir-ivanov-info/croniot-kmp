package com.croniot.android.features.login.data.repository

import com.croniot.android.core.data.source.local.SharedPreferences
import com.croniot.android.core.util.DevicePropertiesController
import com.croniot.android.features.login.data.LoginApiService
import com.croniot.android.features.login.domain.repository.LoginRepository
import croniot.messages.MessageLoginRequest
import croniot.models.LoginResult

class LoginRepositoryImpl(private val apiService: LoginApiService) : LoginRepository {

    override suspend fun login(request: MessageLoginRequest): LoginResult {
        val response = apiService.login(request)
        if (response.isSuccessful) {
            response.body()?.let { return it }
        }
        throw Exception("Login failed: ${response.errorBody()?.string()}")
    }

    override suspend fun logout() {
        SharedPreferences.clearCache()
    }

    override fun getDeviceUuid(): String? {
        return SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_UUID)
    }

    override fun getDeviceToken(): String? {
        return SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_TOKEN)
    }

    override fun getDeviceProperties(): Map<String, String> {
        return DevicePropertiesController.getDeviceDetails() //TODO inject controller
    }
}
