package com.katchy.focuslive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Fondo unificado con el sistema visual Brish 2.0 (crema cálido en claro, carbón en oscuro).
 * Mantiene la firma pública para compatibilidad.
 */
@Composable
fun CosmicBackground(
    modifier: Modifier = Modifier,
    showOrbs: Boolean = true,
    showParticles: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        content = content
    )
}
