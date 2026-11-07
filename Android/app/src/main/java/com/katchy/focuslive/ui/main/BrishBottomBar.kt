package com.katchy.focuslive.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.tween
import com.katchy.focuslive.ui.theme.MotionTokens
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.animation.core.Animatable
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katchy.focuslive.ui.navigation.Screen
import com.katchy.focuslive.ui.theme.BrishCard
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishNightCard
import com.katchy.focuslive.ui.theme.BrishNightText
import com.katchy.focuslive.ui.theme.BrishNightTextSecondary
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishTextTertiary
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.BrishYellowSoft
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.isDarkTheme
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private val BarHeight = 76.dp
private val CaptureSlot = 70.dp
private val IndicatorWidth = 88.dp
private val IndicatorStretch = 12.dp

/** Alto que ocupa la barra flotante, incluido su margen inferior. */
val BrishBottomBarSpace = BarHeight + 28.dp

/** Espacio inferior que las pestañas dejan libre para que su contenido pase por debajo de la barra. */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

/**
 * Barra inferior flotante. El indicador amarillo sigue la posición del pager mientras el dedo
 * arrastra y se estira entre pestañas; se lee en la fase de layout para no recomponer cada frame.
 */
@Composable
fun BrishBottomBar(
    tabs: List<Screen>,
    pagerState: PagerState,
    onTabClick: (Int) -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier,
    captureOpen: Boolean = false,
    hazeState: HazeState? = null
) {
    val dark = isDarkTheme()
    val haptics = LocalHapticFeedback.current
    val activeIndex by remember(pagerState) {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .roundToInt().coerceIn(0, tabs.lastIndex)
        }
    }

    // La superficie (sombra, recorte y desenfoque) va en su propia capa: si se recortara la barra
    // entera, el ＋ que sobresale por arriba quedaría cortado.
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(BarHeight)) {
        Box(
            Modifier
            .matchParentSize()
            .brishShadow(BrishShapes.bar, elevation = 18.dp, strength = if (dark) 0f else 1f)
            .clip(BrishShapes.bar)
            .then(
                if (hazeState != null) {
                    val base = if (dark) BrishNightCard else BrishCard
                    Modifier.hazeChild(
                        hazeState,
                        HazeStyle(
                            backgroundColor = base,
                            tints = listOf(HazeTint(base.copy(alpha = 0.72f))),
                            blurRadius = 22.dp,
                            noiseFactor = 0.03f,
                            fallbackTint = HazeTint(base.copy(alpha = 0.96f))
                        )
                    )
                } else Modifier.background((if (dark) BrishNightCard else BrishCard).copy(alpha = 0.96f))
            )
            .brishEdgeHighlight(alpha = if (dark) 0.06f else 0.9f, inset = 28.dp)
        )
        val tabWidth = (maxWidth - CaptureSlot) / tabs.size
        // Centro de cada pestaña; las dos de la derecha quedan desplazadas por el hueco del ＋.
        val centers = tabs.indices.map { i ->
            tabWidth * i + tabWidth / 2 + if (i >= tabs.size / 2) CaptureSlot else 0.dp
        }

        Box(
            Modifier
                .padding(top = 10.dp)
                .height(32.dp)
                .layout { measurable, constraints ->
                    val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                        .coerceIn(0f, tabs.lastIndex.toFloat())
                    val from = floor(position).toInt().coerceAtMost(tabs.lastIndex)
                    val to = (from + 1).coerceAtMost(tabs.lastIndex)
                    val fraction = position - from
                    val center = centers[from].toPx() + (centers[to].toPx() - centers[from].toPx()) * fraction
                    val width = IndicatorWidth.toPx() + IndicatorStretch.toPx() * sin(PI * fraction).toFloat()
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = width.roundToInt(), maxWidth = width.roundToInt())
                    )
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.place(IntOffset((center - width / 2).roundToInt(), 0))
                    }
                }
                .clip(RoundedCornerShape(16.dp))
                .background(BrishYellowSoft)
        )

        Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, screen ->
                if (index == tabs.size / 2) {
                    CaptureButton(
                        open = captureOpen,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCaptureClick()
                        },
                        modifier = Modifier.width(CaptureSlot)
                    )
                }
                val selected = index == activeIndex
                val reduced = rememberReducedMotion()
                val tabInteraction = remember { MutableInteractionSource() }
                val tabPressed by tabInteraction.collectIsPressedAsState()
                var tapBounceTrigger by remember { mutableIntStateOf(0) }
                var wasSelected by remember { mutableStateOf(selected) }
                val bounceScale = remember { Animatable(1f) }
                LaunchedEffect(selected, tapBounceTrigger) {
                    val becameSelected = selected && !wasSelected
                    wasSelected = selected
                    if ((becameSelected || tapBounceTrigger > 0) && !reduced) {
                        bounceScale.snapTo(0.82f)
                        bounceScale.animateTo(1.18f, tween(140, easing = MotionTokens.overshoot))
                        bounceScale.animateTo(1f, MotionTokens.pressSpring())
                    } else {
                        bounceScale.snapTo(1f)
                    }
                }
                val pressScale by animateFloatAsState(
                    targetValue = when {
                        tabPressed -> 0.92f
                        selected -> 1f
                        else -> 0.94f
                    },
                    animationSpec = MotionTokens.pressSpring(),
                    label = "tabIconScale"
                )
                val iconScale = pressScale * bounceScale.value
                val color by animateColorAsState(
                    targetValue = when {
                        selected -> BrishInk
                        dark -> BrishNightTextSecondary
                        else -> BrishTextTertiary
                    },
                    label = "tabColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = tabInteraction,
                            indication = null,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tapBounceTrigger++
                                onTabClick(index)
                            }
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Row(
                        Modifier.padding(top = 10.dp).height(36.dp).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            screen.icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp).graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                        )
                        AnimatedVisibility(
                            visible = selected,
                            enter = fadeIn(tween(MotionTokens.microMs)) + expandHorizontally(MotionTokens.sheetSpring()),
                            exit = fadeOut(tween(MotionTokens.tabExitMs)) + shrinkHorizontally(MotionTokens.sheetSpring())
                        ) {
                            Text(
                                text = screen.title,
                                color = if (dark) BrishNightText else BrishInk,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureButton(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, BrishMotion.lively(), label = "captureScale")
    val rotation by animateFloatAsState(if (open) 45f else 0f, BrishMotion.lively(), label = "captureRotation")
    Box(modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .offset(y = (-16).dp)
                .size(58.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .brishShadow(CircleShape, elevation = 16.dp, tone = BrishInk, strength = if (pressed) 1.6f else 1.2f)
                .clip(CircleShape)
                .background(BrishInk)
                .semantics { contentDescription = "Capturar" }
                .selectable(
                    selected = open,
                    role = Role.Button,
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = rotation }
            )
        }
    }
}
