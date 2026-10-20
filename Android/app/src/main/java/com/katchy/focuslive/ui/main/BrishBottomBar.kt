package com.katchy.focuslive.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.isDarkTheme
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private val BarHeight = 76.dp
private val CaptureSlot = 70.dp
private val IndicatorWidth = 44.dp
private val IndicatorStretch = 28.dp

/** Alto que ocupa la barra flotante, incluido su margen inferior. */
val BrishBottomBarSpace = BarHeight + 28.dp

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
    captureOpen: Boolean = false
) {
    val dark = isDarkTheme()
    val haptics = LocalHapticFeedback.current
    val activeIndex by remember(pagerState) {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .roundToInt().coerceIn(0, tabs.lastIndex)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .brishShadow(BrishShapes.bar, elevation = 18.dp, strength = if (dark) 0f else 1f)
            .clip(BrishShapes.bar)
            .background((if (dark) BrishNightCard else BrishCard).copy(alpha = 0.96f))
            .brishEdgeHighlight(alpha = if (dark) 0.06f else 0.9f, inset = 28.dp)
    ) {
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
                .background(BrishYellow)
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
                val color by animateColorAsState(
                    targetValue = when {
                        selected -> BrishInk
                        dark -> BrishNightTextSecondary
                        else -> BrishTextTertiary
                    },
                    label = "tabColor"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabClick(index) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Box(Modifier.padding(top = 10.dp).height(32.dp), contentAlignment = Alignment.Center) {
                        Icon(screen.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        text = screen.title,
                        color = if (selected) (if (dark) BrishNightText else BrishInk) else color,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 3.dp)
                    )
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
