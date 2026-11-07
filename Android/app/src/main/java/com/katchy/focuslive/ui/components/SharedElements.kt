package com.katchy.focuslive.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.katchy.focuslive.ui.theme.MotionTokens
import com.katchy.focuslive.ui.theme.rememberReducedMotion

/** Ámbito de la transición compartida que envuelve la navegación principal. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Ámbito animado del destino de navegación actual. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Resorte de pantalla para que la tarjeta crezca sin rebotes bruscos. */
@OptIn(ExperimentalSharedTransitionApi::class)
private val CardToScreen = BoundsTransform { _, _ -> MotionTokens.cardSpring() }

/**
 * Une dos elementos con la misma [key] en pantallas distintas: al navegar, la tarjeta
 * se expande hasta ocupar la pantalla nueva y al volver se encoge a su sitio.
 * Sin ámbito (vista previa, pruebas) o con movimiento reducido no hace nada.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.brishSharedBounds(key: String, shape: Shape = RoundedCornerShape(28.dp)): Modifier {
    // Desactivado: la navegación usa push/modal estilo iOS y una tarjeta que crece a la vez
    // que la pantalla se desliza producía un doble movimiento.
    if (!SharedTransitionsEnabled) return this
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedScope.current ?: return this
    if (rememberReducedMotion()) return this
    return with(shared) {
        this@brishSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
            boundsTransform = CardToScreen,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(shape)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.brishSharedElement(key: String): Modifier {
    // Desactivado: la navegación usa push/modal estilo iOS y una tarjeta que crece a la vez
    // que la pantalla se desliza producía un doble movimiento.
    if (!SharedTransitionsEnabled) return this
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedScope.current ?: return this
    if (rememberReducedMotion()) return this
    return with(shared) {
        this@brishSharedElement.sharedElement(
            state = rememberSharedContentState(key),
            animatedVisibilityScope = animated
        )
    }
}

private const val SharedTransitionsEnabled = false
