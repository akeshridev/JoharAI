package com.akeshridev.johar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val JoharCream = Color(0xFFFFFBF0)
val JoharForest = Color(0xFF122A1E)
val JoharRust = Color(0xFF8B2500)
val JoharSand = Color(0xFFE8D5B5)
val JoharGreen = Color(0xFF2E7D32)
val JoharMuted = Color(0xFF665F54)
val JoharSurface = Color(0xFFFFFFFF)
val JoharSoftGreen = Color(0xFFE8F5E9)
val JoharSoftOrange = Color(0xFFFFF0E5)

private val JoharLightColors = lightColorScheme(
    primary = JoharForest,
    onPrimary = JoharCream,
    secondary = JoharRust,
    onSecondary = Color.White,
    background = JoharCream,
    onBackground = JoharForest,
    surface = JoharSurface,
    onSurface = JoharForest,
    surfaceVariant = JoharSand,
    onSurfaceVariant = JoharForest,
    outline = JoharSand,
)

@Composable
fun JoharTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JoharLightColors,
        content = content,
    )
}
