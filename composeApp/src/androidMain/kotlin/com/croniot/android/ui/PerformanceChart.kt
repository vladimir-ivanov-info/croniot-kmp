package com.croniot.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import croniot.models.dto.SensorDto
import kotlin.random.Random

private fun getValuePercentageForRange(value: Float, max: Float, min: Float) =
    (value - min) / (max - min)

fun getRandomEntries2() : List<Float> {
    // entryOf(it, Random.nextFloat() * 20f)
    //val random = Random()
    val listSize = 30
    val floatList = MutableList(listSize) { Random.nextFloat() }
    return floatList
}

@Composable
//fun PerformanceChart(modifier: Modifier = Modifier, list: List<Float> = listOf(10f, 20f, 3f, 1f)) {
fun PerformanceChart(sensorInfo: SensorDto, modifier: Modifier, list: List<Float>) {

    if(list.isEmpty()) return

    val zipList: List<Pair<Float, Float>> = list.zipWithNext()
    Box(modifier = Modifier){
        Box(modifier = Modifier.height(150.dp)) {

            Row(modifier = modifier.padding(8.dp)) {

                val max = sensorInfo.parameters.first().constraints.get("maxValue")?.toFloat() //TODO adapt for when there are many
                val min = sensorInfo.parameters.first().constraints.get("minValue")?.toFloat() //TODO adapt for when there are many

               // if (list.last() > list.first()) Color.Cyan else Color.Red // <-- Line color is Green if its going up and Red otherwise
                val lineColor = MaterialTheme.colorScheme.inversePrimary

                for (pair in zipList) {

                    var fromValuePercentage : Float = 0.0F
                    var toValuePercentage : Float = 0.0F

                    try{
                        fromValuePercentage = getValuePercentageForRange(pair.first, max!!, min!!)
                        toValuePercentage = getValuePercentageForRange(pair.second, max!!, min!!)
                    }catch (e: Throwable){
                        println()
                    }

                   // val fromValuePercentage = getValuePercentageForRange(pair.first, max!!, min!!)
                   // val toValuePercentage = getValuePercentageForRange(pair.second, max!!, min!!)

                   // val fromValuePercentage = getValuePercentageForRange(pair.first, max!!, min!!)
                  //  val toValuePercentage = getValuePercentageForRange(pair.second, max!!, min!!)

                    Canvas(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        onDraw = {
                            val fromPoint = Offset(x = 0f, y = size.height.times(1 - fromValuePercentage)) // <-- Use times so it works for any available space
                            val toPoint =
                                Offset(x = size.width, y = size.height.times(1 - toValuePercentage)) // <-- Also here!

                            /*drawLine(
                                color = lineColor,
                                start = fromPoint,
                                end = toPoint,
                                strokeWidth = 3f
                            )*/

                            // Control points for cubic Bézier curve
                            val controlPoint1 = Offset(x = size.width * 0.5f, y = fromPoint.y)
                            val controlPoint2 = Offset(x = size.width * 0.5f, y = toPoint.y)

                            drawPath(
                                path = Path().apply {
                                    moveTo(fromPoint.x, fromPoint.y)
                                    cubicTo(
                                        controlPoint1.x, controlPoint1.y,
                                        controlPoint2.x, controlPoint2.y,
                                        toPoint.x, toPoint.y
                                    )
                                },
                                color = lineColor,
                                style = Stroke(width = 5f)
                            )
                        })
                }
            }
        }
    }
}