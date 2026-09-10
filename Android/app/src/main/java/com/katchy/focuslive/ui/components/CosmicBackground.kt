package com.katchy.focuslive.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.katchy.focuslive.data.repository.UserStatsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlin.math.sin

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CosmicBackgroundEntryPoint {
    fun userStatsRepository(): UserStatsRepository
}

@Composable
fun CosmicBackground(
    modifier: Modifier = Modifier,
    showOrbs: Boolean = true,
    showParticles: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current.applicationContext
    val userStatsRepository = remember(context) {
        EntryPointAccessors.fromApplication(
            context,
            CosmicBackgroundEntryPoint::class.java
        ).userStatsRepository()
    }
    val isPremium by userStatsRepository.isPremium.collectAsState()

    val gradientColors = if (isPremium) {
        listOf(
            Color(0xFF16082B), // Deep Cosmic Purple
            Color(0xFF070B19), // Deep Indigo / Dark Navy
            Color(0xFF1F1407)  // Warm Gold/Amber tint at bottom
        )
    } else {
        listOf(
            Color(0xFF0A0E1A), // Dark Navy
            Color(0xFF111827), // Deep Charcoal
            Color(0xFF1A1510)  // Warm brown tint at bottom
        )
    }

    val gradient = Brush.verticalGradient(colors = gradientColors)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        if (showOrbs) {
            WarmGlowOrbs(isPremium = isPremium)
        }
        if (showParticles) {
            StarParticles(isPremium = isPremium)
        }
        content()
    }
}

@Composable
private fun WarmGlowOrbs(isPremium: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowOrbs")

    val orbOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb1"
    )
    val orbOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb2"
    )
    val orbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orbAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w < 1f || h < 1f) return@Canvas

        // Top right orb (Magenta/Purple for Premium, Cyan for standard)
        val colorTopRight = if (isPremium) Color(0xFFD946EF) else Color(0xFF22D3EE)
        val xTopRight = w * 0.8f + kotlin.math.cos(orbOffset1) * 35.dp.toPx()
        val yTopRight = h * 0.15f + kotlin.math.sin(orbOffset1) * 35.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorTopRight.copy(alpha = orbAlpha), Color.Transparent),
                center = Offset(xTopRight, yTopRight),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(xTopRight, yTopRight)
        )

        // Bottom left orb (Royal Indigo for Premium, Cyan for standard)
        val colorBottomLeft = if (isPremium) Color(0xFF6366F1) else Color(0xFF06B6D4)
        val xBottomLeft = w * 0.15f + kotlin.math.sin(orbOffset2) * 25.dp.toPx()
        val yBottomLeft = h * 0.75f + kotlin.math.cos(orbOffset2) * 25.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorBottomLeft.copy(alpha = orbAlpha * 0.85f), Color.Transparent),
                center = Offset(xBottomLeft, yBottomLeft),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(xBottomLeft, yBottomLeft)
        )

        // Center orb (Amber/Gold for Premium, sky accent for standard)
        val colorCenter = if (isPremium) Color(0xFFFBBF24) else Color(0xFFCFFAFE)
        val xCenter = w * 0.5f + kotlin.math.sin(orbOffset1 * 0.5f) * 15.dp.toPx()
        val yCenter = h * 0.45f + kotlin.math.cos(orbOffset2 * 0.5f) * 15.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorCenter.copy(alpha = orbAlpha * (if (isPremium) 0.55f else 0.45f)), Color.Transparent),
                center = Offset(xCenter, yCenter),
                radius = w * 0.4f
            ),
            radius = w * 0.4f,
            center = Offset(xCenter, yCenter)
        )
    }
}

@Composable
private fun StarParticles(isPremium: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val particles = remember {
        List(30) {
            val isGold = isPremium && Math.random().toFloat() < 0.4f // 40% gold particles if premium
            BackgroundParticle(
                x = Math.random().toFloat(),
                y = Math.random().toFloat(),
                radius = (0.5f + Math.random().toFloat() * 2f),
                speed = (0.15f + Math.random().toFloat() * 0.6f),
                isGold = isGold
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val offsetY = (p.y + time * p.speed) % 1f
            val yPos = h * (1f - offsetY)
            
            // Add a very subtle horizontal sway to the stars to make them drift organically
            val swayFactor = kotlin.math.sin(offsetY * Math.PI * 2f).toFloat() * 6.dp.toPx()
            val xPos = w * p.x + swayFactor

            val alpha = 0.15f + 0.5f * kotlin.math.sin(offsetY * Math.PI).toFloat()

            val particleColor = if (p.isGold) {
                if (p.radius > 1.5f) {
                    Color(0xFFFBBF24).copy(alpha = alpha * 0.8f)
                } else {
                    Color(0xFFF59E0B).copy(alpha = alpha)
                }
            } else {
                if (p.radius > 1.5f) {
                    Color(0xFFCFFAFE).copy(alpha = alpha * 0.7f)
                } else {
                    Color.White.copy(alpha = alpha)
                }
            }

            drawCircle(
                color = particleColor,
                radius = p.radius.dp.toPx(),
                center = Offset(xPos, yPos)
            )
        }
    }
}

private data class BackgroundParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val isGold: Boolean = false
)

