package com.katchy.focuslive.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Shared motion values for the Brish interface. */
object MotionTokens {
    val emphasized: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val overshoot: Easing = CubicBezierEasing(0.2f, 1.25f, 0.35f, 1f)
    val standard: Easing = EaseOutCubic
    val exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val microMs = 150
    const val componentMs = 220
    const val screenMs = 420
    const val heroMs = 480
    const val celebrationMs = 620
    const val tabExitMs = 90
    const val tabEnterMs = 210
    const val exitRatio = 0.7f
    const val staggerMs = 28L
    const val staggerLimit = 4
    const val enterOffsetDp = 8
    const val reducedMotionMs = 150
    const val pressScale = 0.96f

    fun <T> pressSpring() = spring<T>(dampingRatio = 0.55f, stiffness = 700f)
    fun <T> overshoot() = spring<T>(dampingRatio = 0.58f, stiffness = 520f)
    fun <T> sheetSpring() = spring<T>(dampingRatio = 0.78f, stiffness = 420f)
    fun <T> cardSpring() = spring<T>(dampingRatio = 0.86f, stiffness = 320f)
    fun <T> screenTween() = tween<T>(screenMs, easing = emphasized)
}

/**
 * Staggered cascade entrance (48 ms step, max 6 items, 20 dp vertical slide + fade).
 * Falls back to a <= 150 ms fade when Reduce Motion is active.
 */
@Composable
fun Modifier.brishCascade(index: Int, key: Any? = Unit): Modifier {
    val reduced = rememberReducedMotion()
    val clamped = index.coerceIn(0, MotionTokens.staggerLimit - 1)
    var visible by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (!reduced && clamped > 0) {
            delay(clamped * MotionTokens.staggerMs)
        }
        visible = true
    }
    // Se guardan los State (sin «by») y se leen dentro de graphicsLayer: así la animación solo
    // vuelve a dibujar la capa y no recompone la fila en cada fotograma.
    val alpha = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (reduced) MotionTokens.reducedMotionMs else MotionTokens.componentMs,
            easing = MotionTokens.emphasized
        ),
        label = "cascadeAlpha"
    )
    val offsetPx = with(LocalDensity.current) { MotionTokens.enterOffsetDp.dp.toPx() }
    val translationY = animateFloatAsState(
        targetValue = if (visible || reduced) 0f else offsetPx,
        animationSpec = tween(
            durationMillis = if (reduced) MotionTokens.reducedMotionMs else MotionTokens.componentMs,
            easing = MotionTokens.emphasized
        ),
        label = "cascadeOffset"
    )
    return this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
    }
}
