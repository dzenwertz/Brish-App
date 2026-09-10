package com.katchy.focuslive.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A safe, high-performance "Glossy" effect that simulates glass
 * without using expensive or risky RenderEffect/Blur APIs.
 * This guarantees 0 crashes and a consistent Premium look on all devices.
 */
fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color? = null, // Defaults to theme-specific base
    borderColor: Color? = null,     // Defaults to theme-specific border
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 0.dp // Ignored in this safe version, kept for API compatibility
): Modifier = composed {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val baseColor = backgroundColor ?: (if (isDark) Color(0xFF131B2E).copy(alpha = 0.70f) else Color.White.copy(alpha = 0.65f))
    val borderCol = borderColor ?: (if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))

    this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = baseColor.alpha * 0.85f), // Top: slightly more transparent
                    baseColor.copy(alpha = baseColor.alpha * 0.98f)  // Bottom: almost solid
                )
            ),
            shape = shape
        )
        // Add a "Glossy Reflection" overlay
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDark) 0.05f else 0.10f), // Subtle shine at top-left
                    Color.Transparent,
                    Color.Transparent
                )
            ),
            shape = shape
        )
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDark) 0.15f else 0.30f), // Top border highlight (Light catch)
                    borderCol                                              // Bottom border shadow (uses provided alpha)
                )
            ),
            shape = shape
        )
}
