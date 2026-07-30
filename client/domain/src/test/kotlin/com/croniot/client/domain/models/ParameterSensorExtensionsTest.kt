package com.croniot.client.domain.models

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import croniot.models.ParameterTypes
import org.junit.jupiter.api.Test

class ParameterSensorExtensionsTest {

    private fun parameter(type: String) = ParameterSensor(
        uid = 1L,
        name = "param",
        type = type,
        unit = "",
        description = "",
        constraints = emptyMap(),
    )

    @Test
    fun `isNumeric is true only for the number parameter type`() {
        assertThat(parameter(ParameterTypes.NUMBER).isNumeric()).isTrue()
        assertThat(parameter(ParameterTypes.TIME).isNumeric()).isFalse()
        assertThat(parameter(ParameterTypes.STATEFUL).isNumeric()).isFalse()
    }

    @Test
    fun `isTime is true only for the time parameter type`() {
        assertThat(parameter(ParameterTypes.TIME).isTime()).isTrue()
        assertThat(parameter(ParameterTypes.NUMBER).isTime()).isFalse()
        assertThat(parameter(ParameterTypes.STATEFUL).isTime()).isFalse()
    }

    @Test
    fun `isStateful is true only for the stateful parameter type`() {
        assertThat(parameter(ParameterTypes.STATEFUL).isStateful()).isTrue()
        assertThat(parameter(ParameterTypes.NUMBER).isStateful()).isFalse()
        assertThat(parameter(ParameterTypes.TIME).isStateful()).isFalse()
    }
}