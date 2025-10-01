package com.croniot.client.core.util

import kotlin.math.roundToInt

object TaskUtil {

    fun formatNumber(value: Float, constraints: /*Mutable*/Map<String, String>): String {
        var result: String = value.toString()

        val decimals = constraints["decimals"]

        if (decimals != null) {
            if (decimals.toInt() == 0) {
                result = value.roundToInt().toString()
            }
        }

        return result
    }
}
