package com.example.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF063326),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFC7F3E1),
    secondary = Color(0xFF9EC8B8),
    onSecondary = Color(0xFF12362A),
    secondaryContainer = Color(0xFF21483A),
    onSecondaryContainer = Color(0xFFD5EEE4),
    tertiary = AccentGoldLight,
    onTertiary = Color(0xFF3F2E00),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder,
    outlineVariant = Color(0xFF20382F),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = OnPrimaryContainerBlue,
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainerTeal,
    onSecondaryContainer = Color(0xFF183A2F),
    tertiary = AccentGold,
    onTertiary = Color.White,
    tertiaryContainer = AccentGoldContainer,
    background = NeutralLight,
    onBackground = NeutralDark,
    surface = SurfaceCard,
    onSurface = NeutralDark,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = NeutralSlate,
    outline = SurfaceBorder,
    outlineVariant = Color(0xFFE8EEEB),
    error = ErrorRed,
    errorContainer = ErrorRedContainer
)

private val LinguaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LinguaFaTheme(
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
        shapes = LinguaShapes,
        content = content
    )
}
