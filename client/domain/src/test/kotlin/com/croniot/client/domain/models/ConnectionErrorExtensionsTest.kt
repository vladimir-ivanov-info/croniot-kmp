package com.croniot.client.domain.models

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ConnectionErrorExtensionsTest {

    @Test
    fun `WHEN error is MqttBrokerUnreachable THEN toUserMessage includes the host`() {
        val error = ConnectionError.MqttBrokerUnreachable(host = "10.0.0.5", cause = "timeout")

        assertThat(error.toUserMessage()).contains("10.0.0.5")
    }

    @Test
    fun `WHEN error is Unknown THEN toUserMessage returns a generic message`() {
        assertThat(ConnectionError.Unknown.toUserMessage()).isEqualTo("Error de conexión desconocido.")
    }
}
