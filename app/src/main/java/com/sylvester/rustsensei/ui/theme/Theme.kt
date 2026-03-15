package com.sylvester.rustsensei.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Rust brand colors
val RustOrange = Color(0xFFCE412B)
val RustOrangeLight = Color(0xFFFF7043)
val RustOrangeDark = Color(0xFF9C1A0A)

// Dark palette — GitHub-dark inspired
val DarkBackground = Color(0xFF0D1117)
val DarkSurface = Color(0xFF161B22)
val DarkSurfaceVariant = Color(0xFF21262D)
val CodeBlockBackground = Color(0xFF161B22)

// Accent colors
val WarmAmber = Color(0xFFF0883E)
val CoolBlue = Color(0xFF58A6FF)
val ErrorRed = Color(0xFFF85149)
val SoftWhite = Color(0xFFE6EDF3)
val MutedGray = Color(0xFF8B949E)
val OutlineDark = Color(0xFF30363D)

// Light palette
val LightBackground = Color(0xFFFAFBFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F2F5)

private val DarkColorScheme = darkColorScheme(
    primary = RustOrange,
    onPrimary = Color.White,
    primaryContainer = RustOrangeDark,
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = WarmAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5A3600),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = CoolBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF0A3069),
    onTertiaryContainer = Color(0xFFB6D7FF),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkBackground,
    onBackground = SoftWhite,
    surface = DarkSurface,
    onSurface = SoftWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = MutedGray,
    outline = OutlineDark,
    outlineVariant = Color(0xFF21262D),
    inverseSurface = SoftWhite,
    inverseOnSurface = DarkBackground,
    surfaceContainerHighest = Color(0xFF2D333B),
    surfaceContainerHigh = Color(0xFF262C36),
    surfaceContainer = DarkSurfaceVariant,
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
    tertiary = Color(0xFF1A7FC4),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1E8FF),
    onTertiaryContainer = Color(0xFF0A3069),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = LightBackground,
    onBackground = Color(0xFF1C1B1F),
    surface = LightSurface,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF57606A),
    outline = Color(0xFFD0D7DE),
    outlineVariant = Color(0xFFE1E4E8),
    inverseSurface = Color(0xFF1C1B1F),
    inverseOnSurface = LightBackground,
    surfaceContainerHighest = Color(0xFFE1E4E8),
    surfaceContainerHigh = Color(0xFFEAECEF),
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerLow = LightSurface,
    surfaceContainerLowest = Color.White
)

@Composable
fun RustSenseiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic colors disabled — app identity should be consistent
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
