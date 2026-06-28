package com.croniot.client.data.source.local.ble

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptedBleCredentialStore(
    context: Context,
) : BleCredentialStore {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override suspend fun save(deviceUuid: String, username: String, password: String) {
        withContext(Dispatchers.IO) {
            prefs.edit()
                .putString(userKey(deviceUuid), username)
                .putString(passKey(deviceUuid), password)
                .commit()
        }
    }

    override suspend fun get(deviceUuid: String): BleCredentials? = withContext(Dispatchers.IO) {
        val username = prefs.getString(userKey(deviceUuid), null) ?: return@withContext null
        val password = prefs.getString(passKey(deviceUuid), null) ?: return@withContext null
        BleCredentials(username, password)
    }

    override suspend fun forget(deviceUuid: String) {
        withContext(Dispatchers.IO) {
            prefs.edit()
                .remove(userKey(deviceUuid))
                .remove(passKey(deviceUuid))
                .commit()
        }
    }

    override suspend fun forgetAll() {
        withContext(Dispatchers.IO) {
            prefs.edit().clear().commit()
        }
    }

    private fun userKey(uuid: String): String = "cred.$uuid.user"
    private fun passKey(uuid: String): String = "cred.$uuid.pass"

    private companion object {
        const val FILE_NAME = "croniot_ble_credentials"
    }
}
