package com.croniot.client.domain.models

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class SensorTypeExtensionsTest {

    private fun parameter(constraints: Map<String, String>) = ParameterSensor(
        uid = 1L,
        name = "param",
        type = "number",
        unit = "",
        description = "",
        constraints = constraints,
    )

    private fun sensorType(parameters: Set<ParameterSensor>) = SensorType(
        uid = 1L,
        name = "sensor",
        description = "",
        parameters = parameters,
    )

    @Test
    fun `WHEN the first parameter has both minValue and maxValue THEN isChartable is true`() {
        val type = sensorType(setOf(parameter(mapOf("minValue" to "0", "maxValue" to "100"))))

        assertThat(type.isChartable()).isTrue()
    }

    @Test
    fun `WHEN minValue or maxValue is missing THEN isChartable is false`() {
        assertThat(sensorType(setOf(parameter(mapOf("minValue" to "0")))).isChartable()).isFalse()
        assertThat(sensorType(setOf(parameter(emptyMap()))).isChartable()).isFalse()
    }

    @Test
    fun `WHEN there are no parameters THEN isChartable is false`() {
        assertThat(sensorType(emptySet()).isChartable()).isFalse()
    }
}
