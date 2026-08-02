package com.croniot.client.domain.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class ParameterTaskExtensionsTest {

    private fun parameter(type: String = "number", constraints: Map<String, String> = emptyMap()) = ParameterTask(
        uid = 1L,
        name = "param",
        type = type,
        unit = "",
        description = "",
        constraints = constraints,
    )

    @Test
    fun `WHEN parameter type is stateful THEN isStateful is true, otherwise false`() {
        assertThat(parameter(type = "stateful").isStateful()).isTrue()
        assertThat(parameter(type = "number").isStateful()).isFalse()
    }

    @Test
    fun `WHEN constraints are exactly state_1 and state_2 THEN isRepresentsSwitch is true`() {
        assertThat(parameter(constraints = mapOf("state_1" to "ON", "state_2" to "OFF")).isRepresentsSwitch()).isTrue()
    }

    @Test
    fun `WHEN constraints have extra or missing keys THEN isRepresentsSwitch is false`() {
        assertThat(parameter(constraints = mapOf("state_1" to "ON")).isRepresentsSwitch()).isFalse()
        assertThat(
            parameter(constraints = mapOf("state_1" to "ON", "state_2" to "OFF", "extra" to "x")).isRepresentsSwitch(),
        ).isFalse()
    }

    @Test
    fun `WHEN constraints include minValue, maxValue and stepSize THEN isRepresentsSlider is true`() {
        val constraints = mapOf("minValue" to "0", "maxValue" to "100", "stepSize" to "1")
        assertThat(parameter(constraints = constraints).isRepresentsSlider()).isTrue()
    }

    @Test
    fun `WHEN any required constraint is missing THEN isRepresentsSlider is false`() {
        val constraints = mapOf("minValue" to "0", "maxValue" to "100")
        assertThat(parameter(constraints = constraints).isRepresentsSlider()).isFalse()
    }

    @Test
    fun `WHEN decimals constraint is zero THEN formatValue rounds to an integer string`() {
        val param = parameter(constraints = mapOf("decimals" to "0"))

        assertThat(param.formatValue(3.7f)).isEqualTo("4")
    }

    @Test
    fun `WHEN decimals constraint is absent THEN formatValue keeps the float representation`() {
        val param = parameter()

        assertThat(param.formatValue(3.5f)).isEqualTo("3.5")
    }

    @Test
    fun `WHEN decimals constraint is nonzero THEN formatValue keeps the float representation`() {
        val param = parameter(constraints = mapOf("decimals" to "2"))

        assertThat(param.formatValue(3.5f)).isEqualTo("3.5")
    }
}
