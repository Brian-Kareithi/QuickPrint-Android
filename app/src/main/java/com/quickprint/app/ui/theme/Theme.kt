package com.quickprint.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
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

@Immutable
data class QpExtras(val panel: Color, val onPanel: Color, val accent: Color, val success: Color)

val LocalQpExtras = staticCompositionLocalOf { QpExtras(Ink, Color.White, Frost, Success) }

@Composable
fun QuickprintTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }
    val extras = QpExtras(Ink, Color.White, Frost, Success)
    CompositionLocalProvider(LocalQpExtras provides extras) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = Typography,
            shapes = QpShapes,
            content = content,
        )
    }
}
