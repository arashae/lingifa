package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF134E4A),
    tertiary = AccentGoldLight,
    onTertiary = Color(0xFF451A03),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder,
    error = Color(0xFFF87171)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = OnPrimaryContainerBlue,
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainerTeal,
    tertiary = AccentGold,
    onTertiary = Color.White,
    tertiaryContainer = AccentGoldContainer,
    background = NeutralLight,
    onBackground = NeutralDark,
    surface = SurfaceCard,
    onSurface = NeutralDark,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = NeutralSlate,
    outline = SurfaceBorder,
    error = ErrorRed,
    errorContainer = ErrorRedContainer
)

@Composable
fun LinguaFaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent educational branding
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
        content = content
    )
}
