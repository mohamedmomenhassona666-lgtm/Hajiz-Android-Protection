package com.hajiz.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HajizLight = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F1E7),
    onPrimaryContainer = Color(0xFF003731),
    secondary = Color(0xFFC27A36),
    secondaryContainer = Color(0xFFFFDDBB),
    onSecondaryContainer = Color(0xFF2C1600),
    background = Color(0xFFF6F8F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE1EAE7),
    onSurface = Color(0xFF17201E),
    onSurfaceVariant = Color(0xFF48534F),
    error = Color(0xFFBA1A1A),
)

private val HajizDark = darkColorScheme(
    primary = Color(0xFF69DCCF),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFFB8F1E7),
    secondary = Color(0xFFF0B876),
    background = Color(0xFF0D1513),
    surface = Color(0xFF141D1B),
    surfaceVariant = Color(0xFF3B4945),
)

@Composable
fun HajizTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) HajizDark else HajizLight,
        content = content,
    )
}