package com.katchy.focuslive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BrishInk,
    onPrimary = Color.White,
    primaryContainer = BrishYellow,
    onPrimaryContainer = BrishInk,
    secondary = BrishYellow,
    onSecondary = BrishInk,
    secondaryContainer = BrishYellowSoft,
    onSecondaryContainer = BrishInk,
    tertiary = BrishWarning,
    background = BrishCream,
    onBackground = BrishInk,
    surface = BrishCard,
    onSurface = BrishInk,
    surfaceVariant = BrishInsetSoft,
    onSurfaceVariant = BrishTextSecondary,
    outline = BrishOutlineLight,
    outlineVariant = BrishHairline,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = BrishCard,
    surfaceContainerLow = BrishCard,
    surfaceContainer = BrishCard,
    surfaceContainerHigh = BrishCard,
    surfaceContainerHighest = BrishInset,
    error = AntiError
)

private val DarkColorScheme = darkColorScheme(
    primary = BrishNightText,
    onPrimary = BrishNight,
    primaryContainer = BrishYellow,
    onPrimaryContainer = BrishInk,
    secondary = BrishYellow,
    onSecondary = BrishInk,
    secondaryContainer = BrishSecondaryContainerDark,
    onSecondaryContainer = BrishYellowLight,
    tertiary = BrishTertiaryDark,
    background = BrishNight,
    onBackground = BrishNightText,
    surface = BrishNightCard,
    onSurface = BrishNightText,
    surfaceVariant = BrishNightInset,
    onSurfaceVariant = BrishNightTextSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = BrishNightCard,
    surfaceContainerLow = BrishNightCard,
    surfaceContainer = BrishNightCard,
    surfaceContainerHigh = BrishNightCard,
    surfaceContainerHighest = BrishNightInset,
    outline = Color.White.copy(alpha = 0.10f),
    outlineVariant = Color.White.copy(alpha = 0.06f),
    error = AntiError
)

private fun blendColors(base: Color, accent: Color, fraction: Float): Color {
    return Color(
        red = base.red + (accent.red - base.red) * fraction,
        green = base.green + (accent.green - base.green) * fraction,
        blue = base.blue + (accent.blue - base.blue) * fraction,
        alpha = base.alpha + (accent.alpha - base.alpha) * fraction
    )
}

@Composable
fun BrishTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    dynamicColor: Boolean = true, // Material You support
    accentColor: Int? = null, // Custom accent support
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val activeAccent = if (accentColor != null) Color(accentColor) else AntiPrimary
    
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && accentColor == null -> {
            if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
        }
        darkTheme -> {
            if (isAmoled) {
                val baseSurface = BrishAmoledSurface
                val baseSurfaceVariant = BrishAmoledSurfaceVariant
                val tintedSurface = blendColors(baseSurface, activeAccent, 0.03f)
                val tintedSurfaceVariant = blendColors(baseSurfaceVariant, activeAccent, 0.06f)
                DarkColorScheme.copy(
                    background = Color.Black,
                    surface = tintedSurface,
                    surfaceVariant = tintedSurfaceVariant
                )
            } else {
                val baseBackground = BrishNight
                val baseSurface = BrishNightCard
                val baseSurfaceVariant = BrishNightInset
                val tintedBackground = blendColors(baseBackground, activeAccent, 0.02f)
                val tintedSurface = blendColors(baseSurface, activeAccent, 0.04f)
                val tintedSurfaceVariant = blendColors(baseSurfaceVariant, activeAccent, 0.06f)
                DarkColorScheme.copy(
                    background = tintedBackground,
                    surface = tintedSurface,
                    surfaceVariant = tintedSurfaceVariant
                )
            }
        }
        else -> LightColorScheme
    }
    
    // Apply custom accent if present
    val finalScheme = if (accentColor != null) {
        val accent = Color(accentColor)
        
        // Check if accent is very dark to adjust container visibility
        val isVeryDark = accentColor == 0xFF000000.toInt() || accentColor == 0xFF111827.toInt()
        
        // Dynamic text color on primary: dark ink if accent color is light in light theme, otherwise white
        val isAccentLight = accent.luminance() > 0.5f
        val dynamicOnPrimary = if (isAccentLight && !darkTheme) BrishInk else Color.White
        
        baseScheme.copy(
            primary = accent,
            secondary = accent,
            tertiary = accent, // unify accent
            onPrimary = dynamicOnPrimary,
            primaryContainer = if (darkTheme && isVeryDark) Color.White.copy(alpha = 0.12f) else accent.copy(alpha = 0.15f),
            onPrimaryContainer = if (darkTheme && isVeryDark) Color.White else accent
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography,
        shapes = androidx.compose.material3.Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
        ),
        content = content
    )
}

/**
 * Centralized dark theme detection utility.
 * Use this instead of repeating `MaterialTheme.colorScheme.background.luminance() < 0.5f` everywhere.
 */
@Composable
fun isDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background.luminance() < 0.5f
}
