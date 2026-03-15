package com.sylvester.rustsensei.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Rust brand colors
val RustOrange = Color(0xFFCE412B)
val RustOrangeLight = Color(0xFFFF7043)
val RustOrangeDark = Color(0xFF9C1A0A)

val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2D2D2D)
val CodeBlockBackground = Color(0xFF1E1E1E)

private val DarkColorScheme = darkColorScheme(
    primary = RustOrange,
    onPrimary = Color.White,
    primaryContainer = RustOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = RustOrangeLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFBBBBBB)
)

private val LightColorScheme = lightColorScheme(
    primary = RustOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD1),
    onPrimaryContainer = RustOrangeDark,
    secondary = RustOrangeLight,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun RustSenseiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context).copy(
                primary = RustOrange,
                onPrimary = Color.White
            ) else dynamicLightColorScheme(context).copy(
                primary = RustOrange,
                onPrimary = Color.White
            )
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
