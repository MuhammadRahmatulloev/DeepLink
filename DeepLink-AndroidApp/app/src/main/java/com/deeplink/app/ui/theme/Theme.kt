package com.deeplink.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = BrandPurple,
    secondary = AccentTeal,
    tertiary = AccentTeal,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurface.copy(alpha = 0.88f),
    onPrimary = LightSurface,
    onSecondary = DarkBackground,
    onBackground = LightSurface,
    onSurface = LightSurface,
    onSurfaceVariant = LightSurface.copy(alpha = 0.72f)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPurple,
    secondary = AccentTeal,
    tertiary = AccentTeal,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightBackground,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurface.copy(alpha = 0.62f)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DeepLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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
        shapes = AppShapes,
        content = content
    )
}