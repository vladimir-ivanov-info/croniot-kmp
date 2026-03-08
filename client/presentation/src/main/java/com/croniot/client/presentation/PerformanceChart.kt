package com.croniot.client.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.croniot.client.core.models.SensorType

@Composable
fun PerformanceChart(sensorType: SensorType, modifier: Modifier, list: List<Float>) {
    if (list.size < 2) return

    val firstParam = remember(sensorType.uid) { sensorType.parameters.firstOrNull() }
    val max = remember(firstParam) { firstParam?.constraints?.get("maxValue")?.toFloatOrNull() } ?: return
    val min = remember(firstParam) { firstParam?.constraints?.get("minValue")?.toFloatOrNull() } ?: return
    val range = max - min
    if (range == 0f) return

    val lineColor = MaterialTheme.colorScheme.inversePrimary

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        val w = size.width
        val h = size.height
        val segmentCount = list.size - 1

        val path = Path().apply {
            val startY = h * (1f - (list[0] - min) / range)
            moveTo(0f, startY)

            for (i in 0 until segmentCount) {
                val x0 = w * i / segmentCount
                val x1 = w * (i + 1) / segmentCount
                val y0 = h * (1f - (list[i] - min) / range)
                val y1 = h * (1f - (list[i + 1] - min) / range)
                val cx = (x0 + x1) / 2f
                cubicTo(cx, y0, cx, y1, x1, y1)
            }
        }

        drawPath(path, color = lineColor, style = Stroke(width = 8f))
    }
}
