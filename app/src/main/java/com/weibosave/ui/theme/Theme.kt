package com.weibosave.ui.theme

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

private val accentOrange = Color(0xFFE8533A)
private val accentOrangeLight = Color(0xFFFF8A70)

private val DarkColors = darkColorScheme(
    primary = accentOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D1A12),
    onPrimaryContainer = accentOrangeLight,
    background = Color(0xFF0C0E14),
    surface = Color(0xFF141820),
    surfaceVariant = Color(0xFF1C2233),
    onSurface = Color(0xFFEDE8DF),
    onSurfaceVariant = Color(0xFF8490A8),
)

private val LightColors = lightColorScheme(
    primary = accentOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDD7),
    onPrimaryContainer = Color(0xFF3A0A02),
    background = Color(0xFFFAF9F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF3EEE8),
    onSurface = Color(0xFF1C1A18),
    onSurfaceVariant = Color(0xFF6B6460),
)

@Composable
fun WeiboSaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
