package com.katchy.focuslive.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.AreaHomeBg
import com.katchy.focuslive.ui.theme.AreaPersonalBg
import com.katchy.focuslive.ui.theme.AreaWorkBg
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.rememberReducedMotion

/** Ilustraciones de Brish dibujadas en código: nítidas en cualquier pantalla y sin imágenes pesadas. */
enum class BrishArtKind { Sun, Capture, Calendar, Plant }

private val Paper = Color(0xFFFFFDF9)
private val PaperShade = Color(0xFFEFE7D8)
private val Line = Color(0xFFE0D5C1)
private val Gold = Color(0xFFF2B21E)
private val ShadowWarm = Color(0x33C9962A)

@Composable
fun BrishArt(kind: BrishArtKind, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val float = rememberInfiniteTransition(label = "artFloat")
    val bob by float.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse),
        label = "bob"
    )
    Canvas(modifier) {
        val u = size.minDimension / 100f // unidad: el lienzo mide 100×100 u
        translate(left = (size.width - 100 * u) / 2, top = (size.height - 100 * u) / 2) {
            // Halo cálido de fondo
            drawCircle(
                Brush.radialGradient(listOf(Color(0x66F8D468), Color(0x00F8D468)), center = Offset(50 * u, 50 * u), radius = 52 * u),
                radius = 52 * u,
                center = Offset(50 * u, 50 * u)
            )
            drawOval(ShadowWarm, topLeft = Offset(18 * u, 86 * u), size = Size(64 * u, 8 * u))
            val lift = -3 * u * bob
            when (kind) {
                BrishArtKind.Sun -> sun(u, lift)
                BrishArtKind.Capture -> capture(u, lift)
                BrishArtKind.Calendar -> calendar(u, lift)
                BrishArtKind.Plant -> plant(u, lift)
            }
        }
    }
}

private fun DrawScope.bulbGradient(center: Offset, r: Float) = Brush.radialGradient(
    listOf(Color(0xFFFFF4C2), Color(0xFFFCD34D), Color(0xFFE9A21A)),
    center = center - Offset(r * 0.3f, r * 0.35f),
    radius = r * 1.4f
)

private fun DrawScope.sun(u: Float, lift: Float) {
    // Tarjeta del día con una línea de ahora y un sol saliendo detrás.
    val c = Offset(64 * u, 30 * u + lift)
    for (i in 0 until 8) {
        rotate(i * 45f, pivot = c) {
            drawLine(Gold, c + Offset(0f, -22 * u), c + Offset(0f, -28 * u), strokeWidth = 3.5f * u, cap = StrokeCap.Round)
        }
    }
    drawCircle(bulbGradient(c, 17 * u), radius = 17 * u, center = c)
    rotate(-6f, pivot = Offset(42 * u, 58 * u)) {
        drawRoundRect(Paper, topLeft = Offset(16 * u, 36 * u + lift), size = Size(54 * u, 46 * u), cornerRadius = CornerRadius(9 * u))
        drawRoundRect(PaperShade, topLeft = Offset(16 * u, 36 * u + lift), size = Size(54 * u, 46 * u), cornerRadius = CornerRadius(9 * u), style = Stroke(1.2f * u))
        drawRoundRect(AreaWorkBg, topLeft = Offset(23 * u, 45 * u + lift), size = Size(40 * u, 10 * u), cornerRadius = CornerRadius(4 * u))
        drawRoundRect(AreaHomeBg, topLeft = Offset(23 * u, 59 * u + lift), size = Size(30 * u, 8 * u), cornerRadius = CornerRadius(4 * u))
        drawRoundRect(AreaPersonalBg, topLeft = Offset(23 * u, 70 * u + lift), size = Size(36 * u, 6 * u), cornerRadius = CornerRadius(3 * u))
        drawLine(Color(0xFFE2412B), Offset(20 * u, 57 * u + lift), Offset(66 * u, 57 * u + lift), strokeWidth = 1.6f * u)
        drawCircle(Color(0xFFE2412B), radius = 2.4f * u, center = Offset(20 * u, 57 * u + lift))
    }
}

private fun DrawScope.capture(u: Float, lift: Float) {
    rotate(-10f, pivot = Offset(42 * u, 58 * u)) {
        drawRoundRect(Paper, topLeft = Offset(20 * u, 28 * u), size = Size(48 * u, 58 * u), cornerRadius = CornerRadius(8 * u))
        drawRoundRect(Line, topLeft = Offset(20 * u, 28 * u), size = Size(48 * u, 58 * u), cornerRadius = CornerRadius(8 * u), style = Stroke(1.2f * u))
        listOf(38f to 30f, 48f to 36f, 58f to 26f, 68f to 32f).forEach { (y, w) ->
            drawRoundRect(Line, topLeft = Offset(28 * u, y * u), size = Size(w * u, 4 * u), cornerRadius = CornerRadius(2 * u))
        }
    }
    // Lápiz
    rotate(32f, pivot = Offset(76 * u, 62 * u)) {
        drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFF6C343), Color(0xFFE3A21C))), topLeft = Offset(71 * u, 36 * u), size = Size(10 * u, 42 * u), cornerRadius = CornerRadius(2 * u))
        drawRoundRect(Color(0xFFE8A8A0), topLeft = Offset(71 * u, 32 * u), size = Size(10 * u, 6 * u), cornerRadius = CornerRadius(2 * u))
        val tip = Path().apply { moveTo(71 * u, 78 * u); lineTo(81 * u, 78 * u); lineTo(76 * u, 88 * u); close() }
        drawPath(tip, Color(0xFFF3D9A7))
        drawCircle(BrishInk, radius = 1.4f * u, center = Offset(76 * u, 86.5f * u))
    }
    // Foco
    val c = Offset(70 * u, 22 * u + lift)
    drawCircle(bulbGradient(c, 13 * u), radius = 13 * u, center = c)
    drawRoundRect(Color(0xFFB9AE9C), topLeft = Offset(65 * u, 33 * u + lift), size = Size(10 * u, 7 * u), cornerRadius = CornerRadius(2 * u))
    listOf(Offset(86f, 12f) to Offset(91f, 9f), Offset(88f, 22f) to Offset(94f, 22f), Offset(80f, 4f) to Offset(83f, 0f)).forEach { (a, b) ->
        drawLine(Gold, a * u + Offset(0f, lift), b * u + Offset(0f, lift), strokeWidth = 2.6f * u, cap = StrokeCap.Round)
    }
}

private fun DrawScope.calendar(u: Float, lift: Float) {
    val top = 22 * u + lift
    drawRoundRect(Paper, topLeft = Offset(14 * u, top), size = Size(72 * u, 62 * u), cornerRadius = CornerRadius(10 * u))
    drawRoundRect(Color(0xFFF8D468), topLeft = Offset(14 * u, top), size = Size(72 * u, 15 * u), cornerRadius = CornerRadius(10 * u, 10 * u))
    drawRect(Color(0xFFF8D468), topLeft = Offset(14 * u, top + 8 * u), size = Size(72 * u, 7 * u))
    listOf(30f, 50f, 70f).forEach { x ->
        drawRoundRect(BrishInk, topLeft = Offset((x - 2) * u, top - 5 * u), size = Size(4 * u, 10 * u), cornerRadius = CornerRadius(2 * u))
    }
    val cols = listOf(AreaWorkBg, AreaHomeBg, AreaPersonalBg, Color(0xFFDDF1E5))
    for (i in 0 until 4) {
        val x = (20 + i * 16) * u
        drawRoundRect(cols[i], topLeft = Offset(x, top + 22 * u + (i % 2) * 6 * u), size = Size(12 * u, (18 + (i % 3) * 6) * u), cornerRadius = CornerRadius(3 * u))
    }
    drawRoundRect(Line, topLeft = Offset(14 * u, top), size = Size(72 * u, 62 * u), cornerRadius = CornerRadius(10 * u), style = Stroke(1.2f * u))
    // Check verde
    val c = Offset(80 * u, 78 * u + lift)
    drawCircle(AreaHealth, radius = 10 * u, center = c)
    val check = Path().apply { moveTo(c.x - 4.5f * u, c.y); lineTo(c.x - 1 * u, c.y + 3.5f * u); lineTo(c.x + 5 * u, c.y - 3.5f * u) }
    drawPath(check, Color.White, style = Stroke(2.6f * u, cap = StrokeCap.Round))
}

private fun DrawScope.plant(u: Float, lift: Float) {
    val leaf = Brush.linearGradient(listOf(Color(0xFF7BCB83), Color(0xFF2F8A4A)))
    drawLine(Color(0xFF2F8A4A), Offset(50 * u, 66 * u), Offset(50 * u, 34 * u + lift), strokeWidth = 3 * u, cap = StrokeCap.Round)
    val left = Path().apply {
        moveTo(50 * u, 50 * u + lift)
        cubicTo(32 * u, 50 * u + lift, 22 * u, 38 * u + lift, 24 * u, 24 * u + lift)
        cubicTo(40 * u, 24 * u + lift, 50 * u, 34 * u + lift, 50 * u, 50 * u + lift)
    }
    val right = Path().apply {
        moveTo(50 * u, 42 * u + lift)
        cubicTo(54 * u, 24 * u + lift, 68 * u, 16 * u + lift, 82 * u, 18 * u + lift)
        cubicTo(82 * u, 34 * u + lift, 68 * u, 44 * u + lift, 50 * u, 42 * u + lift)
    }
    drawPath(left, leaf)
    drawPath(right, leaf)
    val pot = Path().apply { moveTo(26 * u, 66 * u); lineTo(74 * u, 66 * u); lineTo(68 * u, 90 * u); lineTo(32 * u, 90 * u); close() }
    drawPath(pot, Brush.verticalGradient(listOf(Color(0xFFF3A774), Color(0xFFD9763F)), startY = 66 * u, endY = 90 * u))
    drawRoundRect(Color(0xFFE58A52), topLeft = Offset(22 * u, 62 * u), size = Size(56 * u, 9 * u), cornerRadius = CornerRadius(4 * u))
}
