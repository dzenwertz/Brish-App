package com.katchy.focuslive.ui.components.mascot

import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.katchy.focuslive.ui.components.BrishPose

fun DrawScope.drawTurtle(
    pose: BrishPose,
    mainColor: Color,
    secondaryColor: Color,
    level: Int,
    isPremiumCrown: Boolean,
    bodyOffset: Float,
    breathe: Float,
    headBobVal: Float,
    eyePanVal: Float,
    liftVal: Float,
    armRotation: Float,
    eyePopScale: Float = 1f
) {
    val layout = mascotLayout
    val u = layout.unit
    val cx = layout.center.x
    val cy = layout.center.y

    val isBaby = level < 5
    val baseScale = if (isBaby) 0.75f else 1.0f
    val eyePopScale = eyePopScale * (if (isBaby) 1.25f else 1.0f)

    scale(baseScale, baseScale, pivot = Offset(cx, cy + 40f * u)) {
    val skinColor = mainColor
    val shellColor = secondaryColor
    val plastronColor = Color(0xFFD1FAE5) // Light Belly

    val bob = bodyOffset * (u / 3f)
    val headBob = headBobVal * (u / 3f)
    val eyeX = eyePanVal * (u / 3f)
    val liftY = liftVal * (u / 3f)

    val bodyCy = cy + 4.2f * u + bob

    // 1. SHELL RIM (Background)
    drawOval(shellColor, topLeft = Offset(cx - 33.3f * u, bodyCy - 29.2f * u), size = Size(66.7f * u, 75f * u))

    // 2. FEET (Chunky pads)
    drawOval(skinColor, topLeft = Offset(cx - 29.2f * u, bodyCy + 29.2f * u), size = Size(20.8f * u, 16.7f * u))
    drawOval(skinColor, topLeft = Offset(cx + 8.3f * u, bodyCy + 29.2f * u), size = Size(20.8f * u, 16.7f * u))

    // 3. BODY (Plastron/Belly)
    drawOval(skinColor, topLeft = Offset(cx - 23.3f * u, bodyCy - 20.8f * u), size = Size(46.7f * u, 58.3f * u))
    // Plastron Plates
    drawRect(plastronColor, topLeft = Offset(cx - 12.5f * u, bodyCy - 12.5f * u), size = Size(25f * u, 37.5f * u), style = Stroke(width = 1.67f * u))
    drawLine(plastronColor, start = Offset(cx - 12.5f * u, bodyCy + 4.2f * u), end = Offset(cx + 12.5f * u, bodyCy + 4.2f * u), strokeWidth = 1.67f * u)

    // 4. HEAD
    val headCy = bodyCy - 33.3f * u + headBob
    val headRadius = 21.7f * u * eyePopScale
    drawCircle(skinColor, radius = headRadius, center = Offset(cx, headCy))

    // Bandana (Ninja Style orange mask)
    val maskColor = Color(0xFFF97316)
    val maskW = 40f * u * eyePopScale
    val maskH = 13.3f * u * eyePopScale
    drawRect(maskColor, topLeft = Offset(cx - maskW / 2f, headCy - maskH / 2f), size = Size(maskW, maskH))

    // 5. FACE
    val eyeSize = 6.7f * u * eyePopScale
    // Eyes (White over mask)
    drawCircle(Color.White, radius = eyeSize, center = Offset(cx - 8.3f * u + eyeX, headCy))
    drawCircle(Color.White, radius = eyeSize, center = Offset(cx + 8.3f * u + eyeX, headCy))
    // Pupils
    drawCircle(Color.Black, radius = 2.5f * u * eyePopScale, center = Offset(cx - 8.3f * u + eyeX, headCy))
    drawCircle(Color.Black, radius = 2.5f * u * eyePopScale, center = Offset(cx + 8.3f * u + eyeX, headCy))

    // 6. PROPS
    when(pose) {
        BrishPose.HABITS -> {
            // Lifting dumbbell
            val armY = bodyCy - 4.2f * u + liftY
            drawCircle(skinColor, radius = 6.7f * u, center = Offset(cx - 29.2f * u, armY))
            drawCircle(skinColor, radius = 6.7f * u, center = Offset(cx + 29.2f * u, armY))
            // Dumbbell
            drawLine(Color.Gray, start = Offset(cx - 33.3f * u, armY), end = Offset(cx + 33.3f * u, armY), strokeWidth = 3.3f * u)
            drawRect(Color.Black, topLeft = Offset(cx - 35f * u, armY - 4.2f * u), size = Size(5f * u, 8.3f * u))
            drawRect(Color.Black, topLeft = Offset(cx + 30f * u, armY - 4.2f * u), size = Size(5f * u, 8.3f * u))
        }
        else -> {
            // Default Arms
            drawOval(skinColor, topLeft = Offset(cx - 30f * u, bodyCy - 8.3f * u), size = Size(10f * u, 20.8f * u))
            drawOval(skinColor, topLeft = Offset(cx + 20f * u, bodyCy - 8.3f * u), size = Size(10f * u, 20.8f * u))
        }
    }
    }
}
