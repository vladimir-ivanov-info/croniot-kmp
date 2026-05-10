package com.croniot.client.data.repositories

import MqttHandler
import com.croniot.client.core.config.ServerConfig
import com.croniot.client.core.util.StringUtil.generateUniqueString
import com.croniot.client.data.source.local.FeatureFlagLocalDatasource
import com.croniot.client.data.source.local.ServerConfigLocalDatasource
import com.croniot.client.data.source.remote.http.FeatureFlagApi
import com.croniot.client.data.source.remote.mqtt.MqttDataProcessorFeatureFlag
import com.croniot.client.data.util.TaggingSocketFactory
import com.croniot.client.domain.repositories.FeatureFlagRepository
import croniot.models.MqttTopics
import croniot.models.dto.FeatureFlagDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import java.util.concurrent.ConcurrentHashMap

class FeatureFlagRepositoryImpl(
    private val api: FeatureFlagApi,
    private val localDatasource: FeatureFlagLocalDatasource,
    private val serverConfigLocalDatasource: ServerConfigLocalDatasource,
    private val appScope: CoroutineScope,
) : FeatureFlagRepository {

    private val flagCache = ConcurrentHashMap<String, Boolean>()
    private var mqttHandler: MqttHandler? = null

    override suspend fun fetchAndCache(): Result<Unit> = runCatching {
        val flags = api.fetchAll()
        localDatasource.saveFlags(flags)
        flags.forEach { flagCache[it.name] = it.enabled }
    }

    override fun observeFlags(): Flow<List<FeatureFlagDto>> =
        localDatasource.observeFlags().onEach { flags ->
            flags.forEach { flagCache[it.name] = it.enabled }
        }

    override fun isEnabled(name: String): Boolean = flagCache[name] ?: false

    override fun startMqttListener() {
        appScope.launch(Dispatchers.IO) {
            mqttHandler?.disconnect()

            val ip = serverConfigLocalDatasource.getServerIp().first()
                ?: ServerConfig.DEFAULT_MQTT_HOST
            val brokerUrl = "tcp://${ip}:${ServerConfig.MQTT_PORT}"
            val clientId = ServerConfig.mqttClientId + "FF" + generateUniqueString(8)
            val mqttClient = MqttClient(brokerUrl, clientId, null)

            mqttHandler = MqttHandler(
                mqttClient = mqttClient,
                mqttDataProcessor = MqttDataProcessorFeatureFlag { flag ->
                    flagCache[flag.name] = flag.enabled
                    appScope.launch {
                        localDatasource.updateFlag(flag.name, flag.enabled)
                    }
                },
                topic = MqttTopics.FEATURE_FLAG_UPDATES_WILDCARD,
                scope = appScope,
                socketFactory = TaggingSocketFactory(),
            )
        }
    }
}
