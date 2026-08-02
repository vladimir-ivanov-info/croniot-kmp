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
    fun `WHEN parameter type is number THEN isNumeric is true, otherwise false`() {
        assertThat(parameter(ParameterTypes.NUMBER).isNumeric()).isTrue()
        assertThat(parameter(ParameterTypes.TIME).isNumeric()).isFalse()
        assertThat(parameter(ParameterTypes.STATEFUL).isNumeric()).isFalse()
    }

    @Test
    fun `WHEN parameter type is time THEN isTime is true, otherwise false`() {
        assertThat(parameter(ParameterTypes.TIME).isTime()).isTrue()
        assertThat(parameter(ParameterTypes.NUMBER).isTime()).isFalse()
        assertThat(parameter(ParameterTypes.STATEFUL).isTime()).isFalse()
    }

    @Test
    fun `WHEN parameter type is stateful THEN isStateful is true, otherwise false`() {
        assertThat(parameter(ParameterTypes.STATEFUL).isStateful()).isTrue()
        assertThat(parameter(ParameterTypes.NUMBER).isStateful()).isFalse()
        assertThat(parameter(ParameterTypes.TIME).isStateful()).isFalse()
    }
}