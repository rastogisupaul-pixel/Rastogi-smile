package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SmileLightColorScheme = lightColorScheme(
    primary = SmileYellow,
    onPrimary = SmileInk,
    primaryContainer = SmileYellowSoft,
    onPrimaryContainer = SmileInk,
    secondary = SmileGreen,
    onSecondary = SmileWhite,
    secondaryContainer = SmileGreenSoft,
    onSecondaryContainer = SmileInk,
    tertiary = SmileYellowDark,
    onTertiary = SmileInk,
    background = SmileWhite,
    onBackground = SmileInk,
    surface = SmileWhite,
    onSurface = SmileInk,
    surfaceVariant = SmileYellowTint,
    onSurfaceVariant = SmileSlate,
    outline = SmileDivider,
    error = SmileRed,
    onError = SmileWhite,
    errorContainer = SmileRedSoft,
    onErrorContainer = SmileRed
)

private val SmileDarkColorScheme = darkColorScheme(
    primary = SmileYellow,
    onPrimary = SmileInk,
    primaryContainer = SmileYellowDark,
    onPrimaryContainer = SmileInk,
    secondary = SmileGreen,
    onSecondary = SmileWhite,
    secondaryContainer = SmileDarkSurfaceHigh,
    onSecondaryContainer = SmileDarkInk,
    tertiary = SmileYellowSoft,
    onTertiary = SmileInk,
    background = SmileDarkBg,
    onBackground = SmileDarkInk,
    surface = SmileDarkSurface,
    onSurface = SmileDarkInk,
    surfaceVariant = SmileDarkSurfaceHigh,
    onSurfaceVariant = SmileDarkMuted,
    outline = SmileDarkSurfaceHigh,
    error = SmileRed,
    onError = SmileWhite,
    errorContainer = SmileRedSoft,
    onErrorContainer = SmileRed
)

val SmileShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SmileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SmileDarkColorScheme else SmileLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = SmileShapes,
        content = content
    )
}
