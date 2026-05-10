package com.croniot.client.data.source.remote.mqtt

import android.util.Log
import croniot.messages.MessageFactory
import croniot.models.MqttDataProcessor
import croniot.models.dto.FeatureFlagDto

class MqttDataProcessorFeatureFlag(
    private val onFlagUpdate: (FeatureFlagDto) -> Unit,
) : MqttDataProcessor {

    override fun process(topic: String, data: Any) {
        try {
            val flagName = parseFlagName(topic) ?: return
            val dto = MessageFactory.fromJson<FeatureFlagDto>(data as String)
            Log.d("FeatureFlag", "MQTT update: ${dto.name}=${dto.enabled}")
            onFlagUpdate(dto.copy(name = flagName))
        } catch (e: Exception) {
            Log.e("FeatureFlag", "Failed to process message on topic=$topic", e)
        }
    }

    private fun parseFlagName(topic: String): String? {
        // /server/feature_flags/{flagName}
        val parts = topic.trim('/').split('/')
        if (parts.size != 3) return null
        if (parts[0] != "server" || parts[1] != "feature_flags") return null
        return parts[2].takeIf { it.isNotEmpty() }
    }
}
