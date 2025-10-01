package com.croniot.client.features.sensors.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.core.Constants
import com.croniot.client.presentation.constants.UtilUi
import com.croniot.client.core.models.SensorData
import com.croniot.client.core.models.Device
import com.croniot.client.core.models.SensorType
import com.croniot.client.presentation.PerformanceChart
import java.time.ZonedDateTime

/*
@Composable
fun SensorItem(
    sensor: SensorType,
    selectedDevice: Device,
    sensorItemViewModel: SensorItemViewModel
) {
    val deviceUuid = remember { selectedDevice.uuid }

    // 1) Usa collectAsStateWithLifecycle para evitar recomposiciones extra
    val sensorDataFlow = remember {
        sensorItemViewModel.observeLiveSensorData(sensor.uid, deviceUuid).collectAsState()
    }

    //val sensorData = sensorItemViewModel.observeLiveSensorData(sensor.uid, deviceUuid).collectAsState()

    val sensorData by sensorDataFlow.collectAsState(
        // Si puedes, usa collectAsStateWithLifecycle de lifecycle-runtime-compose
        initial = SensorData(
            deviceUuid = deviceUuid,
            sensorTypeUid = sensor.uid,
            //sensorUid = sensor.uid,
            value = Constants.PARAMETER_VALUE_UNDEFINED,
            timeStamp = ZonedDateTime.now()
        )
    )

    // 2) Mantén el buffer del gráfico estable y solo cámbialo en efectos
    val chartValues = remember { mutableStateListOf<SensorData>() }

    // Carga inicial (una vez por sensor / device)
    LaunchedEffect(Unit) {
        withFrameNanos { /* aquí ya está en pantalla */ }

        val initial = sensorItemViewModel.getInitialChartData(sensor.uid, deviceUuid)
        chartValues.clear()
        chartValues.addAll(initial.takeLast(50)) // ya vienen en orden; evita el reversed() si no es necesario
    }


    // 3) Añade el nuevo punto SOLO cuando cambie la muestra, y nunca durante la composición
    LaunchedEffect(sensorData.timeStamp) {
        if (sensorData.value != Constants.PARAMETER_VALUE_UNDEFINED) {
            val lastTime = chartValues.lastOrNull()?.timeStamp
            if (lastTime != sensorData.timeStamp) {
                chartValues.add(sensorData)
                if (chartValues.size > 50) chartValues.removeAt(0)
            }
        }
    }

    // 4) Calcula derivados con derivedStateOf para no repetir trabajo
    val firstParam = remember(sensor.parameters) { sensor.parameters.firstOrNull() }
    val sensorName = firstParam?.name ?: ""
    val sensorUnit = firstParam?.unit ?: ""

    val valueText by remember(sensorData.value, sensorUnit) {
        derivedStateOf {
            if (sensorData.value == Constants.PARAMETER_VALUE_UNDEFINED) "" else "${sensorData.value} $sensorUnit"
        }
    }

    // Evita map() en cada recomposición; hazlo solo cuando cambie chartValues
    /*val latestSensorValues: List<Float> by remember(chartValues) {
        derivedStateOf {
            chartValues.mapNotNull { it.value.toFloatOrNull() }
        }
    }*/

    val latestSensorValues by remember(chartValues) {
        derivedStateOf { chartValues.mapNotNull { it.value.toFloatOrNull() } }
    }

    val accessibilityManager = LocalAccessibilityManager.current


    val spokenSensorItem = remember(sensorName, valueText) {
        // Usa unidad “humana” si procede: “vatios”, “grados”
        "$sensorName: $valueText"
    }


    // 5) UI liviana: evita trabajo extra en layout/medición (altura fija, sin fillMaxSize en el Box si no hace falta)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
        ,
        elevation = CardDefaults.elevatedCardElevation(),
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .semantics(mergeDescendants = true) {
                    contentDescription = spokenSensorItem
                }
                ,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = valueText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.Center)
                    .clearAndSetSemantics { } // <- sin semántica propia,

            )

            Text(
                text = sensorName,
                fontSize = UtilUi.TEXT_SIZE_3,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.BottomCenter)
                    .clearAndSetSemantics { }
            )

            if(isChartable(sensor)){
                PerformanceChart(
                    sensorType = sensor,
                    modifier = Modifier.clearAndSetSemantics { }, //TODO poner un tamaño fijo si puedes para ahorrar layout passes
                    list = latestSensorValues
                )
            }

        }
    }

}*/







@Composable
fun SensorItem(
    sensor: SensorType,
    selectedDevice: Device,
    sensorsViewModel: SensorsViewModel
) {
    val deviceUuid = selectedDevice.uuid
    val sensorUid = sensor.uid

    /*LaunchedEffect(deviceUuid) {
        sensorsViewModel.startListening(selectedDevice)
    }

    val liveFlow = sensorsViewModel.listenSensorData(sensorUid, deviceUuid)

    val sensorData by liveFlow.collectAsStateWithLifecycle(
        initialValue = SensorData(
            deviceUuid = deviceUuid,
            sensorTypeUid = sensorUid,
            value = Constants.PARAMETER_VALUE_UNDEFINED,
            timeStamp = ZonedDateTime.now()
        )
    )*/

    val sensorDataFlow = sensorsViewModel.listenSensorData(sensorUid, deviceUuid).collectAsState()
    val sensorData = sensorDataFlow.value

    // 1) Live value
    /*val liveFlow = remember(deviceUuid, sensorUid) {
        sensorsViewModel.listenSensorData(sensorUid, deviceUuid)
    }
    val sensorData by liveFlow.collectAsStateWithLifecycle(
        initialValue = SensorData(
            deviceUuid = deviceUuid,
            sensorTypeUid = sensorUid,
            value = Constants.PARAMETER_VALUE_UNDEFINED,
            timeStamp = ZonedDateTime.now()
        )
    )*/

    // 2) Buffer para el chart
    val chartValues = remember { mutableStateListOf<SensorData>() }

    // Carga inicial de histórico
    LaunchedEffect(deviceUuid, sensorUid) {
        val initial = sensorsViewModel.getInitialChartData(sensorUid, deviceUuid)
        chartValues.clear()
        chartValues.addAll(initial.takeLast(50))
    }

    // Añadir nuevos puntos cuando cambie el dato vivo
    LaunchedEffect(sensorData.timeStamp) {
        if (sensorData.value != Constants.PARAMETER_VALUE_UNDEFINED) {
            val lastTime = chartValues.lastOrNull()?.timeStamp
            if (lastTime != sensorData.timeStamp) {
                chartValues.add(sensorData)
                if (chartValues.size > 50) chartValues.removeAt(0)
            }
        }
    }

    // … tu UI (igual que ya la tienes)


    val firstParam = remember(sensor.parameters) { sensor.parameters.firstOrNull() }
    val sensorName = firstParam?.name ?: ""
    val sensorUnit = firstParam?.unit ?: ""

    val valueText by remember(sensorData.value, sensorUnit) {
        derivedStateOf {
            if (sensorData.value == Constants.PARAMETER_VALUE_UNDEFINED) "" else "${sensorData.value} $sensorUnit"
        }
    }

    val spokenSensorItem = remember(sensorName, valueText) {
        // Usa unidad “humana” si procede: “vatios”, “grados”
        "$sensorName: $valueText"
    }


    val latestSensorValues by remember(chartValues) {
        derivedStateOf { chartValues.mapNotNull { it.value.toFloatOrNull() } }
    }


    // 5) UI liviana: evita trabajo extra en layout/medición (altura fija, sin fillMaxSize en el Box si no hace falta)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            //.height(100.dp)
            .height(80.dp)

        ,
       // elevation = CardDefaults.elevatedCardElevation(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                //.background(MaterialTheme.colorScheme.primaryContainer)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics(mergeDescendants = true) {
                    contentDescription = spokenSensorItem
                }
            ,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = valueText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.Center)
                    .clearAndSetSemantics { } // <- sin semántica propia,

            )

            Text(
                text = sensorName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = UtilUi.TEXT_SIZE_3,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .align(Alignment.BottomCenter)
                    .clearAndSetSemantics { }
            )

            if(isChartable(sensor)){
                PerformanceChart(
                    sensorType = sensor,
                    modifier = Modifier.clearAndSetSemantics { }, //TODO poner un tamaño fijo si puedes para ahorrar layout passes
                    list = latestSensorValues
                )
            }
        }

        /*Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Valor principal destacado
            Text(
                text = buildString {
                    append(valueText)
                    //if (!sensorUnit.isNullOrBlank()) append(" $sensorUnit")
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Nombre del sensor más discreto
            Text(
                text = sensorName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            )
        }*/

    }
}





fun isChartable(sensorType: SensorType) : Boolean {
    var result = false

    val max = sensorType.parameters.first().constraints["maxValue"]?.toFloat()
    val min = sensorType.parameters.first().constraints["minValue"]?.toFloat()

    if(max != null && min != null){
        result = true
    }

    return result

}