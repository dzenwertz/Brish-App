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
    primary = AntiPrimary,
    onPrimary = Color.White,
    secondary = AntiAccent,
    onSecondary = Color.White,
    background = AntiFlashWhite,
    onBackground = AntiTextPrimary,
    surface = AntiWhite,
    onSurface = AntiTextPrimary,
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = AntiTextSecondary,
    outline = Color(0xFF7B8493),
    outlineVariant = Color(0xFFE5E8EE),
    surfaceTint = Color.Transparent,
    surfaceContainer = Color(0xFFF7F8FA),
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFEFF1F5),
    error = AntiError
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF3F4F6),
    onPrimary = Color(0xFF111827),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0F172A),
    background = Color(0xFF0C0D12), // Unified Deep Obsidian Background
    onBackground = Color(0xFFF8FAFC), // Slate 50
    surface = Color(0xFF15171E), // Unified Obsidian Card Surface
    onSurface = Color(0xFFF3F4F6), // Slate 100
    surfaceVariant = Color(0xFF1E212B), // Unified Chip / Inner Surface
    onSurfaceVariant = Color(0xFF94A3B8), // Slate 400
    outline = Color.White.copy(alpha = 0.08f),
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
                val baseSurface = Color(0xFF101116)
                val baseSurfaceVariant = Color(0xFF1A1C24)
                val tintedSurface = blendColors(baseSurface, activeAccent, 0.03f)
                val tintedSurfaceVariant = blendColors(baseSurfaceVariant, activeAccent, 0.06f)
                DarkColorScheme.copy(
                    background = Color(0xFF000000),
                    surface = tintedSurface,
                    surfaceVariant = tintedSurfaceVariant
                )
            } else {
                val baseBackground = Color(0xFF0C0D12)
                val baseSurface = Color(0xFF15171E)
                val baseSurfaceVariant = Color(0xFF1E212B)
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
        
        // Dynamic text color on primary: dark Slate if accent color is light in light theme, otherwise white
        val isAccentLight = accent.luminance() > 0.5f
        val dynamicOnPrimary = if (isAccentLight && !darkTheme) Color(0xFF0F172A) else Color.White
        
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
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
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
