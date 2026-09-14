package com.akeshridev.johar.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun JoharSplashScreen(
    modifier: Modifier = Modifier,
    title: String = "Johar",
    subtitle: String = "Ranchi AI",
    tagline: String = "Ask anything about Ranchi",
    compactPreview: Boolean = false,
) {
    val height = if (compactPreview) 500.dp else 640.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = if (compactPreview) RoundedCornerShape(JoharRadius.ExtraLarge) else RoundedCornerShape(0.dp),
        color = JoharColors.Cream,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            JoharColors.Cream,
                            Color(0xFFF4E8CF),
                            Color(0xFFDDE7C8),
                        ),
                    ),
                ),
        ) {
            SplashLandscape(modifier = Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (compactPreview) 34.dp else 64.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    color = JoharColors.Forest,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = JoharColors.Rust,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(JoharSpacing.Xs))
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JoharColors.Forest.copy(alpha = 0.74f),
                    textAlign = TextAlign.Center,
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = if (compactPreview) 72.dp else 100.dp),
                shape = RoundedCornerShape(JoharRadius.ExtraLarge),
                color = JoharColors.Cream.copy(alpha = 0.90f),
                shadowElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    JoharDrummerMascot(size = if (compactPreview) 122.dp else 150.dp)
                    Spacer(Modifier.height(JoharSpacing.Md))
                    Text(
                        text = "Johar!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = JoharColors.Forest,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Your local Ranchi companion",
                        style = MaterialTheme.typography.bodySmall,
                        color = JoharColors.Muted,
                    )
                }
            }

            Text(
                text = "LOCAL • GROUNDED • OFFLINE-FIRST",
                style = MaterialTheme.typography.labelSmall,
                color = JoharColors.Forest.copy(alpha = 0.66f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun SplashLandscape(modifier: Modifier = Modifier) {
    val forest = JoharColors.Forest
    val leaf = Color(0xFF7EA56C)
    val lightLeaf = Color(0xFFA8C58B)
    val rust = JoharColors.Rust

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val farHill = Path().apply {
            moveTo(0f, h * 0.50f)
            cubicTo(w * 0.20f, h * 0.40f, w * 0.32f, h * 0.54f, w * 0.50f, h * 0.45f)
            cubicTo(w * 0.68f, h * 0.37f, w * 0.82f, h * 0.49f, w, h * 0.39f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(farHill, color = lightLeaf.copy(alpha = 0.58f))

        val nearHill = Path().apply {
            moveTo(0f, h * 0.63f)
            cubicTo(w * 0.18f, h * 0.54f, w * 0.33f, h * 0.68f, w * 0.54f, h * 0.57f)
            cubicTo(w * 0.73f, h * 0.48f, w * 0.88f, h * 0.63f, w, h * 0.55f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(nearHill, color = leaf.copy(alpha = 0.72f))

        // Simple field rhythm to evoke Jharkhand's green landscape without becoming decorative noise.
        val rowYs = listOf(0.72f, 0.78f, 0.84f, 0.90f)
        rowYs.forEachIndexed { row, yFactor ->
            for (i in 0..11) {
                val x = (i + if (row % 2 == 0) 0.2f else 0.7f) / 12f * w
                val y = h * yFactor
                drawLine(
                    color = forest.copy(alpha = 0.20f),
                    start = Offset(x, y + h * 0.025f),
                    end = Offset(x - w * 0.012f, y),
                    strokeWidth = 2f,
                )
                drawLine(
                    color = forest.copy(alpha = 0.20f),
                    start = Offset(x, y + h * 0.025f),
                    end = Offset(x + w * 0.012f, y),
                    strokeWidth = 2f,
                )
            }
        }

        // Two understated festival flags anchor the folk-art reference.
        listOf(0.16f, 0.83f).forEachIndexed { index, xFactor ->
            val poleX = w * xFactor
            val poleTop = h * (if (index == 0) 0.28f else 0.24f)
            drawLine(
                color = forest.copy(alpha = 0.48f),
                start = Offset(poleX, poleTop),
                end = Offset(poleX, h * 0.58f),
                strokeWidth = 2f,
            )
            val flag = Path().apply {
                moveTo(poleX, poleTop)
                lineTo(poleX + w * 0.07f, poleTop + h * 0.018f)
                lineTo(poleX, poleTop + h * 0.065f)
                close()
            }
            drawPath(flag, color = if (index == 0) Color.White else rust.copy(alpha = 0.90f))
        }

        drawOval(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(w * 0.10f, h * 0.12f),
            size = Size(w * 0.24f, h * 0.07f),
        )
    }
}
