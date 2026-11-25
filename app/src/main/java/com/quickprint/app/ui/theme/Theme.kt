package com.quickprint.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Ocean,
    onPrimary = Color.White,
    secondary = Ink,
    onSecondary = Color.White,
    tertiary = Frost,
    onTertiary = Ink,
    background = Mist,
    onBackground = Ink,
    surface = LightCard,
    onSurface = Ink,
    surfaceVariant = LightMuted,
    onSurfaceVariant = LightMutedText,
    surfaceContainerLowest = LightCard,
    surfaceContainerLow = LightCard,
    surfaceContainer = LightCard,
    surfaceContainerHigh = LightCard,
    surfaceContainerHighest = LightMuted,
    outline = LightInput,
    outlineVariant = LightBorder,
    error = Danger,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = DarkInk,
    onSecondary = Color.White,
    tertiary = Frost,
    onTertiary = Ink,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkCard,
    onSurface = DarkForeground,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkMutedText,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkCard,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkMuted,
    surfaceContainerHighest = DarkMuted,
    outline = DarkInput,
    outlineVariant = DarkBorder,
    error = DangerDark,
    onError = Color.Black,
)

/** Brand colours that Material doesn't have a slot for (the deep "panel" used for headers). */
@Immutable
data class QpExtras(val panel: Color, val onPanel: Color, val accent: Color, val success: Color, val isDark: Boolean = false)

val LocalQpExtras = staticCompositionLocalOf { QpExtras(Ink, Color.White, Frost, Success) }

/** Follows the phone's light/dark setting, like the website does. */
@Composable
fun QuickprintTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    val extras = if (darkTheme) QpExtras(DarkPanel, Color.White, Frost, Color(0xFF4ADE80), isDark = true) else QpExtras(Ink, Color.White, Frost, Success)
    CompositionLocalProvider(LocalQpExtras provides extras) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            shapes = QpShapes,
            content = content,
        )
    }
}
