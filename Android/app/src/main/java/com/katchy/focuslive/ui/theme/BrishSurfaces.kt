package com.katchy.focuslive.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Formas y profundidad de Brish 2.0 (lienzo «Nuevo Brish», tablero Motion y efectos). */
object BrishShapes {
    val card = RoundedCornerShape(26.dp)
    val hero = RoundedCornerShape(30.dp)
    val pill = RoundedCornerShape(50)
    val bar = RoundedCornerShape(32.dp)
}

private val ShadowTone = Color(0xFF46341A)

/**
 * Sombra suave en dos capas: una nítida de contacto y otra difusa.
 * Los colores de sombra solo se aplican desde Android 9; antes queda la sombra gris del sistema.
 */
fun Modifier.brishShadow(
    shape: Shape,
    elevation: Dp = 14.dp,
    tone: Color = ShadowTone,
    strength: Float = 1f
): Modifier = this
    .shadow(1.dp, shape, clip = false, ambientColor = tone.copy(alpha = 0.10f * strength), spotColor = tone.copy(alpha = 0.10f * strength))
    .shadow(elevation, shape, clip = false, ambientColor = tone.copy(alpha = 0.10f * strength), spotColor = tone.copy(alpha = 0.16f * strength))

/** Tarjeta estándar: crema claro con sombra en capas. */
@Composable
fun Modifier.brishCard(shape: Shape = BrishShapes.card): Modifier {
    val dark = isDarkTheme()
    return if (dark) {
        this.background(BrishNightCard, shape)
    } else {
        this.brishShadow(shape).background(BrishCard, shape)
    }
}

/** Degradado de la superficie destacada de cada pantalla (tarjeta Ahora, fijadas, resumen). */
val BrishHeroBrush: Brush = Brush.linearGradient(
    colors = listOf(BrishYellowLight, Color(0xFFF7D064)),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

/** Degradado de los botones amarillos (Guardar, Pausa). */
val BrishButtonBrush: Brush = Brush.verticalGradient(listOf(Color(0xFFFADD7E), BrishYellowDeep))

/** Línea de luz interior en el borde superior: da sensación de material. */
fun Modifier.brishEdgeHighlight(alpha: Float = 0.7f, inset: Dp = 18.dp): Modifier = drawWithContent {
    drawContent()
    val x = inset.toPx()
    drawLine(
        color = Color.White.copy(alpha = alpha),
        start = Offset(x, 0.5f),
        end = Offset(size.width - x, 0.5f),
        strokeWidth = 1.dp.toPx()
    )
}

/** Perfiles de movimiento: Suave (tarjetas), Vivo (presión y checks), Pantalla (hojas y páginas). */
object BrishMotion {
    fun <T> soft() = spring<T>(dampingRatio = 0.85f, stiffness = 380f)
    fun <T> lively() = spring<T>(dampingRatio = 0.7f, stiffness = 600f)
    fun <T> screen() = spring<T>(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)
    const val PressScale = 0.96f
}

/** True cuando el usuario desactivó las animaciones del sistema. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
