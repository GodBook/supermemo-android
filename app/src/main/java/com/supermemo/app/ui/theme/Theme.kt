package com.supermemo.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SkyDarkPrimary,
    onPrimary = SkyDarkOnPrimary,
    primaryContainer = SkyDarkPrimaryContainer,
    onPrimaryContainer = SkyDarkOnPrimaryContainer,
    secondary = SkyDarkSecondary,
    onSecondary = SkyDarkOnSecondary,
    secondaryContainer = SkyDarkSecondaryContainer,
    onSecondaryContainer = SkyDarkOnSecondaryContainer,
    tertiary = SkyDarkTertiary,
    onTertiary = SkyDarkOnTertiary,
    tertiaryContainer = SkyDarkTertiaryContainer,
    background = SkyDarkBackground,
    surface = SkyDarkSurface,
    surfaceVariant = SkyDarkSurfaceVariant,
    outline = SkyDarkOutline,
    outlineVariant = SkyDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = SkyPrimary,
    onPrimary = SkyOnPrimary,
    primaryContainer = SkyPrimaryContainer,
    onPrimaryContainer = SkyOnPrimaryContainer,
    secondary = SkySecondary,
    onSecondary = SkyOnSecondary,
    secondaryContainer = SkySecondaryContainer,
    onSecondaryContainer = SkyOnSecondaryContainer,
    tertiary = SkyTertiary,
    onTertiary = SkyOnTertiary,
    tertiaryContainer = SkyTertiaryContainer,
    background = SkyBackground,
    surface = SkySurface,
    surfaceVariant = SkySurfaceVariant,
    outline = SkyOutline,
    outlineVariant = SkyOutlineVariant
)

@Composable
fun SuperMemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    isAmoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && isAmoledBlack) {
                baseScheme.copy(
                    background = AmoledBackground,
                    surface = AmoledSurface,
                    surfaceVariant = AmoledSurfaceVariant
                )
            } else {
                baseScheme
            }
        }
        darkTheme -> {
            if (isAmoledBlack) {
                DarkColorScheme.copy(
                    background = AmoledBackground,
                    surface = AmoledSurface,
                    surfaceVariant = AmoledSurfaceVariant
                )
            } else {
                DarkColorScheme
            }
        }
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
