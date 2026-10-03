package com.example.spire.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Sky600,
    onPrimary = Color.White,
    primaryContainer = Sky100,
    onPrimaryContainer = Sky900,
    secondary = Teal600,
    onSecondary = Color.White,
    secondaryContainer = Teal100,
    onSecondaryContainer = Teal900,
    tertiary = Amber500,
    onTertiary = Color.White,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber800,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Slate200,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Red600,
    errorContainer = Red100,
    onErrorContainer = Red800,
)

private val DarkColorScheme = darkColorScheme(
    primary = Sky400,
    onPrimary = Sky950,
    primaryContainer = Sky800,
    onPrimaryContainer = Sky100,
    secondary = Teal300,
    onSecondary = Teal900,
    secondaryContainer = Teal900,
    onSecondaryContainer = Teal100,
    tertiary = Amber500,
    onTertiary = Slate900,
    tertiaryContainer = Amber900,
    onTertiaryContainer = Amber200,
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate400,
    surfaceContainer = Slate800,
    surfaceContainerHigh = Slate700,
    outline = Slate500,
    outlineVariant = Slate700,
    error = Red400,
    errorContainer = Red900,
    onErrorContainer = Red100,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SPIRETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
