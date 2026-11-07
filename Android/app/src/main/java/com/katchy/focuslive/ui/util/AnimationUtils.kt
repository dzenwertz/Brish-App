package com.katchy.focuslive.ui.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.PointerEventPass
import com.katchy.focuslive.ui.theme.MotionTokens

/**
 * A custom modifier that adds an Apple-style "spring bounce" scaling animation
 * when the component is pressed.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f,
    onClick: () -> Unit
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Estilo iOS: al pulsar se atenúa al instante y al soltar vuelve con un fundido corto (sin rebote).
    val alpha = animateFloatAsState(
        targetValue = if (isPressed) PressedAlpha else 1f,
        animationSpec = if (isPressed) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(180),
        label = "press"
    )

    this
        .graphicsLayer { this.alpha = alpha.value }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Remove default Android ripple effect for a cleaner iOS feel
            onClick = onClick
        )
}

/**
 * A custom modifier that adds an Apple-style "spring bounce" scaling animation
 * when the component is pressed (without handling the click internally).
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f
) = composed {
    var isPressed by remember { mutableStateOf(false) }
    
    val alpha = animateFloatAsState(
        targetValue = if (isPressed) PressedAlpha else 1f,
        animationSpec = if (isPressed) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(180),
        label = "press"
    )

    this
        .graphicsLayer { this.alpha = alpha.value }
        .pointerInput(Unit) {
            try {
                awaitPointerEventScope {
                    while (true) {
                        try {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            isPressed = true
                            waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        } catch (e: Exception) {
                            isPressed = false
                            if (e is kotlinx.coroutines.CancellationException) {
                                throw e
                            }
                            break
                        } finally {
                            isPressed = false
                        }
                    }
                }
            } catch (e: Exception) {
                isPressed = false
            }
        }
}

/** Opacidad al pulsar (como los botones y filas de iOS). */
private const val PressedAlpha = 0.55f
