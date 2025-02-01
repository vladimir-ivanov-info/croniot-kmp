package com.croniot.android.features.login.data.repository

import com.croniot.android.core.data.source.local.DataStoreController
import com.croniot.android.core.util.DevicePropertiesController
import com.croniot.android.features.login.data.LoginApiService
import com.croniot.android.features.login.domain.repository.LoginRepository
import croniot.messages.MessageLoginRequest
import croniot.models.LoginResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginRepositoryImpl(private val apiService: LoginApiService) : LoginRepository {

    override suspend fun login(request: MessageLoginRequest): LoginResult {
        val response = apiService.login(request)
        if (response.isSuccessful) {

            val accountEmail = request.email
            val accountPassword = request.password

            DataStoreController.saveData(DataStoreController.KEY_ACCOUNT_EMAIL, accountEmail)
            DataStoreController.saveData(DataStoreController.KEY_ACCOUNT_PASSWORD, accountPassword)

            response.body()?.let { return it }
        }
        throw Exception("Login failed: ${response.errorBody()?.string()}")
    }

    override suspend fun logout() {
        //SharedPreferences.clearCache()

        CoroutineScope(Dispatchers.IO).launch {
            DataStoreController.clearAllCacheExceptDeviceUuid()
        }
    }

    override suspend fun getDeviceUuid(): String? {
        return DataStoreController.loadData(DataStoreController.KEY_DEVICE_UUID).first()
    }

    override suspend fun getDeviceToken(): String? {
        return DataStoreController.loadData(DataStoreController.KEY_DEVICE_TOKEN).first()
    }

    override fun getDeviceProperties(): Map<String, String> {
        return DevicePropertiesController.getDeviceDetails() //TODO inject controller
    }
}
