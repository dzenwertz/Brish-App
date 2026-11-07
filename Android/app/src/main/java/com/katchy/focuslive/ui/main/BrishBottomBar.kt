package com.katchy.focuslive.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katchy.focuslive.ui.navigation.Screen
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.isDarkTheme
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

private val BarHeight = 56.dp
private val CaptureSlot = 64.dp

/** Alto de la barra inferior sin contar la barra de navegación del sistema. */
val BrishBottomBarSpace = BarHeight

/** Espacio inferior que las pestañas dejan libre para que su contenido no quede debajo de la barra. */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

/**
 * Barra inferior fija, a todo el ancho y opaca (estilo iOS): icono con etiqueta corta debajo,
 * pestaña activa en el color de acento y el ＋ de captura en el centro. Nada pasa por debajo de ella.
 */
@Composable
fun BrishBottomBar(
    tabs: List<Screen>,
    pagerState: PagerState,
    onTabClick: (Int) -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier,
    captureOpen: Boolean = false,
    @Suppress("UNUSED_PARAMETER") hazeState: HazeState? = null
) {
    val dark = isDarkTheme()
    val haptics = LocalHapticFeedback.current
    val activeIndex by remember(pagerState) {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .roundToInt().coerceIn(0, tabs.lastIndex)
        }
    }
    val accent = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Línea fina superior en lugar de sombra.
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f))
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                val color by animateColorAsState(if (selected) accent else inactive, label = "tabColor")
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabClick(index)
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    com.katchy.focuslive.ui.components.BrishGlyph(screen.icon, color, 25.dp) // set de iconos propio (SVG)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = screen.title,
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
        // Debajo de la barra solo queda el área de gestos del sistema, con el mismo fondo.
        Spacer(Modifier.fillMaxWidth().navigationBarsPadding())
    }
}

@Composable
private fun CaptureButton(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, BrishMotion.lively(), label = "captureScale")
    val rotation by animateFloatAsState(if (open) 45f else 0f, BrishMotion.soft(), label = "captureRotation")
    Box(modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
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
                modifier = Modifier.size(26.dp).graphicsLayer { rotationZ = rotation }
            )
        }
    }
}
