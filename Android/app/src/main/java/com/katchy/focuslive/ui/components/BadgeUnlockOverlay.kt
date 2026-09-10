package com.katchy.focuslive.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import com.katchy.focuslive.ui.theme.AntiPrimary
import com.katchy.focuslive.ui.util.glassmorphism
import com.katchy.focuslive.ui.util.bounceClick
import com.katchy.focuslive.util.GamificationLogic
import kotlinx.coroutines.isActive
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val color: Color,
    val shape: Int, // 0 = Rect, 1 = Circle, 2 = Triangle
    var angle: Float,
    val rotationSpeed: Float,
    var wobble: Float,
    val wobbleSpeed: Float,
    var alpha: Float,
    val decay: Float
)

@Composable
fun BadgeUnlockOverlay(
    badgeId: String?,
    onDismiss: () -> Unit
) {
    if (badgeId == null) return

    val badgeIcon = remember(badgeId) { GamificationLogic.getBadgeIcon(badgeId) }
    val badgeName = remember(badgeId) { GamificationLogic.getBadgeName(badgeId) }
    
    val badgeDesc = remember(badgeId) {
        when(badgeId) {
            GamificationLogic.BADGE_EARLY_BIRD -> "Completaste una tarea temprano en la mañana (4 AM - 7 AM). ¡A quien madruga Brish le ayuda!"
            GamificationLogic.BADGE_FINANCIER -> "Registraste 30 transacciones en finanzas. ¡Maestro de la administración económica!"
            GamificationLogic.BADGE_STREAK_3 -> "Mantuviste tu racha de hábitos activa por 3 días seguidos. ¡Buen inicio!"
            GamificationLogic.BADGE_STREAK_7 -> "¡Increíble! Mantuviste una racha de hábitos de 7 días. ¡Eres imparable!"
            GamificationLogic.BADGE_STREAK_30 -> "¡Logro legendario! Alcanzaste 30 días de racha. ¡Nivel deidad!"
            GamificationLogic.BADGE_POMODORO_1 -> "Completaste tu primera sesión de enfoque de 25 minutos. ¡El primer paso de muchos!"
            GamificationLogic.BADGE_POMODORO_5 -> "Alcanzaste 125 minutos de enfoque total. ¡Tu mente está en un estado óptimo!"
            GamificationLogic.BADGE_HABIT_MASTER -> "Completaste 10 hábitos diarios. ¡La disciplina es el puente al éxito!"
            GamificationLogic.BADGE_PLANNER_PRO -> "Completaste 10 tareas en tu agenda. ¡Organización al máximo nivel!"
            GamificationLogic.BADGE_NOTE_TAKER -> "Escribiste 5 notas en el bloc. ¡Ninguna gran idea se te escapará!"
            GamificationLogic.BADGE_PREMIUM_MEMBER -> "Activaste tu membresía VIP Brish Pro. ¡Bienvenido a la élite de productividad!"
            else -> "¡Has completado una meta y ganado una insignia especial!"
        }
    }

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    
    // Confetti particles logic
    LaunchedEffect(badgeId) {
        particles.clear()
        val random = Random()
        val colors = listOf(
            Color(0xFFFBBF24), // Gold / Amber
            Color(0xFF06B6D4), // Cyan
            Color(0xFFEC4899), // Pink
            Color(0xFF10B981), // Emerald
            Color(0xFF818CF8), // Indigo
            Color(0xFFEF4444)  // Rose
        )
        
        // Spawn 90 optimized particles for smooth 60fps/120fps physics
        repeat(90) {
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 6f + random.nextFloat() * 12f
            particles.add(
                ConfettiParticle(
                    x = screenWidthPx / 2f,
                    y = screenHeightPx / 3f, // Explode from card area
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 8f, // initial burst upwards
                    size = 12f + random.nextFloat() * 16f,
                    color = colors[random.nextInt(colors.size)],
                    shape = random.nextInt(3),
                    angle = random.nextFloat() * 360f,
                    rotationSpeed = -4f + random.nextFloat() * 8f,
                    wobble = random.nextFloat() * 100f,
                    wobbleSpeed = 0.04f + random.nextFloat() * 0.08f,
                    alpha = 1.0f,
                    decay = 0.006f + random.nextFloat() * 0.008f
                )
            )
        }
        
        // Game loop for updating particles
        while (isActive && particles.any { it.alpha > 0 }) {
            withFrameNanos { frameTime ->
                for (i in particles.indices) {
                    val p = particles[i]
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += 0.25f // Gravity
                    p.vx *= 0.98f // Air resistance
                    p.vy *= 0.98f
                    p.angle += p.rotationSpeed
                    p.wobble += p.wobbleSpeed
                    p.alpha = maxOf(0f, p.alpha - p.decay)
                }
            }
        }
    }

    // Animation control state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Delayed dismissal callback to let exit transition finish
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            kotlinx.coroutines.delay(200)
            onDismiss()
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Dynamic accent color depending on the badge category
    val accentColor = remember(badgeId, isDark) {
        when(badgeId) {
            GamificationLogic.BADGE_EARLY_BIRD -> if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5) // Tasks Indigo
            GamificationLogic.BADGE_PLANNER_PRO -> if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
            GamificationLogic.BADGE_NOTE_TAKER -> if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309) // Notes Amber / Gold
            GamificationLogic.BADGE_FINANCIER -> if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626) // Finance Rose / Red
            GamificationLogic.BADGE_STREAK_3,
            GamificationLogic.BADGE_STREAK_7,
            GamificationLogic.BADGE_STREAK_30,
            GamificationLogic.BADGE_HABIT_MASTER -> if (isDark) Color(0xFF10B981) else Color(0xFF059669) // Habits Emerald / Green
            GamificationLogic.BADGE_POMODORO_1,
            GamificationLogic.BADGE_POMODORO_5 -> if (isDark) Color(0xFF06B6D4) else Color(0xFF0891B2) // Brand Cyan
            GamificationLogic.BADGE_PREMIUM_MEMBER -> if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309) // Gold
            else -> if (isDark) Color(0xFF06B6D4) else Color(0xFF0891B2) // Fallback to Brand Cyan
        }
    }

    // Backdrop opacity transition
    val backdropAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.85f else 0f,
        animationSpec = tween(durationMillis = 200, easing = EaseOutQuart),
        label = "backdropAlpha"
    )

    // Card scale transition (Bouncy spring on enter, quick shrink on exit)
    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = if (isVisible) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            tween(durationMillis = 200, easing = EaseOutQuart)
        },
        label = "cardScale"
    )

    // Card opacity transition
    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (isVisible) 250 else 150),
        label = "cardAlpha"
    )

    val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = backdropAlpha * 0.75f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = backdropAlpha * 0.35f),
                        scrimColor
                    ),
                    radius = (screenWidthPx * 1.1f).coerceAtLeast(1f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Draw the physics-based confetti particles (only if visible and animating)
        if (backdropAlpha > 0.05f) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { p ->
                    if (p.alpha > 0f) {
                        rotate(p.angle, Offset(p.x, p.y)) {
                            val wobbleScale = cos(p.wobble)
                            val width = p.size * wobbleScale
                            val height = p.size
                            
                            when (p.shape) {
                                0 -> { // Rectangle
                                    drawRect(
                                        color = p.color.copy(alpha = p.alpha * backdropAlpha),
                                        topLeft = Offset(p.x - width / 2, p.y - height / 2),
                                        size = Size(width, height)
                                    )
                                }
                                1 -> { // Circle
                                    drawCircle(
                                        color = p.color.copy(alpha = p.alpha * backdropAlpha),
                                        radius = width / 2,
                                        center = Offset(p.x, p.y)
                                    )
                                }
                                2 -> { // Triangle
                                    val path = Path().apply {
                                        moveTo(p.x, p.y - height / 2)
                                        lineTo(p.x - width / 2, p.y + height / 2)
                                        lineTo(p.x + width / 2, p.y + height / 2)
                                        close()
                                    }
                                    drawPath(
                                        path = path,
                                        color = p.color.copy(alpha = p.alpha * backdropAlpha)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Achievement Glassmorphic Card
        val isPremium = badgeId == GamificationLogic.BADGE_PREMIUM_MEMBER
        val cardBorderBrush = if (isPremium) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFBBF24), // Luxury Gold
                    Color(0xFFFFD54F),
                    Color(0xFFD97706),
                    Color.Transparent,
                    Color(0xFFFBBF24).copy(alpha = 0.4f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    accentColor,
                    accentColor.copy(alpha = 0.3f),
                    Color.Transparent,
                    accentColor.copy(alpha = 0.1f)
                )
            )
        }

        Box(
            modifier = Modifier
                .padding(32.dp)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = cardAlpha
                }
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .glassmorphism(
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    borderColor = Color.Transparent,
                    borderWidth = 0.dp
                )
                .border(
                    width = 1.5.dp,
                    brush = cardBorderBrush,
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(enabled = false) {}
                .widthIn(max = 380.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header tag (capsule shape matching dynamic category accent color)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "¡LOGRO DESBLOQUEADO!",
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsing glass sphere container for the badge icon with rotating rays and orbits
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = EaseInOutQuart),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(24000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )
                val outerRotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(12000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "outerRotation"
                )
                val innerRotation by infiniteTransition.animateFloat(
                    initialValue = 360f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "innerRotation"
                )
                val emojiOffset by infiniteTransition.animateFloat(
                    initialValue = -3f,
                    targetValue = 3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = EaseInOutQuart),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "emojiOffset"
                )

                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Sunburst rotating rays in Canvas
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .size(190.dp)
                            .graphicsLayer { rotationZ = rotationAngle }
                    ) {
                        val numRays = 12
                        val rayAngle = 360f / numRays
                        for (i in 0 until numRays) {
                            val startAngle = i * rayAngle - 10f
                            drawArc(
                                color = accentColor.copy(alpha = 0.08f),
                                startAngle = startAngle,
                                sweepAngle = 20f,
                                useCenter = true,
                                topLeft = Offset.Zero,
                                size = size
                            )
                        }
                    }

                    // Double-rotating dashed orbits
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .size(148.dp)
                            .graphicsLayer { rotationZ = outerRotation }
                    ) {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.35f),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            )
                        )
                    }
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .size(126.dp)
                            .graphicsLayer { rotationZ = innerRotation }
                    ) {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.2f),
                            style = Stroke(
                                width = 1.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        )
                    }

                    // Floating particle decorations (Stars/Sparkles) in the background
                    val starsAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.9f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = EaseInOutQuart),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "starsAlpha"
                    )
                    
                    // Top Left Sparkle
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 28.dp, top = 28.dp)
                            .size(16.dp)
                            .graphicsLayer { alpha = starsAlpha; scaleX = starsAlpha; scaleY = starsAlpha }
                    )
                    // Bottom Right Sparkle
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 24.dp, bottom = 24.dp)
                            .size(18.dp)
                            .graphicsLayer { alpha = 1f - starsAlpha; scaleX = 1f - starsAlpha; scaleY = 1f - starsAlpha }
                    )
                    // Top Right Sparkle
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 32.dp, top = 24.dp)
                            .size(14.dp)
                            .graphicsLayer { alpha = starsAlpha; scaleX = starsAlpha; scaleY = starsAlpha }
                    )

                    // Outer glow shadow
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )
                    
                    // Glass sphere inner base with radial gradient
                    val sphereBrush = if (isPremium) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFAEC), // Light gold center
                                Color(0xFFFFD54F), // Amber gold
                                Color(0xFFF59E0B)  // Gold border area
                            ),
                            radius = 160f
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.45f),
                                accentColor.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            radius = 160f
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(brush = sphereBrush, shape = CircleShape)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = if (isPremium) {
                                        listOf(Color.White, Color(0xFFFFB300).copy(alpha = 0.3f))
                                    } else {
                                        listOf(Color.White.copy(alpha = 0.8f), accentColor.copy(alpha = 0.2f))
                                    }
                                ),
                                shape = CircleShape
                            )
                            .shadow(12.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeIcon,
                            fontSize = 54.sp,
                            modifier = Modifier.graphicsLayer {
                                translationY = emojiOffset.dp.toPx()
                                rotationZ = emojiOffset * 1.5f
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Achievement name using rich text gradient brush
                val textGradientBrush = if (isPremium) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFD97706),
                            Color(0xFFF59E0B),
                            Color(0xFFFFD54F),
                            Color(0xFFF59E0B),
                            Color(0xFFD97706)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor,
                            accentColor.copy(alpha = 0.85f),
                            accentColor.copy(alpha = 0.65f)
                        )
                    )
                }

                Text(
                    text = badgeName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        brush = textGradientBrush,
                        letterSpacing = (-0.5).sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Achievement description using adaptive theme color
                Text(
                    text = badgeDesc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Rewards Banner - Designed as a luxury neon glass ticket
                val rewardsBorderBrush = if (isPremium) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF59E0B).copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.08f),
                            Color(0xFFF59E0B).copy(alpha = 0.4f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.08f),
                            accentColor.copy(alpha = 0.4f)
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphism(
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            borderColor = Color.Transparent,
                            borderWidth = 0.dp
                        )
                        .border(
                            width = 1.2.dp,
                            brush = rewardsBorderBrush,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 2.dp)
                            )
                            Text(
                                text = "+100 XP",
                                color = Color(0xFF06B6D4), // Cyan for XP
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Experiencia",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🪙",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "+50 Monedas",
                                color = Color(0xFFFBBF24), // Gold for coins
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Recompensa",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action button (Close) using custom bounceClick and accentColor gradient
                val buttonGradient = if (isPremium) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFD97706),
                            Color(0xFFF59E0B),
                            Color(0xFFFFB300)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor,
                            accentColor.copy(alpha = 0.85f)
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .background(
                            brush = buttonGradient,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .bounceClick {
                            isVisible = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "¡Excelente!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
