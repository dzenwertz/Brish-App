package com.katchy.focuslive.ui.components.mascot

import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import com.katchy.focuslive.ui.components.BrishPose

/**
 * Renderer de POPPIN (pingüino coach tóxico).
 *
 * Versión redesign: coordenadas relativas (MascotLayout.unit), parpadeo en AMBOS ojos,
 * sincronización pico↔silbato, squash & stretch al saltar, micro-animación de
 * "cejas levantadas" cuando silba, "settle" del accesorio con transición y
 * alpha variable en líneas de aire. Recibe todos los flujos de animación
 * existentes en BrishMascot.kt para que la migración no rompa el resto de mascotas.
 *
 * Parámetros de animación:
 * @param bodyOffset     combinación (breathe + bob) en píxeles → se transla a "u".
 * @param breathe        valor de respiración (0..2) — factor de escala sutil.
 * @param jump           salto de entrada (-20..0 en píxeles relativos originales).
 * @param handWave       rotación de saludo (0..-130).
 * @param eyeScale       factor de parpadeo cíclico (1=abierto, 0.1=cerrado).
 * @param blinkExtra     factor extra de "blink aleatorio" (0..1, 1=cerrado).
 * @param beakOpen       valor base de apertura del pico (idle munch).
 * @param armRotation    rotación de brazos (idle/wave/watering).
 * @param whistleScale   0..10 — escala del silbato (DEFAULT).
 * @param tickRotation   0..360 —aguja del cronómetro (HABITS).
 * @param pupilOffsetX   offset normalizado [-1, 1] de la pupila en X (follow-pointer).
 * @param pupilOffsetY   offset normalizado [-1, 1] de la pupila en Y (follow-pointer).
 * @param drawLevelAura  si se debe renderizar el aura de nivel (level ≥ 10).
 */
@Suppress("LongParameterList", "CyclomaticComplexMethod", "LongMethod")
fun DrawScope.drawPoppin(
    pose: BrishPose,
    mainColor: Color,
    secondaryColor: Color,
    level: Int,
    isPremiumCrown: Boolean,
    bodyOffset: Float,
    breathe: Float,
    jump: Float,
    handWave: Float,
    eyeScale: Float,
    blinkExtra: Float,
    beakOpen: Float,
    armRotation: Float,
    whistleScale: Float,
    tickRotation: Float,
    pupilOffsetX: Float,
    pupilOffsetY: Float,
    drawLevelAura: Boolean,
    eyePopScale: Float = 1f
) {
    val layout = mascotLayout
    val u = layout.unit
    val cx = layout.center.x
    // Centro vertical base: ajustado para que toda la mascota quede dentro del canvas.
    val cy = layout.center.y

    // --- FACTORES DE ANIMACIÓN COMBINADOS ---
    val bob: Float = bodyOffset.coerceIn(-30f, 30f) * (u / 3f) // traduce píxeles→u (u≈3px@xxhdpi)
    val jumpU: Float = jump.coerceIn(-20f, 5f) * (u / 3f)

    // Squash & stretch: al saltar hacia arriba (jump<0) el cuerpo se estira (Y++)
    // y se comprime (X--). Al caer.goBack, inverso. Es el "secondario" del salto.
    val jumpNorm = (jumpU / (10f * u)).coerceIn(-1.5f, 0.5f)
    val squashY = 1f - 0.08f * jumpNorm     // jump≥0 → squashY cae a 0.96
    val stretchX = 1f + 0.05f * jumpNorm    // jump<0  → stretchX sube a 1.075
    val breatheScale = 1f + (breathe * 0.008f)

    val footColor = Color(0xFFF59E0B)
    val accessoryRed = Color(0xFFEF4444)

    // --- GROUND SHADOW ---
    val shadowScale = (1f + (jumpU / (40f * u))).coerceIn(0.4f, 1f)
    val shadowAlpha = (0.15f * shadowScale).coerceIn(0.05f, 0.15f)
    drawOval(
        color = Color.Black.copy(alpha = shadowAlpha),
        topLeft = Offset(cx - 24f * u * shadowScale, cy + 33f * u),
        size = Size(48f * u * shadowScale, 6f * u * shadowScale)
    )

    // --- AURA (nivel oro/plata) ---
    if (drawLevelAura) {
        val aura = if (level >= 20) Color(0xFFFFD700).copy(alpha = 0.3f)
                   else Color(0xFFC0C0C0).copy(alpha = 0.3f)
        drawCircle(aura, radius = 60f * u, center = layout.center)
    }

    // --- FEET ---
    // Reacción al salto: cuando salta (jump<0), pies se "pegan" al cuerpo (pegan abajo).
    val feetOffset = if (jump < 0) jump * 0.4f else 0f
    val feetY = cy + 22f * u + bob - (feetOffset * (u / 3f))
    drawOval(footColor, topLeft = Offset(cx - 18f * u, feetY), size = Size(15f * u, 10f * u))
    drawOval(footColor, topLeft = Offset(cx + 3f * u, feetY), size = Size(15f * u, 10f * u))

    // --- BODY con squash & stretch + breathe ---
    val bodyW = (50f * u * stretchX * breatheScale).coerceAtLeast(1f)
    val bodyH = (45f * u * squashY * breatheScale).coerceAtLeast(1f)
    val bodyTopLeft = Offset(cx - bodyW / 2f, cy - 20f * u + bob)
    drawOval(mainColor, topLeft = bodyTopLeft, size = Size(bodyW, bodyH))

    // --- BELLY ---
    val bellyW = (36f * u * stretchX).coerceAtLeast(1f)
    val bellyH = (28f * u * squashY).coerceAtLeast(1f)
    drawOval(
        secondaryColor,
        topLeft = Offset(cx - bellyW / 2f, cy - 10f * u + bob),
        size = Size(bellyW, bellyH)
    )

    // --- HEAD ---
    val headR = (18f * u * breatheScale).coerceAtLeast(1f)
    val headCy = cy - 35f * u + bob
    drawCircle(mainColor, radius = headR, center = Offset(cx, headCy))

    // --- CUTE CHEEKS BLUSH ---
    drawCircle(Color(0xFFFDA4AF).copy(alpha = 0.35f), radius = 2.5f * u, center = Offset(cx - 12f * u, headCy + 5f * u))
    drawCircle(Color(0xFFFDA4AF).copy(alpha = 0.35f), radius = 2.5f * u, center = Offset(cx + 12f * u, headCy + 5f * u))

    // --- HEADWEAR ---
    when (pose) {
        BrishPose.HABITS -> {
            // Coach Headband (arc)
            drawArc(
                color = accessoryRed,
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(cx - 18f * u, headCy - 22f * u),
                size = Size(36f * u, 30f * u),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f * u)
            )
        }
        BrishPose.FOCUS -> {
            // Drill Sergeant Hat
            val hatColor = Color(0xFF111827)
            drawOval(hatColor, topLeft = Offset(cx - 22f * u, headCy - 28f * u), size = Size(44f * u, 10f * u)) // Brim
            drawRect(hatColor, topLeft = Offset(cx - 12f * u, headCy - 38f * u), size = Size(24f * u, 10f * u)) // Top
            drawOval(Color(0xFFF59E0B), topLeft = Offset(cx - 2.5f * u, headCy - 35f * u), size = Size(5f * u, 5f * u)) // Badge
        }
        BrishPose.DEFAULT -> {
            // Classic Bandana — mismo path original re-escalado
            val bandanaPath = Path().apply {
                moveTo(cx - 18f * u, headCy - 18f * u)
                quadraticTo(cx, headCy - 26f * u, cx + 18f * u, headCy - 18f * u)
                lineTo(cx + 18f * u, headCy - 12f * u)
                quadraticTo(cx, headCy - 20f * u, cx - 18f * u, headCy - 12f * u)
                close()
            }
            drawPath(path = bandanaPath, color = accessoryRed)
        }
        else -> {} // Naked head
    }

    // --- EYES: AMBOS parpadean ahora (bug fix) ---
    // Combina parpadeo cíclico (eyeScale) + blink aleatorio (blinkExtra): un solo factor.
    val eyeOpenFactor = (eyeOpenCombined(eyeScale, blinkExtra)).coerceIn(0.05f, 1f)
    val eyeR = 5f * u
    val eyeY = headCy - 1f * u
    val leftEyeCx = cx - 6f * u
    val rightEyeCx = cx + 6f * u

    // Esclerótica — alto oval *"squash" del blink, ancho se queda (párpados solo cierran vertical)
    val eyeH = (10f * u) * eyeOpenFactor * eyePopScale
    val eyeW = eyeR * 2f * eyePopScale

    drawOval(Color.White, topLeft = Offset(leftEyeCx - eyeW / 2f, eyeY - eyeH / 2f), size = Size(eyeW, eyeH))
    drawOval(Color.White, topLeft = Offset(rightEyeCx - eyeW / 2f, eyeY - eyeH / 2f), size = Size(eyeW, eyeH))

    // Pupilas: offset follow-pointer (normalizado [-1,1] → ±1.5u) + solo se dibujan si blink no cerró el ojo
    val pupilR = 2f * u * eyePopScale
    val pupilOffX = pupilOffsetX.coerceIn(-1f, 1f) * 1.5f * u
    val pupilOffY = pupilOffsetY.coerceIn(-1f, 1f) * 1.5f * u
    val pupilAlpha = ((eyeOpenFactor - 0.1f) / 0.9f).coerceIn(0f, 1f)

    if (pose == BrishPose.FOCUS) {
        // Pupilas reducidas (intense stare)
        drawCircle(Color.Black, radius = 1f * u * eyePopScale, center = Offset(leftEyeCx + pupilOffX, eyeY + pupilOffY))
        drawCircle(Color.Black, radius = 1f * u * eyePopScale, center = Offset(rightEyeCx + pupilOffX, eyeY + pupilOffY))
    } else if (pose == BrishPose.CHAT) {
        // Guiño derecho + ojo izquierdo normal
        val pupilCenter = Offset(leftEyeCx + pupilOffX, eyeY + pupilOffY)
        drawCircle(Color.Black, radius = pupilR, center = pupilCenter, alpha = pupilAlpha)
        drawOval(Color.Black, topLeft = Offset(rightEyeCx - 3f * u * eyePopScale, eyeY - 1f * u * eyePopScale), size = Size(6f * u * eyePopScale, 2f * u * eyePopScale))
    } else {
        drawCircle(Color.Black, radius = pupilR, center = Offset(leftEyeCx + pupilOffX, eyeY + pupilOffY), alpha = pupilAlpha)
        drawCircle(Color.Black, radius = pupilR, center = Offset(rightEyeCx + pupilOffX, eyeY + pupilOffY), alpha = pupilAlpha)
        // Sparkles (movimiento secundario sutil)
        val sparkleR = 0.7f * u * eyePopScale
        val sparkleOffX = 0.8f * u * eyePopScale
        val sparkleOffY = 0.8f * u * eyePopScale
        drawCircle(Color.White, radius = sparkleR, center = Offset(leftEyeCx + pupilOffX + sparkleOffX, eyeY + pupilOffY - sparkleOffY))
        drawCircle(Color.White, radius = sparkleR, center = Offset(rightEyeCx + pupilOffX + sparkleOffX, eyeY + pupilOffY - sparkleOffY))
    }

    // --- EYEBROWS (Angry/Stern) + "lift" durante silbato (follow-through facial) ---
    val browLift = (whistleScale / 10f) * (-1.5f * u)
    val browY = if (pose == BrishPose.FOCUS) headCy - 8f * u
                else (headCy - 10f * u + browLift)
    drawLine(
        color = Color.Black,
        start = Offset(cx - 12f * u, browY),
        end = Offset(cx - 3f * u, browY + 2f * u),
        strokeWidth = 1.5f * u,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.Black,
        start = Offset(cx + 3f * u, browY + 2f * u),
        end = Offset(cx + 12f * u, browY),
        strokeWidth = 1.5f * u,
        cap = StrokeCap.Round
    )

    // --- BEAK sincronizado con whistle y con comer (armRotation alzándose = comiendo) ---
    val eatingActive = (pose == BrishPose.DEFAULT && armRotation < -10f)
    val beakOpenAmount = when {
        pose != BrishPose.DEFAULT -> 0f
        handWave != 0f -> 0f                // en medio del saludo, no "munch"
        whistleScale > 5f -> (whistleScale / 10f) * 4f * u   // soplando = abre
        eatingActive -> (-armRotation * 0.1f) * u           // brazo del fish sube = pico abre
        else -> beakOpen                     // idle munch original
    }
    val beakPath = Path().apply {
        moveTo(cx - 4f * u, headCy + 6f * u)
        lineTo(cx + 4f * u, headCy + 6f * u)
        lineTo(cx, headCy + 10f * u + beakOpenAmount)
        close()
    }
    drawPath(beakPath, footColor)

    // --- WHISTLE (DEFAULT) con squash al soplar + alpha en líneas de aire ---
    if (pose == BrishPose.DEFAULT) {
        val stringColor = Color(0xFF6B7280)
        drawLine(stringColor, start = Offset(cx - 5f * u, headCy + 12f * u), end = Offset(cx, cy - 5f * u + bob), strokeWidth = 1f * u)
        drawLine(stringColor, start = Offset(cx + 5f * u, headCy + 12f * u), end = Offset(cx, cy - 5f * u + bob), strokeWidth = 1f * u)

        val whY = cy - 5f * u + bob
        // Squash al soplar: el silbato "infla" verticalmente
        val blowSquash = 1f + (whistleScale / 10f) * 0.15f
        drawRoundRect(
            color = accessoryRed,
            topLeft = Offset(cx - 4f * u, whY - 8f * u),
            size = Size(8f * u, 12f * u * blowSquash),
            cornerRadius = CornerRadius(2f * u)
        )
        drawCircle(accessoryRed, radius = 5f * u, center = Offset(cx, whY + 4f * u))

        // Air lines con alpha progresiva (no binario)
        val airA = ((whistleScale - 5f) / 5f).coerceIn(0f, 1f)
        if (airA > 0f) {
            drawLine(Color.Black.copy(alpha = airA), start = Offset(cx - 8f * u, whY), end = Offset(cx - 14f * u, whY - 4f * u), strokeWidth = 1f * u)
            drawLine(Color.Black.copy(alpha = airA), start = Offset(cx + 8f * u, whY), end = Offset(cx + 14f * u, whY - 4f * u), strokeWidth = 1f * u)
        }
    }

    // --- FLIPPERS & PROPS ---
    when (pose) {
        BrishPose.CHAT -> {
            // Crossed wings: draw overlapping curved wings over the chest
            // Left folded arm
            val leftFoldedPath = Path().apply {
                moveTo(cx - 20f * u, cy + bob + 2f * u)
                quadraticTo(cx - 24f * u, cy + bob + 9f * u, cx + 8f * u, cy + bob + 8f * u)
            }
            drawPath(leftFoldedPath, mainColor, style = Stroke(width = 6f * u, cap = StrokeCap.Round))

            // Right folded arm
            val rightFoldedPath = Path().apply {
                moveTo(cx + 20f * u, cy + bob + 2f * u)
                quadraticTo(cx + 24f * u, cy + bob + 7f * u, cx - 8f * u, cy + bob + 6f * u)
            }
            drawPath(rightFoldedPath, mainColor, style = Stroke(width = 6f * u, cap = StrokeCap.Round))
        }
        BrishPose.PLANNER -> {
            // Left flipper: on hip
            val leftWingPath = Path().apply {
                moveTo(cx - 22f * u, cy - 8f * u + bob)
                quadraticTo(cx - 29f * u, cy + 2f * u + bob, cx - 18f * u, cy + 8f * u + bob)
                quadraticTo(cx - 20f * u, cy + 2f * u + bob, cx - 22f * u, cy - 8f * u + bob)
                close()
            }
            drawPath(leftWingPath, mainColor)

            // Clipboard on the right side
            val boardX = cx + 16f * u
            val boardY = cy + bob - 2f * u
            val boardW = 14f * u
            val boardH = 18f * u
            
            // Draw wooden board
            drawRoundRect(
                color = Color(0xFF78350F), // Board wood brown
                topLeft = Offset(boardX, boardY),
                size = Size(boardW, boardH),
                cornerRadius = CornerRadius(1.5f * u)
            )
            // Draw white paper
            drawRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset(boardX + 1.5f * u, boardY + 2.5f * u),
                size = Size(boardW - 3f * u, boardH - 4.5f * u)
            )
            // Draw silver clip
            drawRect(
                color = Color(0xFF94A3B8),
                topLeft = Offset(boardX + boardW / 2f - 3f * u, boardY - 1f * u),
                size = Size(6f * u, 2f * u)
            )
            // Draw tactical X & O playbook details
            drawCircle(Color(0xFFEF4444), radius = 0.8f * u, center = Offset(boardX + 4f * u, boardY + 6f * u)) // Red X
            drawLine(Color(0xFF3B82F6), start = Offset(boardX + 4f * u, boardY + 10f * u), end = Offset(boardX + 10f * u, boardY + 12f * u), strokeWidth = 1f * u)
            drawCircle(Color(0xFF3B82F6), radius = 0.8f * u, center = Offset(boardX + 10f * u, boardY + 12f * u)) // Blue O

            // Right flipper holding the clipboard
            val rightWingPath = Path().apply {
                moveTo(cx + 22f * u, cy - 8f * u + bob)
                quadraticTo(cx + 26f * u, cy + 2f * u + bob, boardX + 2f * u, boardY + 9f * u)
                quadraticTo(cx + 18f * u, cy - 4f * u + bob, cx + 22f * u, cy - 8f * u + bob)
                close()
            }
            drawPath(rightWingPath, mainColor)
        }
        BrishPose.FINANCE -> {
            // Left flipper: normal hanging down
            val leftWingPath = Path().apply {
                moveTo(cx - 22f * u, cy - 8f * u + bob)
                quadraticTo(cx - 28f * u, cy + 5f * u + bob, cx - 22f * u, cy + 18f * u + bob)
                quadraticTo(cx - 18f * u, cy + 10f * u + bob, cx - 22f * u, cy - 8f * u + bob)
                close()
            }
            drawPath(leftWingPath, mainColor)

            // Sack of coins
            val bagX = cx + 22f * u
            val bagY = cy + 12f * u + bob
            
            // Draw bag body
            drawCircle(Color(0xFFF59E0B), radius = 8f * u, center = Offset(bagX, bagY))
            
            // Tie at neck
            val tiePath = Path().apply {
                moveTo(bagX - 3f * u, bagY - 7f * u)
                lineTo(bagX + 3f * u, bagY - 7f * u)
                lineTo(bagX, bagY - 11f * u)
                close()
            }
            drawPath(tiePath, Color(0xFFD97706))
            
            // Dollar sign ($) details
            drawLine(Color.Black, start = Offset(bagX, bagY - 4f * u), end = Offset(bagX, bagY + 4f * u), strokeWidth = 1f * u)
            drawArc(
                color = Color.Black,
                startAngle = 90f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(bagX - 1.5f * u, bagY - 3f * u),
                size = Size(3f * u, 2.5f * u),
                style = Stroke(width = 0.8f * u)
            )
            drawArc(
                color = Color.Black,
                startAngle = -90f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(bagX - 1.5f * u, bagY + 0.5f * u),
                size = Size(3f * u, 2.5f * u),
                style = Stroke(width = 0.8f * u)
            )

            // Right flipper holding the tie of the bag
            val rightWingPath = Path().apply {
                moveTo(cx + 22f * u, cy - 8f * u + bob)
                quadraticTo(cx + 26f * u, cy + 4f * u + bob, bagX, bagY - 7f * u)
                quadraticTo(cx + 17f * u, cy - 4f * u + bob, cx + 22f * u, cy - 8f * u + bob)
                close()
            }
            drawPath(rightWingPath, mainColor)
        }
        BrishPose.NOTES -> {
            // Green/teal notepad in left flipper
            val notebookX = cx - 27f * u
            val notebookY = cy + bob + 2f * u
            drawRoundRect(
                color = Color(0xFF10B981), // Emerald cover
                topLeft = Offset(notebookX, notebookY),
                size = Size(10f * u, 14f * u),
                cornerRadius = CornerRadius(1f * u)
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(notebookX + 1.5f * u, notebookY + 0.5f * u),
                size = Size(7f * u, 13f * u)
            )

            // Left flipper holding notepad
            val leftWingPath = Path().apply {
                moveTo(cx - 22f * u, cy - 8f * u + bob)
                quadraticTo(cx - 26f * u, cy + 2f * u + bob, notebookX + 8f * u, notebookY + 7f * u)
                quadraticTo(cx - 18f * u, cy - 4f * u + bob, cx - 22f * u, cy - 8f * u + bob)
                close()
            }
            drawPath(leftWingPath, mainColor)

            // Yellow pencil in right flipper
            val pencilX = cx + 18f * u
            val pencilY = cy + bob + 4f * u
            rotate(degrees = -30f, pivot = Offset(pencilX, pencilY)) {
                drawRect(Color(0xFFFBBF24), topLeft = Offset(pencilX, pencilY), size = Size(2f * u, 7f * u))
                val pencilTipPath = Path().apply {
                    moveTo(pencilX, pencilY + 7f * u)
                    lineTo(pencilX + 2f * u, pencilY + 7f * u)
                    lineTo(pencilX + 1f * u, pencilY + 9f * u)
                    close()
                }
                drawPath(pencilTipPath, Color(0xFF78350F))
            }

            // Right flipper holding pencil
            val rightWingPath = Path().apply {
                moveTo(cx + 22f * u, cy - 8f * u + bob)
                quadraticTo(cx + 26f * u, cy + 2f * u + bob, pencilX, pencilY)
                quadraticTo(cx + 18f * u, cy - 4f * u + bob, cx + 22f * u, cy - 8f * u + bob)
                close()
            }
            drawPath(rightWingPath, mainColor)
        }
        BrishPose.HABITS -> {
            // Left flipper on hip
            val leftWingPath = Path().apply {
                moveTo(cx - 22f * u, cy - 8f * u + bob)
                quadraticTo(cx - 29f * u, cy + 2f * u + bob, cx - 18f * u, cy + 8f * u + bob)
                quadraticTo(cx - 20f * u, cy + 2f * u + bob, cx - 22f * u, cy - 8f * u + bob)
                close()
            }
            drawPath(leftWingPath, mainColor)

            // Heavy iron dumbbell lifted on the right
            val dbY = cy + bob - 10f * u
            // Dumbbell bar
            drawLine(Color.Gray, start = Offset(cx + 10f * u, dbY), end = Offset(cx + 34f * u, dbY), strokeWidth = 3f * u)
            // Weight plates
            drawCircle(Color(0xFF1E293B), radius = 5f * u, center = Offset(cx + 14f * u, dbY))
            drawCircle(Color(0xFF1E293B), radius = 5f * u, center = Offset(cx + 30f * u, dbY))

            // Right flipper holding the dumbbell aloft
            val rightWingPath = Path().apply {
                moveTo(cx + 22f * u, cy - 8f * u + bob)
                quadraticTo(cx + 28f * u, cy - 14f * u + bob, cx + 22f * u, dbY)
                quadraticTo(cx + 16f * u, cy - 6f * u + bob, cx + 22f * u, cy - 8f * u + bob)
                close()
            }
            drawPath(rightWingPath, mainColor)
        }
        BrishPose.FOCUS -> {
            // Draw DJ Headphones over the head
            drawArc(
                color = Color(0xFF06B6D4), // Cyan color matching the brand
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(cx - 21f * u, headCy - 21f * u),
                size = Size(42f * u, 42f * u),
                style = Stroke(width = 3.5f * u)
            )
            // Cushions/cups
            drawOval(Color(0xFF06B6D4), topLeft = Offset(cx - 23f * u, headCy - 8f * u), size = Size(6f * u, 14f * u))
            drawOval(Color(0xFF06B6D4), topLeft = Offset(cx + 17f * u, headCy - 8f * u), size = Size(6f * u, 14f * u))

            // DJ mixing deck in front
            val tableY = cy + 18f * u + bob
            drawRoundRect(
                color = Color(0xFF334155),
                topLeft = Offset(cx - 28f * u, tableY),
                size = Size(56f * u, 12f * u),
                cornerRadius = CornerRadius(2f * u)
            )
            // Vinyl records
            drawCircle(Color.Black, radius = 5f * u, center = Offset(cx - 12f * u, tableY + 6f * u))
            drawCircle(Color.White, radius = 1.5f * u, center = Offset(cx - 12f * u, tableY + 6f * u))
            
            drawCircle(Color.Black, radius = 5f * u, center = Offset(cx + 12f * u, tableY + 6f * u))
            drawCircle(Color.White, radius = 1.5f * u, center = Offset(cx + 12f * u, tableY + 6f * u))

            // Flippers scratching records
            val leftWingPath = Path().apply {
                moveTo(cx - 22f * u, cy - 8f * u + bob)
                quadraticTo(cx - 26f * u, cy + 8f * u + bob, cx - 14f * u, tableY + 4f * u)
                quadraticTo(cx - 18f * u, cy + 2f * u + bob, cx - 22f * u, cy - 8f * u + bob)
                close()
            }
            drawPath(leftWingPath, mainColor)

            val rightWingPath = Path().apply {
                moveTo(cx + 22f * u, cy - 8f * u + bob)
                quadraticTo(cx + 26f * u, cy + 8f * u + bob, cx + 14f * u, tableY + 4f * u)
                quadraticTo(cx + 18f * u, cy + 2f * u + bob, cx + 22f * u, cy - 8f * u + bob)
                close()
            }
            drawPath(rightWingPath, mainColor)
        }
        BrishPose.GARDENING -> {
            // Left flipper hanging down
            val leftWingPath = Path().apply {
                moveTo(cx - 22f * u, cy - 8f * u + bob)
                quadraticTo(cx - 28f * u, cy + 5f * u + bob, cx - 22f * u, cy + 18f * u + bob)
                quadraticTo(cx - 18f * u, cy + 10f * u + bob, cx - 22f * u, cy - 8f * u + bob)
                close()
            }
            drawPath(leftWingPath, mainColor)

            // Green watering can
            val canX = cx + 16f * u
            val canY = cy + bob + 4f * u
            drawRoundRect(
                color = Color(0xFF10B981),
                topLeft = Offset(canX, canY),
                size = Size(14f * u, 10f * u),
                cornerRadius = CornerRadius(2f * u)
            )
            // Handle
            drawArc(
                color = Color(0xFF047857),
                startAngle = 90f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(canX - 4f * u, canY + 1f * u),
                size = Size(8f * u, 8f * u),
                style = Stroke(width = 1.5f * u)
            )
            // Spout
            val spoutPath = Path().apply {
                moveTo(canX + 14f * u, canY + 3f * u)
                lineTo(canX + 21f * u, canY - 2f * u)
                lineTo(canX + 20f * u, canY - 4f * u)
                lineTo(canX + 14f * u, canY + 1f * u)
                close()
            }
            drawPath(spoutPath, Color(0xFF047857))
            // Blue water droplets
            val dropOffset = (breathe * 2f) % 8f
            drawCircle(Color(0xFF60A5FA), radius = 1f * u, center = Offset(canX + 22f * u, canY - 4f * u + dropOffset * u))
            drawCircle(Color(0xFF60A5FA), radius = 0.8f * u, center = Offset(canX + 24f * u, canY - 2f * u + ((dropOffset + 4f) % 8f) * u))

            // Right flipper holding the handle of the can
            val rightWingPath = Path().apply {
                moveTo(cx + 22f * u, cy - 8f * u + bob)
                quadraticTo(cx + 24f * u, cy + 4f * u + bob, canX, canY + 5f * u)
                quadraticTo(cx + 18f * u, cy - 2f * u + bob, cx + 22f * u, cy - 8f * u + bob)
                close()
            }
            drawPath(rightWingPath, mainColor)
        }
        else -> {
            // DEFAULT (and any other unhandled poses)
            // Left flipper: normal hanging down
            val leftAngle = 10f + (breathe - 1f) * 3f
            rotate(degrees = leftAngle, pivot = Offset(cx - 22f * u, cy - 8f * u + bob)) {
                val leftWingPath = Path().apply {
                    moveTo(cx - 22f * u, cy - 8f * u + bob)
                    quadraticTo(cx - 28f * u, cy + 5f * u + bob, cx - 22f * u, cy + 18f * u + bob)
                    quadraticTo(cx - 18f * u, cy + 10f * u + bob, cx - 22f * u, cy - 8f * u + bob)
                    close()
                }
                drawPath(leftWingPath, mainColor)
            }

            // Right flipper:
            if (whistleScale > 5f) {
                // Holding whistle to mouth
                val rightWingPath = Path().apply {
                    moveTo(cx + 22f * u, cy - 8f * u + bob)
                    quadraticTo(cx + 24f * u, cy + 5f * u + bob, cx + 5f * u, cy + bob)
                    quadraticTo(cx + 12f * u, cy - 4f * u + bob, cx + 22f * u, cy - 8f * u + bob)
                    close()
                }
                drawPath(rightWingPath, mainColor)
            } else if (eatingActive) {
                // Draw little fish near mouth
                val fishX = cx + 8f * u
                val fishY = headCy + 10f * u
                val fishPath = Path().apply {
                    moveTo(fishX, fishY)
                    quadraticTo(fishX + 6f * u, fishY - 3f * u, fishX + 12f * u, fishY)
                    quadraticTo(fishX + 6f * u, fishY + 3f * u, fishX, fishY)
                    close()
                }
                drawPath(fishPath, Color(0xFF94A3B8)) // Silver fish
                
                val tailPath = Path().apply {
                    moveTo(fishX + 12f * u, fishY)
                    lineTo(fishX + 16f * u, fishY - 4f * u)
                    lineTo(fishX + 15f * u, fishY)
                    lineTo(fishX + 16f * u, fishY + 4f * u)
                    close()
                }
                drawPath(tailPath, Color(0xFF94A3B8))
                drawCircle(Color.Black, radius = 0.5f * u, center = Offset(fishX + 3f * u, fishY - 1f * u))

                // Flipper holding the fish
                rotate(degrees = armRotation, pivot = Offset(cx + 22f * u, cy - 8f * u + bob)) {
                    val rightWingPath = Path().apply {
                        moveTo(cx + 22f * u, cy - 8f * u + bob)
                        quadraticTo(cx + 26f * u, cy + 2f * u + bob, fishX + 2f * u, fishY + 2f * u)
                        quadraticTo(cx + 16f * u, cy - 4f * u + bob, cx + 22f * u, cy - 8f * u + bob)
                        close()
                    }
                    drawPath(rightWingPath, mainColor)
                }
            } else {
                // Normal hanging down, waving if handWave is active
                val rightAngle = if (handWave != 0f) handWave else (-10f - (breathe - 1f) * 3f)
                rotate(degrees = rightAngle, pivot = Offset(cx + 22f * u, cy - 8f * u + bob)) {
                    val rightWingPath = Path().apply {
                        moveTo(cx + 22f * u, cy - 8f * u + bob)
                        quadraticTo(cx + 28f * u, cy + 5f * u + bob, cx + 22f * u, cy + 18f * u + bob)
                        quadraticTo(cx + 18f * u, cy + 10f * u + bob, cx + 22f * u, cy - 8f * u + bob)
                        close()
                    }
                    drawPath(rightWingPath, mainColor)
                }
            }
        }
    }

    // --- CORONA PREMIUM (reservado para uso futuro: isPremiumCrown=true) ---
    if (isPremiumCrown) {
        val crownY = headCy - 30f * u + bob
        val crownW = 16f * u
        val crownH = 10f * u
        val crownPath = Path().apply {
            moveTo(cx - crownW / 2f, crownY + crownH)
            lineTo(cx - crownW / 2f - 2f * u, crownY + 2f * u)
            lineTo(cx - crownW / 4f, crownY + crownH / 2f + 2f * u)
            lineTo(cx, crownY)
            lineTo(cx + crownW / 4f, crownY + crownH / 2f + 2f * u)
            lineTo(cx + crownW / 2f + 2f * u, crownY + 2f * u)
            lineTo(cx + crownW / 2f, crownY + crownH)
            close()
        }
        drawPath(crownPath, Color(0xFFFBBF24))
        drawCircle(Color(0xFFEF4444), radius = 1.2f * u, center = Offset(cx, crownY + 1f * u))
    }
}

/**
 * Combina el blink cíclico (eyeScale ∈ [0.1, 1]) con el blink aleatorio
 * (blinkExtra ∈ [0, 1], 1 = cerrado). Devuelve el factor de "apertura" final
 * (1=abierto, ~0.1=cerrado) para alimentar el alto del oval del ojo.
 */
private fun eyeOpenCombined(eyeScale: Float, blinkExtra: Float): Float {
    // Si blinkExtra = 1 → forzado cerrado. Sino, combinación mínima.
    val fromCyclic = eyeScale.coerceIn(0.1f, 1f)
    val fromRandom = 1f - blinkExtra.coerceIn(0f, 1f)
    return minOf(fromCyclic, fromRandom)
}
