package com.croniot.android

import android.content.Context
import android.content.SharedPreferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object SharedPreferences : KoinComponent {

    val context: Context by inject()

    const val KEY_SERVER_ADDRESS = "server_address"


    const val KEY_ACCOUNT_EMAIL = "account_email"
    const val KEY_ACCOUNT_PASSWORD = "account_password"

    const val KEY_DEVICE_TOKEN = "device_token"
    const val KEY_DEVICE_UUID = "device_uuid"

    private const val PREF_NAME = "secure_prefs"
    private const val SECRET_KEY = "your_secret_key"

    fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveData(key: String, value: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putString(key, value).apply()
    }

    fun loadData(key: String): String? {
        val prefs = getSharedPreferences(context)
        return prefs.getString(key, null)
    }
}