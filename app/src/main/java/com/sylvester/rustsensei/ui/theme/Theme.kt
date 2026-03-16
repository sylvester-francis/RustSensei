package com.sylvester.rustsensei.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = RustOrange,
    onPrimary = Color.White,
    primaryContainer = RustOrangeDark,
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003D3C),
    onSecondaryContainer = Color(0xFFB2FFFD),
    tertiary = WarningAmber,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF5A2800),
    onTertiaryContainer = Color(0xFFFFDDB3),
    error = ErrorNeon,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkBackground,
    onBackground = CrispWhite,
    surface = DarkSurface,
    onSurface = CrispWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SecondaryText,
    outline = OutlineDark,
    outlineVariant = Color(0xFF151A23),
    inverseSurface = CrispWhite,
    inverseOnSurface = DarkBackground,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerLow = DarkSurface,
    surfaceContainerLowest = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = RustOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD1),
    onPrimaryContainer = RustOrangeDark,
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF003D3C),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF5A3600),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = LightBackground,
    onBackground = Color(0xFF12141A),
    surface = LightSurface,
    onSurface = Color(0xFF12141A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF4A505C),
    outline = Color(0xFFCDD2DA),
    outlineVariant = Color(0xFFDFE2E8),
    inverseSurface = Color(0xFF12141A),
    inverseOnSurface = LightBackground,
    surfaceContainerHighest = Color(0xFFDDE0E6),
    surfaceContainerHigh = Color(0xFFE6E9EF),
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerLow = LightSurface,
    surfaceContainerLowest = Color.White
)

// M3 Shape system — consistent corner radii across all components
val RustSenseiShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RustSenseiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Keep disabled — Rust Orange identity is too important
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RustSenseiTypography,
        shapes = RustSenseiShapes,
        content = content
    )
}
