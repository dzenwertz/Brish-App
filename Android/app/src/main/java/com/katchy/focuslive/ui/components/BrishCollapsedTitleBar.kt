package com.katchy.focuslive.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katchy.focuslive.ui.theme.isDarkTheme

/**
 * Barra de título plegada (como la navegación de iOS): cuando el título grande sale de la
 * pantalla al hacer scroll, aparece arriba el título pequeño centrado con una línea finísima.
 * Va encima de la lista, alineada arriba; el título grande debe ser el primer elemento.
 */
@Composable
fun BrishCollapsedTitleBar(title: String, listState: LazyListState, modifier: Modifier = Modifier) {
    val threshold = with(LocalDensity.current) { 20.dp.roundToPx() } // en cuanto el título grande empieza a esconderse
    val collapsed by remember(listState, threshold) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > threshold }
    }
    val alpha = animateFloatAsState(if (collapsed) 1f else 0f, tween(160), label = "collapsedTitle")
    if (!collapsed && alpha.value == 0f) return
    val dark = isDarkTheme()
    val barColor = MaterialTheme.colorScheme.background.copy(alpha = 0.97f)
    Column(
        modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha.value }
            // También cubre la franja de la barra de estado, para que no asome el título grande por arriba.
            .drawBehind { drawRect(barColor, topLeft = androidx.compose.ui.geometry.Offset(0f, -size.height), size = androidx.compose.ui.geometry.Size(size.width, size.height * 2)) }
    ) {
        Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier.semantics { heading() }
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f))
        )
    }
}
