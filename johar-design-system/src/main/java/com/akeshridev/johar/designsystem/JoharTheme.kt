package com.akeshridev.johar.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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

private val JoharDarkColors = darkColorScheme(
    primary = Color(0xFFFFB59A),
    onPrimary = JoharColors.Forest,
    secondary = Color(0xFFE8D5B5),
    onSecondary = JoharColors.Forest,
    background = JoharColors.Forest,
    onBackground = JoharColors.Cream,
    surface = JoharColors.DarkSurface,
    onSurface = JoharColors.Cream,
    surfaceVariant = Color(0xFF223E31),
    onSurfaceVariant = JoharColors.Cream,
    outline = Color(0xFF2D4A3B),
)

private val JoharTypography = Typography()

@Composable
fun JoharTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) JoharDarkColors else JoharLightColors,
        typography = JoharTypography,
        content = content,
    )
}
