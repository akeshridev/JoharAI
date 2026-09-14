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
import androidx.compose.ui.graphics.Path
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
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(480),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nagadaPulse",
    )
    val stickBeat = transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(320),
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
                .size(40.dp)
                .graphicsLayer {
                    scaleX = pulse.value
                    scaleY = pulse.value
                },
            beat = stickBeat.value,
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
    beat: Float,
) {
    val shell = MaterialTheme.colorScheme.secondary
    val rim = MaterialTheme.colorScheme.primary
    val rope = MaterialTheme.colorScheme.onSurfaceVariant
    val skin = MaterialTheme.colorScheme.surfaceVariant
    val stick = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Deep bowl-shaped shell: this is the strongest visual cue for a nagada.
        val bowl = Path().apply {
            moveTo(w * 0.16f, h * 0.43f)
            cubicTo(w * 0.20f, h * 0.68f, w * 0.34f, h * 0.88f, w * 0.50f, h * 0.92f)
            cubicTo(w * 0.66f, h * 0.88f, w * 0.80f, h * 0.68f, w * 0.84f, h * 0.43f)
            close()
        }
        drawPath(bowl, color = shell)

        // Broad stretched skin and heavy rim.
        drawOval(
            color = skin,
            topLeft = Offset(w * 0.12f, h * 0.28f),
            size = Size(w * 0.76f, h * 0.25f),
        )
        drawOval(
            color = rim,
            topLeft = Offset(w * 0.12f, h * 0.28f),
            size = Size(w * 0.76f, h * 0.25f),
            style = Stroke(width = w * 0.065f),
        )

        // Rope lacing across the body.
        val ropeWidth = w * 0.028f
        val anchors = listOf(0.20f, 0.34f, 0.50f, 0.66f, 0.80f)
        anchors.forEachIndexed { index, x ->
            val lowerX = if (index % 2 == 0) 0.50f else x
            drawLine(
                color = rope,
                start = Offset(w * x, h * 0.46f),
                end = Offset(w * lowerX, h * 0.84f),
                strokeWidth = ropeWidth,
                cap = StrokeCap.Round,
            )
        }
        for (i in 0 until anchors.lastIndex) {
            drawLine(
                color = rope,
                start = Offset(w * anchors[i], h * 0.62f),
                end = Offset(w * anchors[i + 1], h * 0.75f),
                strokeWidth = ropeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = rope,
                start = Offset(w * anchors[i], h * 0.75f),
                end = Offset(w * anchors[i + 1], h * 0.62f),
                strokeWidth = ropeWidth,
                cap = StrokeCap.Round,
            )
        }

        // Two beaters above the drum head; slight alternating movement.
        val swing = beat * w * 0.025f
        drawLine(
            color = stick,
            start = Offset(w * 0.24f - swing, h * 0.10f),
            end = Offset(w * 0.45f, h * 0.33f),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = stick,
            start = Offset(w * 0.76f + swing, h * 0.10f),
            end = Offset(w * 0.55f, h * 0.33f),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round,
        )
    }
}
