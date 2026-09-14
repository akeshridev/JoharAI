package com.akeshridev.johar.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun JoharNagadaLoader(
    modifier: Modifier = Modifier,
    label: String? = "Johar is finding the best answer…",
) {
    val transition = rememberInfiniteTransition(label = "nagada")
    val pulse = transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(420),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nagadaPulse",
    )
    val stickBeat = transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(260),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "stickBeat",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JoharSpacing.Md),
    ) {
        NagadaIcon(
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer {
                    scaleX = pulse.value
                    scaleY = pulse.value
                },
            stickRotation = stickBeat.value,
        )
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun JoharNagadaThinkingBubble(
    modifier: Modifier = Modifier,
    label: String = "Johar is thinking…",
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = JoharElevation.Flat,
    ) {
        Column(Modifier.padding(horizontal = JoharSpacing.Lg, vertical = JoharSpacing.Md)) {
            JoharNagadaLoader(label = null)
            Spacer(Modifier.height(JoharSpacing.Xs))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NagadaIcon(
    modifier: Modifier = Modifier,
    stickRotation: Float,
) {
    val drum = MaterialTheme.colorScheme.secondary
    val rim = MaterialTheme.colorScheme.primary
    val stick = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = rim,
            topLeft = Offset(w * 0.18f, h * 0.36f),
            size = Size(w * 0.64f, h * 0.20f),
            style = Stroke(width = w * 0.055f),
        )
        drawArc(
            color = drum,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.18f, h * 0.42f),
            size = Size(w * 0.64f, h * 0.42f),
        )
        drawLine(
            color = rim,
            start = Offset(w * 0.23f, h * 0.57f),
            end = Offset(w * 0.77f, h * 0.57f),
            strokeWidth = w * 0.045f,
            cap = StrokeCap.Round,
        )

        val delta = stickRotation / 100f * w
        drawLine(
            color = stick,
            start = Offset(w * 0.28f - delta, h * 0.13f),
            end = Offset(w * 0.46f, h * 0.40f),
            strokeWidth = w * 0.05f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = stick,
            start = Offset(w * 0.72f + delta, h * 0.13f),
            end = Offset(w * 0.54f, h * 0.40f),
            strokeWidth = w * 0.05f,
            cap = StrokeCap.Round,
        )
    }
}
