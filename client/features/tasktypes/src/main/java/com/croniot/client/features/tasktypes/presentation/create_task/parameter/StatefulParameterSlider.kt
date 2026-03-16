package com.croniot.client.features.tasktypes.presentation.create_task.parameter

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.croniot.client.core.models.ParameterTask
import com.croniot.client.core.models.TaskStateInfo
import kotlin.math.roundToInt

@Composable
fun StatefulParameterSlider(
    taskStateInfoSynced: State<Boolean>,
    parameter: ParameterTask,
    latestState: TaskStateInfo?,
    onStateChanged: (newState: String) -> Unit,
) {
    // --- Constraints unificados ---
    val minValue = parameter.constraints["minValue"]?.toFloatOrNull() ?: 0f // TODO don't give default values, just show error
    val maxValue = parameter.constraints["maxValue"]?.toFloatOrNull() ?: 100f // TODO don't give default values, just show error
    val step = (parameter.constraints["stepSize"]?.toFloatOrNull() ?: 1f)
        .coerceAtLeast(0.0001f)

    val lo = minValue.coerceAtMost(maxValue)
    val hi = maxValue.coerceAtLeast(minValue)

    fun snap(v: Float): Float {
        val clipped = v.coerceIn(lo, hi)
        val k = ((clipped - lo) / step).roundToInt()
        return (lo + k * step).coerceIn(lo, hi)
    }

    val rawSteps = (((maxValue - minValue) / step).roundToInt()).coerceAtLeast(0)

    val safeSteps = if (rawSteps > 15) 0 else rawSteps

    // Comparación en enteros (evita falsos desync por redondeos)
    fun toDomainInt(v: Float) = v.roundToInt()

    // Parse numérico del server
    fun parseNumeric(s: String?): Float? =
        s?.trim()
            ?.takeUnless { it == "CREATED" || it == "RECEIVED" || it == "off" || it == "on" }
            ?.toFloatOrNull()

    val serverFloat: Float? = remember(latestState?.state) {
        parseNumeric(latestState?.state)?.coerceIn(lo, hi)
    }
    val serverInt: Int? = serverFloat?.let(::toDomainInt)

    var sliderValue by rememberSaveable(parameter.uid) { mutableStateOf(serverFloat ?: lo) }
    var isDragging by rememberSaveable(parameter.uid) { mutableStateOf(false) }
    var awaitingAck by rememberSaveable(parameter.uid) { mutableStateOf(false) }
    var lastSentInt by rememberSaveable(parameter.uid) { mutableStateOf<Int?>(null) }
    var lastUserTouchTs by rememberSaveable(parameter.uid) { mutableStateOf(0L) }
    var desyncCandidateSince by rememberSaveable(parameter.uid) { mutableStateOf<Long?>(null) }

    val epsilonInt = 0 // comparamos ints exactos
    val userQuietMs = 800L
    val desyncGraceMs = 1200L

    // ACK: si server == lastSent -> liberar lock y sincronizar UI
    LaunchedEffect(serverInt) {
        val si = serverInt
        if (awaitingAck && lastSentInt != null && si != null && si == lastSentInt) {
            awaitingAck = false
            sliderValue = snap(si.toFloat())
            desyncCandidateSince = null
        }
    }

    // Adoptar server solo en reposo y tras ventana de usuario
    LaunchedEffect(serverInt) {
        val si = serverInt ?: return@LaunchedEffect
        if (!isDragging && !awaitingAck) {
            val now = System.currentTimeMillis()
            if (now - lastUserTouchTs >= userQuietMs) {
                val currentInt = toDomainInt(sliderValue)
                if (currentInt != si) sliderValue = snap(si.toFloat())
            }
        }
    }

    // Estado visual con ventana de gracia
    val now = System.currentTimeMillis()
    val currentInt = toDomainInt(sliderValue)
    val rawDiff = serverInt?.let { it != currentInt } ?: false

    if (rawDiff && !isDragging && !awaitingAck) {
        if (desyncCandidateSince == null) desyncCandidateSince = now
    } else {
        desyncCandidateSince = null
    }
    val sustainedDesync = desyncCandidateSince?.let { now - it >= desyncGraceMs } ?: false

    val status: SyncStatus = when {
        isDragging || awaitingAck -> SyncStatus.Pending
        !taskStateInfoSynced.value -> SyncStatus.Desynced
        sustainedDesync -> SyncStatus.Desynced
        else -> SyncStatus.Synced
    }

    // Valor mostrado: % si rango 0..100, si no entero
    val valueText = if (lo == 0f && hi == 100f) {
        "$currentInt"
    } else {
        currentInt.toString()
    }

    // ---- UI en Card ----
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        // .padding(horizontal = 16.dp, vertical = 8.dp)
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SyncIndicator(status = status)
            Spacer(Modifier.width(10.dp))
            Text(
                text = parameter.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ValuePill(text = valueText)
        }

        Slider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp),
            value = sliderValue,
            onValueChange = { raw ->
                isDragging = true
                lastUserTouchTs = System.currentTimeMillis()
                sliderValue = snap(raw)
            },
            onValueChangeFinished = {
                isDragging = false
                lastUserTouchTs = System.currentTimeMillis()
                val sendInt = toDomainInt(sliderValue)
                val shouldSend = !awaitingAck && (lastSentInt == null || lastSentInt != sendInt)
                if (shouldSend) {
                    awaitingAck = true
                    lastSentInt = sendInt
                    onStateChanged(sendInt.toString())
                }
            },
            valueRange = lo..hi,
            steps = safeSteps,
            enabled = true,
            colors = SliderDefaults.colors(
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@Composable
fun SyncIndicator(status: SyncStatus, modifier: Modifier = Modifier) {
    val (icon, tint, desc) = when (status) {
        SyncStatus.Synced ->
            Triple(Icons.Filled.CheckCircle, MaterialTheme.colorScheme.primary, "Valor sincronizado")
        SyncStatus.Pending ->
            Triple(Icons.Filled.Cached, MaterialTheme.colorScheme.tertiary, "Sincronizando…")
        SyncStatus.Desynced ->
            Triple(Icons.Filled.ErrorOutline, MaterialTheme.colorScheme.error, "No sincronizado")
    }

    val alphaAnim = if (status == SyncStatus.Pending) {
        val t = rememberInfiniteTransition(label = "pulse")
        t.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "alpha",
        ).value
    } else {
        1f
    }

    Icon(
        imageVector = icon,
        tint = tint,
        contentDescription = desc,
        modifier = modifier.size(20.dp).alpha(alphaAnim),
    )
}

@Composable
private fun ValuePill(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}
