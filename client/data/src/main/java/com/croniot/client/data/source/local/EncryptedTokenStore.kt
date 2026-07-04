package com.croniot.client.data.source.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.croniot.client.domain.models.auth.AuthTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptedTokenStore(context: Context) : TokenStore {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun saveTokens(tokens: AuthTokens) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_ACCESS, tokens.accessToken)
            .putString(KEY_REFRESH, tokens.refreshToken)
            .putLong(KEY_EXPIRES_AT, tokens.expiresAtEpochSeconds)
            .apply()
    }

    override suspend fun getTokens(): AuthTokens? = withContext(Dispatchers.IO) {
        val access = prefs.getString(KEY_ACCESS, null) ?: return@withContext null
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return@withContext null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, -1L).takeIf { it >= 0 } ?: return@withContext null
        AuthTokens(
            accessToken = access,
            refreshToken = refresh,
            expiresAtEpochSeconds = expiresAt,
        )
    }

    override suspend fun clearTokens() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_FILE = "croniot_auth_tokens"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at_epoch_seconds"
    }
}