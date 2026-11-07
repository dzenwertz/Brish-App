package com.katchy.focuslive.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katchy.focuslive.ui.components.BrishPrimaryButton
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishScrimDeep
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.rememberReducedMotion

private data class GuideStep(val title: String, val body: String)

private val GuideSteps = listOf(
    GuideStep("Desliza entre pestañas", "Mueve el dedo hacia los lados para pasar de Hoy a Agenda, Notas y Hábitos."),
    GuideStep("Toca ＋ para capturar", "Escribe como hablas: «pagar la luz el viernes 6pm». Brish lo ordena por ti."),
    GuideStep("Funciona sin conexión", "Todo se guarda en tu teléfono al instante y se sincroniza cuando vuelve internet.")
)

/** Guía de tres pasos que se muestra una vez, sobre las pestañas y encima de la barra. */
@Composable
fun BrishGuide(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val reduced = rememberReducedMotion()
    val motion = rememberInfiniteTransition(label = "guide")
    val swipe by motion.animateFloat(-1f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse), label = "swipe")
    val pulse by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "pulse")
    BackHandler { onFinish() }

    Box(
        modifier
            .fillMaxSize()
            .background(BrishScrimDeep)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        if (step == 0) {
            Icon(
                Icons.Rounded.TouchApp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .graphicsLayer { if (!reduced) translationX = swipe * 70.dp.toPx() }
            )
        }
        if (step == 1) {
            // Anillo que late sobre el botón ＋ de la barra.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 30.dp)
                    .size(76.dp)
                    .graphicsLayer {
                        val s = if (reduced) 1f else 1f + 0.35f * pulse
                        scaleX = s; scaleY = s
                        alpha = if (reduced) 1f else 1f - pulse
                    }
                    .border(3.dp, BrishYellow, CircleShape)
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 128.dp)
                .fillMaxWidth()
                .brishCard(BrishShapes.hero)
                .padding(20.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn(tween(220))) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut(tween(150)))
                },
                label = "guideStep"
            ) { s ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (s == 2) Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Text(GuideSteps[s].title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(GuideSteps[s].body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GuideSteps.indices.forEach { i ->
                        Box(
                            Modifier.width(if (i == step) 22.dp else 8.dp).height(8.dp).clip(CircleShape)
                                .background(if (i == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
                Text(
                    "Omitir",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onFinish).padding(horizontal = 12.dp, vertical = 10.dp)
                )
                BrishPrimaryButton(
                    text = if (step == GuideSteps.lastIndex) "Entendido" else "Siguiente",
                    highlighted = step == GuideSteps.lastIndex,
                    height = 44.dp,
                    onClick = { if (step == GuideSteps.lastIndex) onFinish() else step++ }
                )
            }
        }
    }
}
