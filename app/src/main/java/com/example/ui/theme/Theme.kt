package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

val DarkColorScheme = darkColorScheme(
    primary = HexzieAccent,
    onPrimary = HexzieBackground,
    primaryContainer = HexzieSurfaceElevated,
    onPrimaryContainer = HexzieOnSurface,
    secondary = HexzieAccentElectric,
    onSecondary = HexzieBackground,
    secondaryContainer = HexzieSurfaceVariant,
    onSecondaryContainer = HexzieOnSurface,
    tertiary = HexzieAllowGreen,
    onTertiary = HexzieBackground,
    error = HexzieDenyRed,
    onError = HexzieBackground,
    background = HexzieBackground,
    onBackground = HexzieOnBackground,
    surface = HexzieSurface,
    onSurface = HexzieOnSurface,
    surfaceVariant = HexzieSurfaceVariant,
    onSurfaceVariant = HexzieOnSurfaceMuted,
    outline = HexzieOutline,
    outlineVariant = HexzieSurfaceVariant
)

val LightColorScheme = lightColorScheme(
    primary = HexzieAccent,
    onPrimary = HexzieSurfaceLight,
    primaryContainer = HexzieSurfaceVariantLight,
    onPrimaryContainer = HexzieOnSurfaceLight,
    secondary = HexzieAccentElectric,
    onSecondary = HexzieSurfaceLight,
    secondaryContainer = HexzieSurfaceVariantLight,
    onSecondaryContainer = HexzieOnSurfaceLight,
    tertiary = HexzieAllowGreen,
    onTertiary = HexzieSurfaceLight,
    error = HexzieDenyRed,
    onError = HexzieSurfaceLight,
    background = HexzieBackgroundLight,
    onBackground = HexzieOnBackgroundLight,
    surface = HexzieSurfaceLight,
    onSurface = HexzieOnSurfaceLight,
    surfaceVariant = HexzieSurfaceVariantLight,
    onSurfaceVariant = HexzieOnSurfaceMuted,
    outline = HexzieOutlineLight,
    outlineVariant = HexzieSurfaceVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default dark-first for HexZie
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for HexzieTheme
@Composable
fun HexzieTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
