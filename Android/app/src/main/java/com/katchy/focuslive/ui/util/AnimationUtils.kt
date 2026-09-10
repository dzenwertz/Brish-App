package com.katchy.focuslive.ui.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = 1200f
        ),
        label = "bounce"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
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
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = 1200f
        ),
        label = "bounce"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
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

