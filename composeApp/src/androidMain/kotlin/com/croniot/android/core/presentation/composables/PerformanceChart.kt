package com.croniot.android.core.presentation.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.croniot.android.domain.model.SensorType
import croniot.models.dto.SensorTypeDto
import kotlin.random.Random

private fun getValuePercentageForRange(value: Float, max: Float, min: Float) =
    (value - min) / (max - min)

fun getRandomEntries2(): List<Float> {
    // entryOf(it, Random.nextFloat() * 20f)
    // val random = Random()
    val listSize = 30
    val floatList = MutableList(listSize) { Random.nextFloat() }
    return floatList
}

@Composable
fun PerformanceChart(sensorInfo: SensorType, modifier: Modifier, list: List<Float>) {
    if (list.isEmpty()) return

    val zipList: List<Pair<Float, Float>> = list.zipWithNext()

    Box(modifier = Modifier) {
        Box(modifier = Modifier.height(150.dp)) {
            Row(modifier = modifier.padding(8.dp)) {
                val max = sensorInfo.parameters.first().constraints["maxValue"]?.toFloat()
                val min = sensorInfo.parameters.first().constraints["minValue"]?.toFloat()

                val lineColor = MaterialTheme.colorScheme.inversePrimary

                for (pair in zipList) {
                    var fromValuePercentage = 0.0F
                    var toValuePercentage = 0.0F

                    try {
                        fromValuePercentage = getValuePercentageForRange(pair.first, max!!, min!!)
                        toValuePercentage = getValuePercentageForRange(pair.second, max!!, min!!)
                    } catch (e: Throwable) {
                        println("Error processing chart data: $e")
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        onDraw = {
                            val fromPoint = Offset(x = 0f, y = size.height.times(1 - fromValuePercentage))
                            val toPoint = Offset(x = size.width, y = size.height.times(1 - toValuePercentage))

                            // Control points for cubic Bézier curve
                            val controlPoint1 = Offset(x = size.width * 0.5f, y = fromPoint.y)
                            val controlPoint2 = Offset(x = size.width * 0.5f, y = toPoint.y)

                            drawPath(
                                path = Path().apply {
                                    moveTo(fromPoint.x, fromPoint.y)
                                    cubicTo(
                                        controlPoint1.x,
                                        controlPoint1.y,
                                        controlPoint2.x,
                                        controlPoint2.y,
                                        toPoint.x,
                                        toPoint.y,
                                    )
                                },
                                color = lineColor,
                                style = Stroke(width = 5f),
                            )
                        },
                    )
                }
            }
        }
    }
}