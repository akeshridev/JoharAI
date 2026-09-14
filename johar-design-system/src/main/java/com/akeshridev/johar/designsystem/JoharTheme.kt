package com.akeshridev.johar.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JoharLightColors = lightColorScheme(
    primary = JoharColors.Forest,
    onPrimary = JoharColors.Cream,
    secondary = JoharColors.Rust,
    onSecondary = Color.White,
    background = JoharColors.Cream,
    onBackground = JoharColors.Forest,
    surface = JoharColors.White,
    onSurface = JoharColors.Forest,
    surfaceVariant = JoharColors.Sand,
    onSurfaceVariant = JoharColors.Forest,
    outline = JoharColors.Sand,
)

private val JoharTypography = Typography()

@Composable
fun JoharTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JoharLightColors,
        typography = JoharTypography,
        content = content,
    )
}
