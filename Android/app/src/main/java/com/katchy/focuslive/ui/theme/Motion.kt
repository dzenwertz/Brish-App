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
    val overshoot: Easing = CubicBezierEasing(0.2f, 1.08f, 0.35f, 1f) // apenas un toque de rebote
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
    const val pressScale = 0.98f

    fun <T> pressSpring() = spring<T>(dampingRatio = 0.8f, stiffness = 700f)
    fun <T> overshoot() = spring<T>(dampingRatio = 0.75f, stiffness = 600f)
    fun <T> sheetSpring() = spring<T>(dampingRatio = 0.9f, stiffness = 480f)
    fun <T> cardSpring() = spring<T>(dampingRatio = 0.86f, stiffness = 320f)
    fun <T> screenTween() = tween<T>(screenMs, easing = emphasized)
}

/**
 * Antes: entrada en cascada (cada fila aparecía con fundido escalonado). Se repetía cada vez que
 * una pantalla volvía a mostrarse y, al regresar con «atrás», dejaba la pantalla en blanco un
 * instante. Como en iOS, ahora el contenido ya está ahí: el único movimiento es la transición.
 */
@Suppress("UNUSED_PARAMETER", "UnusedReceiverParameter")
fun Modifier.brishCascade(index: Int, key: Any? = Unit): Modifier = this

/**
 * Movimiento de navegación al estilo iOS.
 * Push: la pantalla nueva entra desde la derecha y la anterior se desplaza 1/4 y se atenúa.
 * Modal: sube desde abajo y la de debajo se queda quieta.
 */
object IosMotion {
    /** Curva de las transiciones de UIKit: arranca rápido y llega muy suave. */
    val curve: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    const val pushMs = 380
    const val modalMs = 420
    const val parallax = 0.25f
    const val dimAlpha = 0.7f
}
