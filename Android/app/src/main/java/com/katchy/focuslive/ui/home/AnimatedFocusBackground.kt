package com.katchy.focuslive.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.math.cos

@Composable
fun AnimatedFocusBackground(
    modifier: Modifier = Modifier,
    customTint: Color? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Smooth time factor for floating movement
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    // Star flickering factor
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starAlpha"
    )

    Spacer(
        modifier = modifier
            .drawBehind {
                val width = size.width
                val height = size.height
                if (width < 1f || height < 1f) return@drawBehind

                // --- 1. DEEP NEBULA ORBS ---
                // Orb 1: Deep Space Blue / Tint (Top Right)
                val orb1Offset = Offset(
                    x = width * (0.8f + 0.1f * sin(time)),
                    y = height * (0.2f + 0.1f * cos(time))
                )
                val color1 = customTint?.copy(alpha = 0.15f) ?: Color(0xFF1E3A8A).copy(alpha = 0.15f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color1, Color.Transparent),
                        center = orb1Offset,
                        radius = width * 0.7f
                    ),
                    center = orb1Offset,
                    radius = width * 0.7f
                )

                // Orb 2: Midnight Purple / Tint (Bottom Left)
                val orb2Offset = Offset(
                    x = width * (0.2f + 0.1f * cos(time * 0.7f)),
                    y = height * (0.8f + 0.1f * sin(time * 0.7f))
                )
                val color2 = customTint?.copy(alpha = 0.12f) ?: Color(0xFF4C1D95).copy(alpha = 0.15f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color2, Color.Transparent),
                        center = orb2Offset,
                        radius = width * 0.7f
                    ),
                    center = orb2Offset,
                    radius = width * 0.7f
                )

                // Orb 3: Very Subtle Indigo / Tint (Center)
                val orb3Offset = Offset(
                    x = width * (0.5f + 0.05f * sin(time * 1.3f)),
                    y = height * (0.5f + 0.05f * cos(time * 1.3f))
                )
                val color3 = customTint?.copy(alpha = 0.08f) ?: Color(0xFF312E81).copy(alpha = 0.1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color3, Color.Transparent),
                        center = orb3Offset,
                        radius = width * 0.5f
                    ),
                    center = orb3Offset,
                    radius = width * 0.5f
                )

                // --- 2. STATIC STARS (Subtle flickers) ---
                val starPositions = listOf(
                    Offset(width * 0.15f, height * 0.15f),
                    Offset(width * 0.85f, height * 0.12f),
                    Offset(width * 0.45f, height * 0.35f),
                    Offset(width * 0.72f, height * 0.75f),
                    Offset(width * 0.22f, height * 0.88f),
                    Offset(width * 0.90f, height * 0.90f),
                    Offset(width * 0.08f, height * 0.65f),
                    Offset(width * 0.55f, height * 0.10f),
                    Offset(width * 0.35f, height * 0.60f),
                    Offset(width * 0.10f, height * 0.40f)
                )

                starPositions.forEachIndexed { index, pos ->
                    val individualFlicker = (starAlpha * (0.8f + 0.2f * sin(time * (index + 1) * 0.5f)))
                    drawCircle(
                        color = Color.White.copy(alpha = individualFlicker),
                        radius = 1.2.dp.toPx(),
                        center = pos
                    )
                    // Halo for some stars
                    if (index % 3 == 0) {
                        drawCircle(
                            color = Color.White.copy(alpha = individualFlicker * 0.2f),
                            radius = 4.dp.toPx(),
                            center = pos
                        )
                    }
                }
            }
    )
}
