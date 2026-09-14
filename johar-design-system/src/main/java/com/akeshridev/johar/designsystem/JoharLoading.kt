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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun JoharNagadaLoader(
    modifier: Modifier = Modifier,
    label: String? = "Johar is finding the best answer…",
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JoharSpacing.Md),
    ) {
        JoharDrummerMascot(size = 44.dp)
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
            JoharDrummerMascot(size = 54.dp)
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
fun JoharDrummerMascot(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    animated: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "joharDrummer")
    val beat = transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(360),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alternatingBeat",
    )
    val bounce = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(360),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drumBounce",
    )

    val skinTone = MaterialTheme.colorScheme.secondary.copy(alpha = 0.92f)
    val shirt = MaterialTheme.colorScheme.primary
    val cloth = MaterialTheme.colorScheme.surfaceVariant
    val drumShell = JoharColors.Rust
    val drumSkin = JoharColors.Sand
    val rope = JoharColors.Cream
    val outline = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val armBeat = if (animated) beat.value else 0f
        val drumDrop = if (animated) bounce.value * h * 0.008f else 0f

        // Head + neck.
        drawCircle(
            color = skinTone,
            radius = w * 0.10f,
            center = Offset(w * 0.50f, h * 0.16f),
        )
        drawCircle(
            color = outline,
            radius = w * 0.102f,
            center = Offset(w * 0.50f, h * 0.145f),
            style = Stroke(width = w * 0.025f),
        )
        drawLine(
            color = skinTone,
            start = Offset(w * 0.50f, h * 0.24f),
            end = Offset(w * 0.50f, h * 0.30f),
            strokeWidth = w * 0.065f,
            cap = StrokeCap.Round,
        )

        // Torso: simple folk-mascot silhouette, intentionally minimal at loader scale.
        val torso = Path().apply {
            moveTo(w * 0.35f, h * 0.31f)
            lineTo(w * 0.65f, h * 0.31f)
            lineTo(w * 0.70f, h * 0.64f)
            lineTo(w * 0.30f, h * 0.64f)
            close()
        }
        drawPath(torso, color = shirt)

        // Cloth/sash gives the character a stronger local folk silhouette.
        drawLine(
            color = cloth,
            start = Offset(w * 0.38f, h * 0.34f),
            end = Offset(w * 0.61f, h * 0.61f),
            strokeWidth = w * 0.07f,
            cap = StrokeCap.Round,
        )

        // Arms and beaters alternate rather than floating above the drum.
        val leftHand = Offset(w * 0.30f, h * (0.43f + 0.05f * armBeat))
        val rightHand = Offset(w * 0.70f, h * (0.43f - 0.05f * armBeat))
        drawLine(skinTone, Offset(w * 0.38f, h * 0.36f), leftHand, w * 0.055f, StrokeCap.Round)
        drawLine(skinTone, Offset(w * 0.62f, h * 0.36f), rightHand, w * 0.055f, StrokeCap.Round)

        val leftStickEnd = Offset(w * 0.43f, h * (0.55f + 0.02f * armBeat))
        val rightStickEnd = Offset(w * 0.57f, h * (0.55f - 0.02f * armBeat))
        drawLine(outline, leftHand, leftStickEnd, w * 0.025f, StrokeCap.Round)
        drawLine(outline, rightHand, rightStickEnd, w * 0.025f, StrokeCap.Round)

        // Wide traditional bowl-shaped nagada in front of the player.
        val topY = h * 0.58f + drumDrop
        val bowl = Path().apply {
            moveTo(w * 0.18f, topY)
            cubicTo(w * 0.22f, h * 0.78f, w * 0.36f, h * 0.93f, w * 0.50f, h * 0.95f)
            cubicTo(w * 0.64f, h * 0.93f, w * 0.78f, h * 0.78f, w * 0.82f, topY)
            close()
        }
        drawPath(bowl, color = drumShell)
        drawOval(
            color = drumSkin,
            topLeft = Offset(w * 0.15f, topY - h * 0.065f),
            size = Size(w * 0.70f, h * 0.15f),
        )
        drawOval(
            color = outline,
            topLeft = Offset(w * 0.15f, topY - h * 0.065f),
            size = Size(w * 0.70f, h * 0.15f),
            style = Stroke(width = w * 0.025f),
        )

        // Rope lacing: only a few strong diagonals so it remains legible at 40–56dp.
        val ropeWidth = w * 0.020f
        val anchors = listOf(0.24f, 0.38f, 0.50f, 0.62f, 0.76f)
        anchors.forEachIndexed { index, x ->
            val lowerX = if (index % 2 == 0) 0.50f else x
            drawLine(
                color = rope,
                start = Offset(w * x, topY + h * 0.045f),
                end = Offset(w * lowerX, h * 0.89f),
                strokeWidth = ropeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}
