package com.croniot.client.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.croniot.client.core.models.SensorType

private const val ANIM_DURATION_MS = 900

@Composable
fun PerformanceChart(sensorType: SensorType, modifier: Modifier, list: List<Float>) {
    if (list.size < 2) return

    val firstParam = remember(sensorType.uid) { sensorType.parameters.firstOrNull() }
    val max = remember(firstParam) { firstParam?.constraints?.get("maxValue")?.toFloatOrNull() } ?: return
    val min = remember(firstParam) { firstParam?.constraints?.get("minValue")?.toFloatOrNull() } ?: return
    val range = max - min
    if (range == 0f) return

    // Each value animates smoothly to its new target
    val animatedList = list.map { value ->
        animateFloatAsState(
            targetValue = value,
            animationSpec = tween(durationMillis = ANIM_DURATION_MS, easing = LinearEasing),
            label = "chart-value",
        ).value
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary
    val ceilingColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier.fillMaxSize(),
    ) {
        val w = size.width
        val h = size.height
        val segmentCount = animatedList.size - 1

        val linePath = Path().apply {
            val startY = h * (1f - (animatedList[0] - min) / range)
            moveTo(0f, startY)

            for (i in 0 until segmentCount) {
                val x0 = w * i / segmentCount
                val x1 = w * (i + 1) / segmentCount
                val y0 = h * (1f - (animatedList[i] - min) / range)
                val y1 = h * (1f - (animatedList[i + 1] - min) / range)
                val cx = (x0 + x1) / 2f
                cubicTo(cx, y0, cx, y1, x1, y1)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), phase = 0f)
        drawLine(
            color = ceilingColor.copy(alpha = 0.25f),
            start = Offset(0f, 0f),
            end = Offset(w, 0f),
            strokeWidth = 1.5f,
            pathEffect = dashEffect,
        )

        val gradient = Brush.verticalGradient(
            colors = listOf(fillColor, Color.Transparent),
        )
        drawPath(fillPath, brush = gradient, style = Fill)
        drawPath(linePath, color = lineColor, style = Stroke(width = 3.5f))
    }
}