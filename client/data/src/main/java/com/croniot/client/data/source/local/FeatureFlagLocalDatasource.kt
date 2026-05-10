package com.croniot.client.data.source.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import croniot.messages.MessageFactory
import croniot.models.dto.FeatureFlagDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.featureFlagDataStore by preferencesDataStore(name = "feature_flags")

class FeatureFlagLocalDatasource(context: Context) {

    private val dataStore = context.featureFlagDataStore

    private companion object {
        val KEY_FLAGS = stringPreferencesKey("flags_json")
    }

    suspend fun saveFlags(flags: List<FeatureFlagDto>) {
        val json = MessageFactory.toJson(flags)
        dataStore.edit { it[KEY_FLAGS] = json }
    }

    suspend fun updateFlag(name: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_FLAGS]
                ?.let { runCatching { MessageFactory.fromJson<List<FeatureFlagDto>>(it) }.getOrNull() }
                ?: emptyList()
            val updated = current.map { if (it.name == name) it.copy(enabled = enabled) else it }
            prefs[KEY_FLAGS] = MessageFactory.toJson(updated)
        }
    }

    fun observeFlags(): Flow<List<FeatureFlagDto>> =
        dataStore.data.map { prefs ->
            prefs[KEY_FLAGS]
                ?.let { runCatching { MessageFactory.fromJson<List<FeatureFlagDto>>(it) }.getOrNull() }
                ?: emptyList()
        }
}
