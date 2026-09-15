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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
        JoharRanchiSatelliteEmblem(size = 44.dp)
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
            JoharRanchiSatelliteEmblem(size = 58.dp)
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
fun JoharRanchiSatelliteEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    animated: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "ranchiSatellite")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dataPulse",
    )

    val forest = JoharColors.Forest
    val rust = JoharColors.Rust
    val sky = Color(0xFF8ED8D8)
    val hill = Color(0xFF6FA96A)
    val satellite = Color(0xFF2B6178)
    val signal = Color(0xFF24C7B7)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawRoundRect(
            color = Color(0xFFF8F3E7),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.20f, h * 0.20f),
        )

        drawRect(
            color = sky,
            topLeft = Offset(w * 0.06f, h * 0.06f),
            size = Size(w * 0.88f, h * 0.88f),
        )

        val hills = Path().apply {
            moveTo(w * 0.06f, h * 0.64f)
            cubicTo(w * 0.22f, h * 0.50f, w * 0.35f, h * 0.68f, w * 0.48f, h * 0.58f)
            cubicTo(w * 0.62f, h * 0.48f, w * 0.78f, h * 0.62f, w * 0.94f, h * 0.50f)
            lineTo(w * 0.94f, h * 0.94f)
            lineTo(w * 0.06f, h * 0.94f)
            close()
        }
        drawPath(hills, hill)

        drawCircle(
            color = Color(0xFFFFD56B),
            radius = w * 0.16f,
            center = Offset(w * 0.44f, h * 0.55f),
        )

        // Compact Albert Ekka / Ranchi landmark silhouette.
        drawLine(
            color = rust,
            start = Offset(w * 0.43f, h * 0.67f),
            end = Offset(w * 0.43f, h * 0.48f),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = rust,
            radius = w * 0.045f,
            center = Offset(w * 0.43f, h * 0.43f),
        )
        drawLine(
            color = rust,
            start = Offset(w * 0.43f, h * 0.52f),
            end = Offset(w * 0.56f, h * 0.40f),
            strokeWidth = w * 0.035f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = forest,
            start = Offset(w * 0.55f, h * 0.39f),
            end = Offset(w * 0.60f, h * 0.31f),
            strokeWidth = w * 0.018f,
            cap = StrokeCap.Round,
        )
        drawOval(
            color = Color(0xFFD4A85D),
            topLeft = Offset(w * 0.29f, h * 0.68f),
            size = Size(w * 0.30f, h * 0.10f),
        )

        // Satellite in upper-right.
        drawRoundRect(
            color = satellite,
            topLeft = Offset(w * 0.68f, h * 0.18f),
            size = Size(w * 0.12f, h * 0.10f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f),
        )
        drawRect(
            color = Color(0xFF164459),
            topLeft = Offset(w * 0.59f, h * 0.16f),
            size = Size(w * 0.09f, h * 0.14f),
        )
        drawRect(
            color = Color(0xFF164459),
            topLeft = Offset(w * 0.80f, h * 0.16f),
            size = Size(w * 0.09f, h * 0.14f),
        )

        val from = Offset(w * 0.73f, h * 0.28f)
        val to = Offset(w * 0.56f, h * 0.40f)
        drawLine(
            color = signal.copy(alpha = 0.28f),
            start = from,
            end = to,
            strokeWidth = w * 0.018f,
            cap = StrokeCap.Round,
        )

        if (animated) {
            val px = from.x + (to.x - from.x) * pulse
            val py = from.y + (to.y - from.y) * pulse
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = w * 0.030f,
                center = Offset(px, py),
            )
            drawCircle(
                color = signal.copy(alpha = 0.35f),
                radius = w * 0.060f,
                center = Offset(px, py),
                style = Stroke(width = w * 0.010f),
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
        animationSpec = infiniteRepeatable(animation = tween(360), repeatMode = RepeatMode.Reverse),
        label = "alternatingBeat",
    )
    val bounce = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(360), repeatMode = RepeatMode.Reverse),
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

        drawCircle(color = skinTone, radius = w * 0.10f, center = Offset(w * 0.50f, h * 0.16f))
        drawCircle(color = outline, radius = w * 0.102f, center = Offset(w * 0.50f, h * 0.145f), style = Stroke(width = w * 0.025f))
        drawLine(color = skinTone, start = Offset(w * 0.50f, h * 0.24f), end = Offset(w * 0.50f, h * 0.30f), strokeWidth = w * 0.065f, cap = StrokeCap.Round)

        val torso = Path().apply {
            moveTo(w * 0.35f, h * 0.31f)
            lineTo(w * 0.65f, h * 0.31f)
            lineTo(w * 0.70f, h * 0.64f)
            lineTo(w * 0.30f, h * 0.64f)
            close()
        }
        drawPath(torso, color = shirt)
        drawLine(color = cloth, start = Offset(w * 0.38f, h * 0.34f), end = Offset(w * 0.61f, h * 0.61f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)

        val leftHand = Offset(w * 0.30f, h * (0.43f + 0.05f * armBeat))
        val rightHand = Offset(w * 0.70f, h * (0.43f - 0.05f * armBeat))
        drawLine(skinTone, Offset(w * 0.38f, h * 0.36f), leftHand, w * 0.055f, StrokeCap.Round)
        drawLine(skinTone, Offset(w * 0.62f, h * 0.36f), rightHand, w * 0.055f, StrokeCap.Round)

        val leftStickEnd = Offset(w * 0.43f, h * (0.55f + 0.02f * armBeat))
        val rightStickEnd = Offset(w * 0.57f, h * (0.55f - 0.02f * armBeat))
        drawLine(outline, leftHand, leftStickEnd, w * 0.025f, StrokeCap.Round)
        drawLine(outline, rightHand, rightStickEnd, w * 0.025f, StrokeCap.Round)

        val topY = h * 0.58f + drumDrop
        val bowl = Path().apply {
            moveTo(w * 0.18f, topY)
            cubicTo(w * 0.22f, h * 0.78f, w * 0.36f, h * 0.93f, w * 0.50f, h * 0.95f)
            cubicTo(w * 0.64f, h * 0.93f, w * 0.78f, h * 0.78f, w * 0.82f, topY)
            close()
        }
        drawPath(bowl, color = drumShell)
        drawOval(color = drumSkin, topLeft = Offset(w * 0.15f, topY - h * 0.065f), size = Size(w * 0.70f, h * 0.15f))
        drawOval(color = outline, topLeft = Offset(w * 0.15f, topY - h * 0.065f), size = Size(w * 0.70f, h * 0.15f), style = Stroke(width = w * 0.025f))

        val ropeWidth = w * 0.020f
        val anchors = listOf(0.24f, 0.38f, 0.50f, 0.62f, 0.76f)
        anchors.forEachIndexed { index, x ->
            val lowerX = if (index % 2 == 0) 0.50f else x
            drawLine(color = rope, start = Offset(w * x, topY + h * 0.045f), end = Offset(w * lowerX, h * 0.89f), strokeWidth = ropeWidth, cap = StrokeCap.Round)
        }
    }
}
