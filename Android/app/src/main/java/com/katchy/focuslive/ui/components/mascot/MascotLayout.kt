package com.katchy.focuslive.ui.components.mascot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Normaliza el canvas a una unidad virtual `unit` que escala con el tamaño real.
 *
 * Convención: el canvas de cada mascota está pensado como una grilla de ~100 u
 * (ancho) x ~120 u (alto). Toda coordenada se dibuja como `center.x + k * unit`,
 * `center.y + k * unit`, donde `k` es un factor constante por parte del cuerpo.
 * Eso elimina el uso de `.dp.toPx()` hardcoded que deformaba las mascotas en
 * pantallas distintas.
 */
data class MascotLayout(
    val unit: Float,
    val center: Offset,
    val width: Float,
    val height: Float
)

/**
 * Extensión de DrawScope que calcula el [MascotLayout] a partir del tamaño real
 * del canvas. `unit` se deriva de la dimensión menor para que la mascota conserve
 * proporciones y nunca se recorte sin importar el aspect ratio del contenedor.
 */
val DrawScope.mascotLayout: MascotLayout
    get() {
        val w = size.width
        val h = size.height
        val u = minOf(w, h) / 100f
        return MascotLayout(
            unit = u,
            center = Offset(w * 0.5f, h * 0.5f),
            width = w,
            height = h
        )
    }