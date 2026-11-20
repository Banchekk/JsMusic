package com.example.personalmusicplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun PersonalMusicTheme(appTheme: AppTheme, content: @Composable () -> Unit) {
    val colors = if (appTheme.isDark) {
        darkColorScheme(
            primary = appTheme.primary,
            onPrimary = appTheme.onPrimary,
            secondary = appTheme.accent,
            onSecondary = appTheme.text,
            primaryContainer = appTheme.card,
            onPrimaryContainer = appTheme.text,
            background = appTheme.background,
            onBackground = appTheme.text,
            surface = appTheme.background,
            onSurface = appTheme.text,
            surfaceVariant = appTheme.card,
            onSurfaceVariant = appTheme.textMuted,
            surfaceContainer = appTheme.card,
            surfaceContainerLow = appTheme.card,
            surfaceContainerHigh = appTheme.card,
            surfaceContainerHighest = appTheme.card,
            secondaryContainer = appTheme.card,
            onSecondaryContainer = appTheme.text,
        )
    } else {
        lightColorScheme(
            primary = appTheme.primary,
            onPrimary = appTheme.onPrimary,
            secondary = appTheme.accent,
            onSecondary = appTheme.text,
            primaryContainer = appTheme.card,
            onPrimaryContainer = appTheme.text,
            background = appTheme.background,
            onBackground = appTheme.text,
            surface = appTheme.background,
            onSurface = appTheme.text,
            surfaceVariant = appTheme.card,
            onSurfaceVariant = appTheme.textMuted,
            surfaceContainer = appTheme.card,
            surfaceContainerLow = appTheme.card,
            surfaceContainerHigh = appTheme.card,
            surfaceContainerHighest = appTheme.card,
            secondaryContainer = appTheme.card,
            onSecondaryContainer = appTheme.text,
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
