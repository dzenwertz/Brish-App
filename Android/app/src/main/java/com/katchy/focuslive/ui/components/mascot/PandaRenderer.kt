package com.katchy.focuslive.ui.components.mascot

import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.katchy.focuslive.ui.components.BrishPose

fun DrawScope.drawPanda(
    pose: BrishPose,
    mainColor: Color,
    secondaryColor: Color,
    level: Int,
    isPremiumCrown: Boolean,
    bodyOffset: Float,
    breathe: Float,
    pandaHeadNod: Float,
    eyePanVal: Float,
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
    val pandaWhite = mainColor
    val pandaBlack = secondaryColor
    val pandaShadow = Color(0xFFF3F4F6)

    val bob = bodyOffset * (u / 3f)
    val nod = pandaHeadNod * (u / 3f)
    val eyeOff = eyePanVal * (u / 3f)

    val bodyCy = cy + 20.8f * u + bob
    val headCy = cy - 12.5f * u + bob + nod

    // 1. LEGS (Stubby & Cute sitting legs)
    drawOval(pandaBlack, topLeft = Offset(cx - 31.7f * u, bodyCy + 16.7f * u), size = Size(20.8f * u, 16.7f * u))
    drawOval(pandaBlack, topLeft = Offset(cx + 10.8f * u, bodyCy + 16.7f * u), size = Size(20.8f * u, 16.7f * u))
    // Paw Pads
    drawCircle(Color(0xFFE5E7EB), radius = 3.3f * u, center = Offset(cx - 20.8f * u, bodyCy + 25f * u))
    drawCircle(Color(0xFFE5E7EB), radius = 3.3f * u, center = Offset(cx + 20.8f * u, bodyCy + 25f * u))

    // 2. BODY (Soft & Round)
    drawOval(pandaWhite, topLeft = Offset(cx - 33.3f * u, bodyCy - 29.2f * u), size = Size(66.7f * u, 62.5f * u))
    // Belly shadow
    drawArc(
        pandaShadow,
        startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - 25f * u, bodyCy),
        size = Size(50f * u, 29.2f * u)
    )

    // 3. ARMS (Black Jacket Look)
    drawArc(
        pandaBlack,
        startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - 31.7f * u, bodyCy - 33.3f * u),
        size = Size(63.3f * u, 50f * u),
        style = Stroke(width = 13.3f * u)
    )

    // 4. HEAD
    val headW = 70f * u * eyePopScale
    val headH = 58.3f * u * eyePopScale
    // Ears
    drawCircle(pandaBlack, radius = 8.3f * u, center = Offset(cx - 26.7f * u, headCy - 16.7f * u))
    drawCircle(pandaBlack, radius = 8.3f * u, center = Offset(cx + 26.7f * u, headCy - 16.7f * u))
    
    // Face
    drawOval(pandaWhite, topLeft = Offset(cx - headW / 2f, headCy - 29.2f * u), size = Size(headW, headH))

    // Eye Patches
    val patchY = headCy - 1.67f * u
    rotate(-15f, pivot = Offset(cx - 16.7f * u, patchY)) {
        drawOval(pandaBlack, topLeft = Offset(cx - 26.7f * u, patchY - 6.7f * u), size = Size(20f * u, 25f * u))
    }
    rotate(15f, pivot = Offset(cx + 16.7f * u, patchY)) {
        drawOval(pandaBlack, topLeft = Offset(cx + 6.7f * u, patchY - 6.7f * u), size = Size(20f * u, 25f * u))
    }

    // Eyes
    if (pose == BrishPose.FOCUS) {
        drawLine(Color.White, start = Offset(cx - 20.8f * u, patchY + 4.2f * u), end = Offset(cx - 12.5f * u, patchY + 4.2f * u), strokeWidth = 1.67f * u)
        drawLine(Color.White, start = Offset(cx + 12.5f * u, patchY + 4.2f * u), end = Offset(cx + 20.8f * u, patchY + 4.2f * u), strokeWidth = 1.67f * u)
    } else {
        drawCircle(Color.White, radius = 2.5f * u * eyePopScale, center = Offset(cx - 16.7f * u + eyeOff, patchY + 2.5f * u))
        drawCircle(Color.White, radius = 2.5f * u * eyePopScale, center = Offset(cx + 16.7f * u + eyeOff, patchY + 2.5f * u))
    }

    // Nose
    drawOval(pandaBlack, topLeft = Offset(cx - 5f * u, headCy + 12.5f * u), size = Size(10f * u, 6.7f * u))

    // Mouth
    val mouthPath = Path().apply {
        moveTo(cx - 3.3f * u, headCy + 19.2f * u)
        quadraticTo(cx, headCy + 21.7f * u, cx + 3.3f * u, headCy + 19.2f * u)
    }
    drawPath(mouthPath, pandaBlack, style = Stroke(width = 1.25f * u))

    // 5. PROPS
    when(pose) {
        BrishPose.DEFAULT -> {
            val bambooColor = Color(0xFFD4B996)
            drawLine(bambooColor, start = Offset(cx - 12.5f * u, headCy + 29.2f * u), end = Offset(cx + 25f * u, bodyCy + 16.7f * u), strokeWidth = 5f * u, cap = StrokeCap.Round)
            drawOval(bambooColor, topLeft = Offset(cx - 16.7f * u, headCy + 25f * u), size = Size(8.3f * u, 3.3f * u))
            
            drawCircle(pandaBlack, radius = 6.7f * u, center = Offset(cx + 8.3f * u, bodyCy))
            drawCircle(pandaBlack, radius = 6.7f * u, center = Offset(cx - 4.2f * u, bodyCy - 4.2f * u))
        }
        BrishPose.FOCUS -> {
            drawCircle(pandaBlack, radius = 6.7f * u, center = Offset(cx - 29.2f * u, bodyCy + 8.3f * u))
            drawCircle(pandaBlack, radius = 6.7f * u, center = Offset(cx + 29.2f * u, bodyCy + 8.3f * u))
        }
        BrishPose.PLANNER -> {
            val scrollColor = Color(0xFFFEF3C7)
            val woodColor = Color(0xFF78350F)
            drawRect(scrollColor, topLeft = Offset(cx - 20.8f * u, bodyCy), size = Size(41.7f * u, 25f * u))
            drawCircle(woodColor, radius = 3.3f * u, center = Offset(cx - 20.8f * u, bodyCy + 12.5f * u))
            drawCircle(woodColor, radius = 3.3f * u, center = Offset(cx + 20.8f * u, bodyCy + 12.5f * u))
            
            drawOval(pandaBlack, topLeft = Offset(cx - 20.8f * u, headCy), size = Size(16.7f * u, 10f * u), style = Stroke(1.67f * u))
            drawOval(pandaBlack, topLeft = Offset(cx + 4.2f * u, headCy), size = Size(16.7f * u, 10f * u), style = Stroke(1.67f * u))
            drawLine(pandaBlack, start = Offset(cx - 4.2f * u, headCy + 5f * u), end = Offset(cx + 4.2f * u, headCy + 5f * u), strokeWidth = 1.67f * u)
            
            drawCircle(pandaBlack, radius = 5f * u, center = Offset(cx - 23.3f * u, bodyCy + 12.5f * u))
            drawCircle(pandaBlack, radius = 5f * u, center = Offset(cx + 23.3f * u, bodyCy + 12.5f * u))
        }
        BrishPose.FINANCE -> {
            val goldColor = Color(0xFFFFD700)
            val boatPath = Path().apply {
                moveTo(cx - 12.5f * u, bodyCy + 8.3f * u)
                quadraticTo(cx, bodyCy + 20.8f * u, cx + 12.5f * u, bodyCy + 8.3f * u)
                lineTo(cx + 16.7f * u, bodyCy + 4.2f * u)
                quadraticTo(cx, bodyCy + 12.5f * u, cx - 16.7f * u, bodyCy + 4.2f * u)
                close()
            }
            drawPath(boatPath, goldColor)
            drawCircle(pandaBlack, radius = 6.7f * u, center = Offset(cx - 10f * u, bodyCy + 8.3f * u))
            drawCircle(pandaBlack, radius = 6.7f * u, center = Offset(cx + 10f * u, bodyCy + 8.3f * u))
        }
        else -> {
            rotate(-30f, pivot = Offset(cx - 29.2f * u, bodyCy)) {
                drawOval(pandaBlack, topLeft = Offset(cx - 37.5f * u, bodyCy - 8.3f * u), size = Size(15f * u, 29.2f * u))
            }
            rotate(30f, pivot = Offset(cx + 29.2f * u, bodyCy)) {
                drawOval(pandaBlack, topLeft = Offset(cx + 22.5f * u, bodyCy - 8.3f * u), size = Size(15f * u, 29.2f * u))
            }
            drawCircle(pandaBlack, radius = 6.7f * u, center = Offset(cx - 12.5f * u, bodyCy + 12.5f * u))
            drawCircle(pandaBlack, radius = 6.7f * u, center = Offset(cx + 12.5f * u, bodyCy + 12.5f * u))
        }
    }
    }
}
