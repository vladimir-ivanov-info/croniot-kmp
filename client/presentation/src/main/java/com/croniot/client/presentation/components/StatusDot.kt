package com.croniot.client.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun StatusDot(isOnline: Boolean, modifier: Modifier = Modifier) {
    val dotColor by animateColorAsState(
        targetValue = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        animationSpec = tween(500),
        label = "status-dot-color",
    )

    Box(
        modifier = modifier
            .size(20.dp)
            .semantics { contentDescription = if (isOnline) "Online" else "Offline" },
        contentAlignment = Alignment.Center,
    ) {
        if (isOnline) {
            val pulse = rememberInfiniteTransition(label = "status-pulse")
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
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = pulseAlpha }
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
