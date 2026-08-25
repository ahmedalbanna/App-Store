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

private val DarkColorScheme = darkColorScheme(
    primary = OasisPrimaryDark,
    secondary = OasisMintDark,
    tertiary = OasisMintDark,
    background = OasisBgDark,
    surface = OasisSurfaceDark,
    surfaceVariant = OasisSurfaceVariantDark,
    onPrimary = OasisBgDark,
    onSecondary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = OasisPrimary,
    secondary = OasisSecondary,
    tertiary = OasisTertiary,
    background = OasisBackgroundLight,
    surface = OasisSurfaceLight,
    surfaceVariant = OasisSandLight,
    onPrimary = OasisSurfaceLight,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Phase 2.4: enabled opt-in — uses Material You when available, falls back to Oasis palette
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
