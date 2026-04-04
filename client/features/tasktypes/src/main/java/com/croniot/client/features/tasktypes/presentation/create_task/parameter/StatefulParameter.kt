package com.croniot.client.features.tasktypes.presentation.create_task.parameter

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.domain.models.ParameterTask
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.isRepresentsSlider
import com.croniot.client.domain.models.isRepresentsSwitch
import com.croniot.client.presentation.constants.UtilUi
import kotlinx.coroutines.flow.StateFlow

enum class SyncStatus { Synced, Pending, Desynced }

@Composable
fun StatefulParameter(
    parameter: ParameterTask,
    latestTaskStateInfoFlow: StateFlow<TaskStateInfo?>,
    isSyncedFlow: StateFlow<Boolean>,
    onStateChanged: (newState: String) -> Unit,
) {
    val isSlider = remember { parameter.isRepresentsSlider() }
    val isSwitch = remember { parameter.isRepresentsSwitch() }

    val latestState: TaskStateInfo? by latestTaskStateInfoFlow.collectAsStateWithLifecycle()
    val taskStateInfoSynced = isSyncedFlow.collectAsStateWithLifecycle()

    if (isSwitch) {
        SwitchTaskTypeParameter(
            taskStateInfoSynced = taskStateInfoSynced,
            parameter = parameter,
            latestState = latestState,
            onStateChanged = onStateChanged,
        )
    } else if (isSlider) {
        StatefulParameterSlider(
            taskStateInfoSynced = taskStateInfoSynced,
            parameter = parameter,
            latestState = latestState,
            onStateChanged = onStateChanged,
        )
    } else {
        Text(
            text = latestState?.state ?: "-",
            fontSize = UtilUi.TEXT_SIZE_3,
        )
    }
}

@Composable
fun SwitchTaskTypeParameter(
    taskStateInfoSynced: State<Boolean>,
    parameter: ParameterTask,
    latestState: TaskStateInfo?,
    onStateChanged: (newState: String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SyncDot(
            isSynced = taskStateInfoSynced.value,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Text(
            modifier = Modifier.weight(1f),
            text = parameter.name,
            fontSize = UtilUi.TEXT_SIZE_3,
        )

        val state1 = parameter.constraints["state_1"]
        val state2 = parameter.constraints["state_2"]

        if (latestState != null && (latestState.state == state1 || latestState.state == state2)) {
            val checked = latestState.state == state1
            Switch(
                modifier = Modifier.padding(horizontal = 8.dp),
                checked = checked,
                onCheckedChange = { newChecked ->
                    val newValue = if (newChecked) parameter.constraints["state_1"] else parameter.constraints["state_2"]
                    if (newValue != null) {
                        onStateChanged(newValue)
                    }
                },
            )
        }
    }
}

@Composable
fun SyncDot(isSynced: Boolean, modifier: Modifier = Modifier) {
    val dotColor by animateColorAsState(
        targetValue = if (isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        animationSpec = tween(500),
        label = "sync-dot-color",
    )

    val pulse = rememberInfiniteTransition(label = "sync-pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse-scale",
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse-alpha",
    )

    Box(
        modifier = modifier.size(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isSynced) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        scaleX = pulseScale;
                        scaleY = pulseScale;
                        alpha = pulseAlpha
                    }
                    .background(color = dotColor, shape = CircleShape),
            )
        }

        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = dotColor, shape = CircleShape),
        )
    }
}