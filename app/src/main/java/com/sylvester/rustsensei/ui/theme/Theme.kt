package com.sylvester.rustsensei.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Neon Terminal palette ────────────────────────────────────────────

// Rust brand / neon accent
val RustOrange = Color(0xFFCE412B)
val NeonOrangeBright = Color(0xFFFF6B35)
val RustOrangeDark = Color(0xFF9C1A0A)

// Dark surfaces — near-black with blue tint
val DarkBackground = Color(0xFF06080C)
val DarkSurface = Color(0xFF0C1018)
val DarkSurfaceVariant = Color(0xFF141820)
val DarkSurfaceContainer = Color(0xFF1A1F2A)
val CodeBlockBackground = Color(0xFF06080C)

// Accent & semantic colors
val NeonCyan = Color(0xFF4DEEEA)
val ErrorNeon = Color(0xFFFF453A)
val CrispWhite = Color(0xFFE8ECF2)
val MediumGray = Color(0xFF6B7280)
val OutlineDark = Color(0xFF1E2430)

// Light palette
val LightBackground = Color(0xFFF8F9FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDEFF3)

private val DarkColorScheme = darkColorScheme(
    primary = RustOrange,
    onPrimary = Color.White,
    primaryContainer = RustOrangeDark,
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = NeonOrangeBright,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5A2800),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = NeonCyan,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF003D3C),
    onTertiaryContainer = Color(0xFFB2FFFD),
    error = ErrorNeon,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkBackground,
    onBackground = CrispWhite,
    surface = DarkSurface,
    onSurface = CrispWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = MediumGray,
    outline = OutlineDark,
    outlineVariant = Color(0xFF141820),
    inverseSurface = CrispWhite,
    inverseOnSurface = DarkBackground,
    surfaceContainerHighest = Color(0xFF252A36),
    surfaceContainerHigh = Color(0xFF1F2533),
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerLow = DarkSurface,
    surfaceContainerLowest = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = RustOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD1),
    onPrimaryContainer = RustOrangeDark,
    secondary = Color(0xFFD97B2A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB3),
    onSecondaryContainer = Color(0xFF5A3600),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2DFDB),
    onTertiaryContainer = Color(0xFF003D3C),
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

@Composable
fun RustSenseiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RustSenseiTypography,
        content = content
    )
}
