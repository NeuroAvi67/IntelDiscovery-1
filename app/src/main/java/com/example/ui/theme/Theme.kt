package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.ThemePreference

private val IntelDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = DeepObsidian,
    primaryContainer = ElectricBlueContainerDark,
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = SubtleVioletSoft,
    onSecondary = DeepObsidian,
    secondaryContainer = SubtleVioletContainerDark,
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = CyanSpark,
    onTertiary = DeepObsidian,
    background = DeepObsidian,
    onBackground = DarkTextPrimary,
    surface = DarkNavySurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkNavyContainer,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = DeepObsidian,
    surfaceContainerLow = DarkNavySurface,
    surfaceContainer = DarkNavyContainer,
    surfaceContainerHigh = DarkNavyHigh,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1E293B),
    error = ErrorCoral,
    onError = DeepObsidian
)

private val IntelLightColorScheme = lightColorScheme(
    primary = ElectricBlueDeep,
    onPrimary = Color.White,
    primaryContainer = ElectricBlueContainerLight,
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = SubtleVioletDeep,
    onSecondary = Color.White,
    secondaryContainer = SubtleVioletContainerLight,
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = CyanSparkDeep,
    onTertiary = Color.White,
    background = LightAlabaster,
    onBackground = LightTextPrimary,
    surface = LightPureSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSlateContainer,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = LightPureSurface,
    surfaceContainerLow = LightAlabaster,
    surfaceContainer = LightSlateContainer,
    surfaceContainerHigh = LightSlateHigh,
    outline = LightOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = ErrorCoralDeep,
    onError = Color.White
)

val IntelShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun IntelDiscoveryTheme(
    themePreference: ThemePreference = ThemePreference.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) IntelDarkColorScheme else IntelLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = IntelTypography,
        shapes = IntelShapes,
        content = content
    )
}
