package com.katchy.focuslive.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.runtime.ReadOnlyComposable
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
    val card = RoundedCornerShape(16.dp)
    val hero = RoundedCornerShape(18.dp)
    val pill = RoundedCornerShape(50)
    val button = RoundedCornerShape(14.dp)
    val bar = RoundedCornerShape(0.dp)
}

private val ShadowTone = BrishShadowTone

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
    // Estilo minimalista: una sola sombra muy suave y más corta que antes.
    .shadow(elevation * 0.5f, shape, clip = false, ambientColor = tone.copy(alpha = 0.04f * strength), spotColor = tone.copy(alpha = 0.08f * strength))

/** Tarjeta estándar: superficie plana sobre el fondo agrupado, sin sombra (como las listas de iOS). */
@Composable
fun Modifier.brishCard(shape: Shape = BrishShapes.card): Modifier {
    val dark = isDarkTheme()
    return this.background(if (dark) BrishNightCard else BrishCard, shape)
}

/** Degradado de la superficie destacada de cada pantalla (tarjeta Ahora, fijadas, resumen). */
val BrishHeroBrush: Brush
    @Composable @ReadOnlyComposable get() = SolidColor(MaterialTheme.colorScheme.surface) // plana; se adapta al tema

/** Degradado de los botones amarillos (Guardar, Pausa). */
val BrishButtonBrush: Brush
    @Composable @ReadOnlyComposable get() = SolidColor(MaterialTheme.colorScheme.primary)

/** Línea de luz interior en el borde superior: da sensación de material. */
@Suppress("UNUSED_PARAMETER")
fun Modifier.brishEdgeHighlight(alpha: Float = 0.7f, inset: Dp = 18.dp): Modifier = this // Plano: sin brillo decorativo

/** Perfiles de movimiento: Suave (tarjetas), Vivo (presión y checks), Pantalla (hojas y páginas). */
object BrishMotion {
    fun <T> soft() = MotionTokens.cardSpring<T>()
    fun <T> lively() = MotionTokens.pressSpring<T>()
    fun <T> screen() = MotionTokens.sheetSpring<T>()
    const val PressScale = MotionTokens.pressScale
}

/** True cuando el usuario desactivó las animaciones del sistema. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f ||
            context.getSharedPreferences("brish_accessibility", android.content.Context.MODE_PRIVATE)
                .getBoolean("reduce_motion", false)
    }
}
