package com.ibrahim.geoalarm.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GeoAlarmColorScheme = darkColorScheme(
    primary = Cyan60,
    onPrimary = DarkSurface,
    primaryContainer = CyanDark,
    onPrimaryContainer = Cyan80,

    secondary = Blue60,
    onSecondary = DarkSurface,
    secondaryContainer = Blue40,
    onSecondaryContainer = Blue80,

    tertiary = GreenBright,
    onTertiary = DarkSurface,
    tertiaryContainer = Green60,
    onTertiaryContainer = Green80,

    error = Red60,
    onError = DarkSurface,
    errorContainer = RedBright,
    onErrorContainer = Red80,

    background = DarkSurface,
    onBackground = TextPrimary,

    surface = DarkSurfaceVariant,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,

    outline = DarkCardBorder,
    outlineVariant = TextMuted
)

@Composable
fun GeoAlarmTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = GeoAlarmColorScheme,
        typography = Typography,
        content = content
    )
}