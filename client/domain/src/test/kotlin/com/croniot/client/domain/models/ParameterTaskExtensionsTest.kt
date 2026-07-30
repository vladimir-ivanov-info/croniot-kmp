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
    fun `isStateful is true only for the stateful parameter type`() {
        assertThat(parameter(type = "stateful").isStateful()).isTrue()
        assertThat(parameter(type = "number").isStateful()).isFalse()
    }

    @Test
    fun `isRepresentsSwitch requires exactly state_1 and state_2 constraints`() {
        assertThat(parameter(constraints = mapOf("state_1" to "ON", "state_2" to "OFF")).isRepresentsSwitch()).isTrue()
    }

    @Test
    fun `isRepresentsSwitch is false with extra or missing constraints`() {
        assertThat(parameter(constraints = mapOf("state_1" to "ON")).isRepresentsSwitch()).isFalse()
        assertThat(
            parameter(constraints = mapOf("state_1" to "ON", "state_2" to "OFF", "extra" to "x")).isRepresentsSwitch(),
        ).isFalse()
    }

    @Test
    fun `isRepresentsSlider requires minValue, maxValue and stepSize constraints`() {
        val constraints = mapOf("minValue" to "0", "maxValue" to "100", "stepSize" to "1")
        assertThat(parameter(constraints = constraints).isRepresentsSlider()).isTrue()
    }

    @Test
    fun `isRepresentsSlider is false when any constraint is missing`() {
        val constraints = mapOf("minValue" to "0", "maxValue" to "100")
        assertThat(parameter(constraints = constraints).isRepresentsSlider()).isFalse()
    }

    @Test
    fun `formatValue rounds to an integer string when decimals constraint is zero`() {
        val param = parameter(constraints = mapOf("decimals" to "0"))

        assertThat(param.formatValue(3.7f)).isEqualTo("4")
    }

    @Test
    fun `formatValue keeps the float representation when decimals constraint is absent`() {
        val param = parameter()

        assertThat(param.formatValue(3.5f)).isEqualTo("3.5")
    }

    @Test
    fun `formatValue keeps the float representation when decimals constraint is nonzero`() {
        val param = parameter(constraints = mapOf("decimals" to "2"))

        assertThat(param.formatValue(3.5f)).isEqualTo("3.5")
    }
}
