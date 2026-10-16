package com.smartjirani.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme =
    lightColorScheme(
        primary = UjamaaEmeraldDeep,
        onPrimary = UjamaaOnPrimary,
        primaryContainer = UjamaaEmerald,
        onPrimaryContainer = UjamaaEmeraldMist,
        secondary = UjamaaTerracottaDeep,
        onSecondary = UjamaaOnPrimary,
        secondaryContainer = UjamaaTerracottaContainer,
        onSecondaryContainer = UjamaaTerracottaOnContainer,
        tertiary = UjamaaAmberDeep,
        onTertiary = UjamaaOnPrimary,
        tertiaryContainer = UjamaaAmberContainer,
        onTertiaryContainer = UjamaaAmberOnContainer,
        background = UjamaaCanvas,
        onBackground = UjamaaOnSurface,
        surface = UjamaaCanvas,
        onSurface = UjamaaOnSurface,
        surfaceVariant = UjamaaSurfaceHighest,
        onSurfaceVariant = UjamaaInkVariant,
        surfaceContainerLowest = UjamaaSurfaceLowest,
        surfaceContainerLow = UjamaaSurfaceLow,
        surfaceContainer = UjamaaSurfaceMid,
        surfaceContainerHigh = UjamaaSurfaceHigh,
        surfaceContainerHighest = UjamaaSurfaceHighest,
        surfaceDim = UjamaaSurfaceDim,
        surfaceBright = UjamaaSurfaceBright,
        outline = UjamaaOutline,
        outlineVariant = UjamaaOutlineVariant,
        inverseSurface = UjamaaInverseSurface,
        inverseOnSurface = UjamaaInverseOnSurface,
        inversePrimary = UjamaaInversePrimary,
        error = UjamaaError,
        onError = UjamaaOnError,
        errorContainer = UjamaaErrorContainer,
        onErrorContainer = UjamaaOnErrorContainer,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = UjamaaEmeraldMist,
        onPrimary = UjamaaEmeraldDeep,
        primaryContainer = UjamaaEmerald,
        onPrimaryContainer = UjamaaEmeraldMist,
        secondary = UjamaaTerracottaContainer,
        onSecondary = UjamaaTerracottaOnContainer,
        secondaryContainer = UjamaaTerracottaDeep,
        onSecondaryContainer = UjamaaTerracottaMist,
        tertiary = UjamaaAmberOnContainer,
        onTertiary = UjamaaAmberDeep,
        tertiaryContainer = UjamaaAmberContainer,
        onTertiaryContainer = UjamaaAmberOnContainer,
        background = UjamaaDarkCanvas,
        onBackground = UjamaaDarkOnSurface,
        surface = UjamaaDarkSurface,
        onSurface = UjamaaDarkOnSurface,
        surfaceVariant = UjamaaDarkSurfaceHigh,
        onSurfaceVariant = UjamaaDarkOnSurfaceVariant,
        surfaceContainerLowest = UjamaaDarkCanvas,
        surfaceContainerLow = UjamaaDarkSurface,
        surfaceContainer = UjamaaDarkSurfaceMid,
        surfaceContainerHigh = UjamaaDarkSurfaceHigh,
        surfaceContainerHighest = UjamaaDarkSurfaceHighest,
        surfaceDim = UjamaaDarkCanvas,
        surfaceBright = UjamaaDarkSurfaceMid,
        outline = UjamaaDarkOnSurfaceVariant,
        outlineVariant = UjamaaDarkSurfaceHigh,
        inverseSurface = UjamaaInverseOnSurface,
        inverseOnSurface = UjamaaInverseSurface,
        inversePrimary = UjamaaEmeraldDeep,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = UjamaaErrorContainer,
        onErrorContainer = UjamaaOnErrorContainer,
    )

@Composable
fun SmartJiraniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmartTypography,
        shapes = SmartShapes,
        content = content,
    )
}
