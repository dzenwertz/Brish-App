package com.katchy.focuslive.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SmoothDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        var animateTrigger by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            animateTrigger = true
        }
        // Como las alertas de iOS: aparece desde un poco más grande hacia su tamaño, con fundido.
        val scale by animateFloatAsState(
            targetValue = if (animateTrigger) 1f else 1.12f,
            animationSpec = tween(260, easing = com.katchy.focuslive.ui.theme.IosMotion.curve),
            label = "smoothDialogScale"
        )
        val alpha by animateFloatAsState(
            targetValue = if (animateTrigger) 1f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "smoothDialogAlpha"
        )

        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        ) {
            content()
        }
    }
}
