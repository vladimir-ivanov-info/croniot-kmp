package com.croniot.client.domain.models

sealed interface ConnectionError {
    data class MqttBrokerUnreachable(val host: String, val cause: String?) : ConnectionError
    data object Unknown : ConnectionError
}

fun ConnectionError.toUserMessage(): String = when (this) {
    is ConnectionError.MqttBrokerUnreachable -> "No se pudo conectar al broker MQTT ($host)."
    ConnectionError.Unknown -> "Error de conexión desconocido."
}
