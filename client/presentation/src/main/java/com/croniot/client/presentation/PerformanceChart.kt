package com.croniot.client.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.croniot.client.core.models.SensorType

private const val ANIM_DURATION_MS = 1000

@Composable
fun PerformanceChart(sensorType: SensorType, modifier: Modifier, list: List<Float>) {
    if (list.size < 2) return

    val firstParam = remember(sensorType.uid) { sensorType.parameters.firstOrNull() }
    val max = remember(firstParam) { firstParam?.constraints?.get("maxValue")?.toFloatOrNull() } ?: return
    val min = remember(firstParam) { firstParam?.constraints?.get("minValue")?.toFloatOrNull() } ?: return
    val range = max - min
    if (range == 0f) return

    val visiblePointCount = list.size
    val buffer = remember { mutableStateListOf<Float>() }
    val scrollAnim = remember { Animatable(0f) }

    LaunchedEffect(list.toList()) {
        // If a previous animation was interrupted, clean up the extra point
        if (buffer.size > visiblePointCount) {
            buffer.removeAt(0)
            scrollAnim.snapTo(0f)
        }

        if (buffer.isEmpty()) {
            buffer.addAll(list)
            return@LaunchedEffect
        }

        val sliding = list.size == buffer.size && list != buffer.toList()

        if (sliding) {
            // Window slid: prepend old first point → buffer has N+1 points
            val oldFirst = buffer.first()
            buffer.clear()
            buffer.add(oldFirst)
            buffer.addAll(list)
            scrollAnim.snapTo(0f)
            scrollAnim.animateTo(1f, tween(ANIM_DURATION_MS, easing = LinearEasing))
            // Scroll done: trim the old point
            buffer.removeAt(0)
            scrollAnim.snapTo(0f)
        } else {
            // List is growing (hasn't hit max yet) or reset — just update
            buffer.clear()
            buffer.addAll(list)
            scrollAnim.snapTo(0f)
        }
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary
    val ceilingColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        val w = size.width
        val h = size.height
        val points = buffer.toList()
        if (points.size < 2) return@Canvas

        val visibleSegments = visiblePointCount - 1
        val segmentWidth = w / visibleSegments
        val scrollPx = if (points.size > visiblePointCount) scrollAnim.value * segmentWidth else 0f

        val linePath = Path().apply {
            val startY = h * (1f - (points[0] - min) / range)
            moveTo(-scrollPx, startY)

            for (i in 0 until points.size - 1) {
                val x0 = segmentWidth * i - scrollPx
                val x1 = segmentWidth * (i + 1) - scrollPx
                val y0 = h * (1f - (points[i] - min) / range)
                val y1 = h * (1f - (points[i + 1] - min) / range)
                val cx = (x0 + x1) / 2f
                cubicTo(cx, y0, cx, y1, x1, y1)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            val lastX = segmentWidth * (points.size - 1) - scrollPx
            lineTo(lastX, h)
            lineTo(-scrollPx, h)
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