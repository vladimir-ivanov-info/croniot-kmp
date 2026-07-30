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
    fun `isChartable is true when the first parameter has both minValue and maxValue`() {
        val type = sensorType(setOf(parameter(mapOf("minValue" to "0", "maxValue" to "100"))))

        assertThat(type.isChartable()).isTrue()
    }

    @Test
    fun `isChartable is false when minValue or maxValue is missing`() {
        assertThat(sensorType(setOf(parameter(mapOf("minValue" to "0")))).isChartable()).isFalse()
        assertThat(sensorType(setOf(parameter(emptyMap()))).isChartable()).isFalse()
    }

    @Test
    fun `isChartable is false when there are no parameters`() {
        assertThat(sensorType(emptySet()).isChartable()).isFalse()
    }
}
