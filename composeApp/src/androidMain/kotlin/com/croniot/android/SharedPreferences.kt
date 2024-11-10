package com.croniot.android

import ZonedDateTimeAdapter
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import croniot.models.dto.AccountDto
import croniot.models.dto.DeviceDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.ZonedDateTime

object SharedPreferences : KoinComponent {

    val context: Context by inject()

    const val KEY_SERVER_ADDRESS = "server_address"

    const val KEY_CONFIGURATION_FOREGROUND_SERVICE = "configuration_foregound_serviec"

    const val KEY_ACCOUNT = "account"
    const val KEY_SELECTED_DEVICE = "selected_device"

    const val KEY_ACCOUNT_EMAIL = "account_email"
    const val KEY_ACCOUNT_PASSWORD = "account_password"

    const val KEY_DEVICE_TOKEN = "device_token"
    const val KEY_DEVICE_UUID = "device_uuid"

    const val KEY_SERVER_MODE = "server_mode"

    private const val PREF_NAME = "secure_prefs"
    private const val SECRET_KEY = "your_secret_key"

    const val KEY_CURRENT_SCREEN = "current_screen"

    fun getSharedPreferences(): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveData(key: String, value: String) {
        val prefs = getSharedPreferences()
        prefs.edit().putString(key, value).apply()
    }

    fun loadData(key: String): String? {
        val prefs = getSharedPreferences()
        return prefs.getString(key, null)
    }

    fun saveAccount(/*context: Context, */account: AccountDto) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val gsonZonedDateTime = GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
            .setPrettyPrinting()
            .create()

        val accountJson = gsonZonedDateTime.toJson(account)

        prefs.edit().putString(KEY_ACCOUNT, accountJson).apply()
    }

    fun getAccout(): AccountDto? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val gsonZonedDateTime = GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
            .setPrettyPrinting()
            .create()

        var accountDto : AccountDto? = null

        val accountJson = prefs.getString(KEY_ACCOUNT, null)

        if (accountJson != null) {
            if(accountJson.isNotEmpty()){
                accountDto = gsonZonedDateTime.fromJson(accountJson, AccountDto::class.java)
            }
        }
        return accountDto
    }

    fun saveSelectedDevice(selectedDevice: DeviceDto) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val gsonZonedDateTime = GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
            .setPrettyPrinting()
            .create()

        val accountJson = gsonZonedDateTime.toJson(selectedDevice)

        prefs.edit().putString(KEY_SELECTED_DEVICE, accountJson).apply()
    }

    fun getSelectedDevice(): DeviceDto? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val gsonZonedDateTime = GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
            .setPrettyPrinting()
            .create()

        var selectedDeviceDto : DeviceDto? = null

        val accountJson = prefs.getString(KEY_SELECTED_DEVICE, null)

        if (accountJson != null) {
            if(accountJson.isNotEmpty()){
                selectedDeviceDto = gsonZonedDateTime.fromJson(accountJson, DeviceDto::class.java)
            }
        }
        return selectedDeviceDto
    }
}