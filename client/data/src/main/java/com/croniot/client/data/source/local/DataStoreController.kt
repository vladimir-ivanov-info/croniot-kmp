package com.croniot.client.data.source.local

import ZonedDateTimeAdapter
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.croniot.client.core.models.Account
import com.croniot.client.core.models.Device
import com.croniot.client.core.util.StringUtil
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.ZonedDateTime

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_prefs")

class DataStoreController : LocalDatasource, KoinComponent {

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

    val KEY_SERVER_MODE = stringPreferencesKey("server_mode")
    val KEY_CURRENT_SCREEN = stringPreferencesKey("current_screen")

    val KEY_CURRENT_ROUTE = stringPreferencesKey("current_route")

    private fun provideGson() = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
        .setPrettyPrinting()
        .create()

    override suspend fun getCurrentRoute() : String? {
        return loadData(KEY_CURRENT_ROUTE).firstOrNull()
    }

    override suspend fun saveCurrentRoute(route: String) {
        saveData(KEY_CURRENT_ROUTE, route)
    }


    override suspend fun getCurrentPassword() : String? {
        return loadData(KEY_ACCOUNT_PASSWORD).firstOrNull()
    }

    override suspend fun getLocalDeviceUuid() : String? {
        return loadData(KEY_DEVICE_UUID).firstOrNull()
    }

    private suspend fun saveData(key: Preferences.Key<String>, value: String) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun loadData(key: Preferences.Key<String>): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    override suspend fun getIsForegroundServiceEnabled() : Boolean {
        return loadData(KEY_CONFIGURATION_FOREGROUND_SERVICE).first() == "true"
    }

    override suspend fun saveIsForegroundServiceEnabled(foregroundServiceEnabled: Boolean) {
        var stringValue = "true"
        if(!foregroundServiceEnabled){
            stringValue = "false"
        }
        saveData(KEY_CONFIGURATION_FOREGROUND_SERVICE, stringValue)
    }

    override suspend fun getServerAddress() : String {
        return loadData(KEY_SERVER_ADDRESS).first() ?: "192.168.50.163" //TODO
    }

    override suspend fun saveServerAddress(serverAddress: String){
        saveData(KEY_SERVER_ADDRESS, serverAddress)
    }

    override suspend fun generateAndSaveDeviceUuidIfNotExists() {
        val deviceUuid = loadData(KEY_DEVICE_UUID).first()
        if (deviceUuid == null) {
            val newDeviceUuid = "android_" + StringUtil.generateRandomString(4)
            saveData(KEY_DEVICE_UUID, newDeviceUuid)
        } else {
            println()
        }
    }

    override suspend fun saveCurrentAccount(account: Account?) {
        if (account == null) {
            dataStore.edit { preferences ->
                preferences.remove(KEY_ACCOUNT)
            }
        } else {
            val accountJson = provideGson().toJson(account)

            dataStore.edit { preferences ->
                preferences[KEY_ACCOUNT] = accountJson
            }
        }
    }

    override suspend fun saveEmail(email: String){
        saveData(KEY_ACCOUNT_EMAIL, email)
    }

    override suspend fun savePassword(password: String){
        saveData(KEY_ACCOUNT_PASSWORD, password)
    }

    override suspend fun saveToken(token: String){
        saveData(KEY_DEVICE_TOKEN, token)
    }

    override suspend fun getCurrentScreen() : String? {
        return loadData(KEY_CURRENT_SCREEN).first()
    }

    override suspend fun saveCurrentScreen(screen: String) {
        saveData(KEY_CURRENT_SCREEN, screen)
    }

    override suspend fun getCurrentAccount(): Account? {
        return dataStore.data.map { preferences ->
            val accountJson = preferences[KEY_ACCOUNT]
            if (!accountJson.isNullOrEmpty()) {
                try{
                    provideGson().fromJson(accountJson, Account::class.java)
                } catch(e: JsonSyntaxException){
                    null
                }
            } else {
                null
            }
        }.firstOrNull()
    }


    override suspend fun saveSelectedDevice(device: Device) {
        val gson = GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
            .setPrettyPrinting()
            .create()
        val deviceJson = gson.toJson(device)
        dataStore.edit { preferences ->
            preferences[KEY_SELECTED_DEVICE] = deviceJson
        }
    }

    override suspend fun getSelectedDevice(): Device? {
        return dataStore.data.map { preferences ->
            val deviceJson = preferences[KEY_SELECTED_DEVICE]
            if (!deviceJson.isNullOrEmpty()) {
                val gson = GsonBuilder()
                    .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
                    .setPrettyPrinting()
                    .create()
                try{
                    gson.fromJson(deviceJson, Device::class.java)
                } catch(e: JsonSyntaxException){
                    null
                }
            } else {
                null
            }
        }.firstOrNull()
    }

    override suspend fun getLocalDeviceToken() : String? {
        return loadData(KEY_DEVICE_TOKEN).firstOrNull()
    }

    override fun getServerMode() : Flow<String?> {
        return loadData(KEY_SERVER_MODE)
    }

    override suspend fun clearAllCacheExceptDeviceUuid() { //TODO and serverMode
        dataStore.edit { preferences ->
            val deviceUuid = preferences[KEY_DEVICE_UUID]
            val serverMode = preferences[KEY_SERVER_MODE]

            preferences.clear()

            deviceUuid?.let { preferences[KEY_DEVICE_UUID] = it }
            serverMode?.let { preferences[KEY_SERVER_MODE] = it }
        }
    }


    override suspend fun getCurrentServerMode(): Flow<String?> {
        return loadData(KEY_SERVER_MODE)
    }

    override suspend fun saveServerMode(serverMode: String){
        saveData(KEY_SERVER_MODE, serverMode)
    }
}
