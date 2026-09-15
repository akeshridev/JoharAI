package com.akeshridev.johar.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    title: String = "Johar AI",
    subtitle: String = "Jharkhand in Your Hands",
    tagline: String = "Local knowledge • Offline-first • Built for Jharkhand",
    compactPreview: Boolean = false,
) {
    val outerModifier = if (compactPreview) {
        modifier
            .fillMaxWidth()
            .height(560.dp)
    } else {
        modifier.fillMaxSize()
    }

    Surface(
        modifier = outerModifier,
        shape = if (compactPreview) RoundedCornerShape(JoharRadius.ExtraLarge) else RoundedCornerShape(0.dp),
        color = JoharColors.Cream,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFFFFFBF0),
                        0.32f to Color(0xFFF4F3E5),
                        0.64f to Color(0xFFDDE9D1),
                        1f to Color(0xFF9FC6A4),
                    ),
                ),
        ) {
            SplashLandscape(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top = if (compactPreview) 38.dp else 70.dp,
                        start = 28.dp,
                        end = 28.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = if (compactPreview) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayLarge,
                    color = JoharColors.Forest,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = JoharColors.Forest.copy(alpha = 0.82f),
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodyLarge,
                    color = JoharColors.Forest.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = if (compactPreview) 82.dp else 108.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f + size.height * 0.06f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.42f),
                        radius = size.minDimension * 0.30f,
                        center = Offset(cx, cy),
                    )
                    drawCircle(
                        color = JoharColors.Forest.copy(alpha = 0.05f),
                        radius = size.minDimension * 0.36f,
                        center = Offset(cx, cy),
                    )
                }

                JoharRanchiSatelliteEmblem(
                    size = if (compactPreview) 190.dp else 240.dp,
                    animated = true,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 28.dp,
                        end = 28.dp,
                        bottom = if (compactPreview) 24.dp else 42.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Ranchi ↔ Satellite • local intelligence in motion",
                    style = MaterialTheme.typography.titleSmall,
                    color = JoharColors.Forest,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Built for people of Jharkhand",
                    style = MaterialTheme.typography.bodySmall,
                    color = JoharColors.Forest.copy(alpha = 0.66f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SplashLandscape(modifier: Modifier = Modifier) {
    val forest = JoharColors.Forest
    val leaf = Color(0xFF7EA56C)
    val lightLeaf = Color(0xFFA8C58B)
    val deepLeaf = Color(0xFF557B48)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val farHill = Path().apply {
            moveTo(0f, h * 0.58f)
            cubicTo(w * 0.18f, h * 0.49f, w * 0.34f, h * 0.60f, w * 0.52f, h * 0.51f)
            cubicTo(w * 0.72f, h * 0.42f, w * 0.86f, h * 0.55f, w, h * 0.47f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(farHill, color = lightLeaf.copy(alpha = 0.56f))

        val middleHill = Path().apply {
            moveTo(0f, h * 0.69f)
            cubicTo(w * 0.17f, h * 0.58f, w * 0.36f, h * 0.72f, w * 0.57f, h * 0.61f)
            cubicTo(w * 0.76f, h * 0.51f, w * 0.90f, h * 0.66f, w, h * 0.59f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(middleHill, color = leaf.copy(alpha = 0.64f))

        val nearHill = Path().apply {
            moveTo(0f, h * 0.79f)
            cubicTo(w * 0.22f, h * 0.71f, w * 0.38f, h * 0.84f, w * 0.60f, h * 0.75f)
            cubicTo(w * 0.76f, h * 0.68f, w * 0.91f, h * 0.77f, w, h * 0.72f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(nearHill, color = deepLeaf.copy(alpha = 0.32f))

        listOf(0.14f, 0.86f).forEachIndexed { index, xFactor ->
            val poleX = w * xFactor
            val poleTop = h * (if (index == 0) 0.28f else 0.25f)
            drawLine(
                color = forest.copy(alpha = 0.34f),
                start = Offset(poleX, poleTop),
                end = Offset(poleX, h * 0.59f),
                strokeWidth = 2f,
            )
            val flag = Path().apply {
                moveTo(poleX, poleTop)
                lineTo(poleX + w * 0.065f, poleTop + h * 0.015f)
                lineTo(poleX, poleTop + h * 0.052f)
                close()
            }
            drawPath(
                flag,
                color = if (index == 0) Color.White.copy(alpha = 0.94f) else JoharColors.Rust.copy(alpha = 0.78f),
            )
        }

        val rowYs = listOf(0.84f, 0.89f, 0.94f)
        rowYs.forEachIndexed { row, yFactor ->
            for (i in 0..10) {
                val x = (i + if (row % 2 == 0) 0.3f else 0.8f) / 11f * w
                val y = h * yFactor
                drawLine(
                    color = forest.copy(alpha = 0.15f),
                    start = Offset(x, y + h * 0.016f),
                    end = Offset(x - w * 0.01f, y),
                    strokeWidth = 2f,
                )
                drawLine(
                    color = forest.copy(alpha = 0.15f),
                    start = Offset(x, y + h * 0.016f),
                    end = Offset(x + w * 0.01f, y),
                    strokeWidth = 2f,
                )
            }
        }

        drawOval(
            color = Color.White.copy(alpha = 0.14f),
            topLeft = Offset(w * 0.08f, h * 0.13f),
            size = Size(w * 0.28f, h * 0.06f),
        )
    }
}
