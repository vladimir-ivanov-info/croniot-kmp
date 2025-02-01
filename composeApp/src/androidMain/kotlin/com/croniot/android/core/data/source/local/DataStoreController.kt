package com.croniot.android.core.data.source.local

import ZonedDateTimeAdapter
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.croniot.android.core.util.StringUtil
import com.google.gson.GsonBuilder
import croniot.models.dto.AccountDto
import croniot.models.dto.DeviceDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.ZonedDateTime

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_prefs")

object DataStoreController : KoinComponent {

    val context: Context by inject()

    private val dataStore: DataStore<Preferences> by lazy {
        context.dataStore
    }

    val KEY_SERVER_ADDRESS = stringPreferencesKey("server_address")
    val KEY_CONFIGURATION_FOREGROUND_SERVICE = stringPreferencesKey("configuration_foreground_service")
    private val KEY_ACCOUNT = stringPreferencesKey("account")
    private val KEY_SELECTED_DEVICE = stringPreferencesKey("selected_device")
    val KEY_ACCOUNT_EMAIL = stringPreferencesKey("account_email")
    val KEY_ACCOUNT_PASSWORD = stringPreferencesKey("account_password")
    val KEY_DEVICE_TOKEN = stringPreferencesKey("device_token")
    val KEY_DEVICE_UUID = stringPreferencesKey("device_uuid")

    // val KEY_SERVER_MODE = booleanPreferencesKey("server_mode")
    val KEY_SERVER_MODE = stringPreferencesKey("server_mode")
    val KEY_CURRENT_SCREEN = stringPreferencesKey("current_screen")

    suspend fun saveData(key: Preferences.Key<String>, value: String) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun loadData(key: Preferences.Key<String>): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    suspend fun generateAndSaveDeviceUuidIfNotExists() {
        val deviceUuid = loadData(KEY_DEVICE_UUID).first()
        if (deviceUuid == null) {
            val newDeviceUuid = "android_" + StringUtil.generateRandomString(4)
            saveData(KEY_DEVICE_UUID, newDeviceUuid)
        } else {
            println()
        }
    }

    suspend fun saveAccount(account: AccountDto?) {
        if (account == null) {
            dataStore.edit { preferences ->
                preferences.remove(KEY_ACCOUNT)
            }
        } else {
            val gson = GsonBuilder()
                .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
                .setPrettyPrinting()
                .create()
            val accountJson = gson.toJson(account)
            dataStore.edit { preferences ->
                preferences[KEY_ACCOUNT] = accountJson
            }
        }
    }

    fun getAccount(): Flow<AccountDto?> {
        return dataStore.data.map { preferences ->
            val accountJson = preferences[KEY_ACCOUNT]
            if (!accountJson.isNullOrEmpty()) {
                val gson = GsonBuilder()
                    .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
                    .setPrettyPrinting()
                    .create()
                gson.fromJson(accountJson, AccountDto::class.java)
            } else {
                null
            }
        }
    }

    suspend fun saveSelectedDevice(selectedDevice: DeviceDto) {
        val gson = GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
            .setPrettyPrinting()
            .create()
        val deviceJson = gson.toJson(selectedDevice)
        dataStore.edit { preferences ->
            preferences[KEY_SELECTED_DEVICE] = deviceJson
        }
    }

    fun getSelectedDevice(): Flow<DeviceDto?> {
        return dataStore.data.map { preferences ->
            val deviceJson = preferences[KEY_SELECTED_DEVICE]
            if (!deviceJson.isNullOrEmpty()) {
                val gson = GsonBuilder()
                    .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
                    .setPrettyPrinting()
                    .create()
                gson.fromJson(deviceJson, DeviceDto::class.java)
            } else {
                null
            }
        }
    }

    suspend fun clearAllCacheExceptDeviceUuid() {
        dataStore.edit { preferences ->
            // TODO clear everything except deviceUui!!!
            // preferences.clear()
        }
    }
}
