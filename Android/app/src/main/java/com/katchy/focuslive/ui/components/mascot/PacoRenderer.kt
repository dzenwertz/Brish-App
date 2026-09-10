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

fun DrawScope.drawPaco(
    pose: BrishPose,
    mainColor: Color,
    secondaryColor: Color,
    level: Int,
    isPremiumCrown: Boolean,
    bodyOffset: Float,
    breathe: Float,
    headBobVal: Float,
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
    val pacoOrange = mainColor
    val pacoLight = secondaryColor
    val pacoDark = Color(0xFFEA580C) // Stripes

    val bob = bodyOffset * (u / 3f)
    val headBob = headBobVal * (u / 3f)
    val eyeOff = eyePanVal * (u / 3f)

    val pacoCy = cy + 16.7f * u + bob
    val headCy = cy - 16.7f * u + bob + headBob

    // 1. TAIL (Wagging)
    val tailX = if (pose == BrishPose.DEFAULT) eyeOff * 2f else 0f
    val tailPath = Path().apply {
        moveTo(cx + 25f * u, pacoCy + 16.7f * u)
        quadraticTo(cx + 41.7f * u + tailX, pacoCy - 8.3f * u, cx + 33.3f * u + tailX, pacoCy - 16.7f * u)
    }
    drawPath(tailPath, color = pacoOrange, style = Stroke(width = 6.7f * u, cap = StrokeCap.Round))

    // 2. BODY (Round Ball)
    drawCircle(color = pacoOrange, radius = 29.2f * u, center = Offset(cx, pacoCy))
    drawCircle(color = pacoLight, radius = 16.7f * u, center = Offset(cx, pacoCy + 8.3f * u)) // Belly

    // 3. HEAD
    val headRadius = 26.7f * u * eyePopScale
    
    // Ears
    val earPath = Path().apply {
        moveTo(cx - 20.8f * u, headCy - 16.7f * u)
        quadraticTo(cx - 29.2f * u, headCy - 37.5f * u, cx - 12.5f * u, headCy - 25f * u)
        
        moveTo(cx + 20.8f * u, headCy - 16.7f * u)
        quadraticTo(cx + 29.2f * u, headCy - 37.5f * u, cx + 12.5f * u, headCy - 25f * u)
    }
    drawPath(earPath, pacoOrange)
    
    drawCircle(color = pacoOrange, radius = headRadius, center = Offset(cx, headCy))
    
    // Stripes (Top)
    drawArc(
        color = pacoDark,
        startAngle = -110f, sweepAngle = 40f, useCenter = false,
        topLeft = Offset(cx - headRadius, headCy - headRadius),
        size = Size(headRadius * 2f, headRadius * 2f),
        style = Stroke(width = 3.3f * u)
    )

    // 4. FACE
    // Muzzle
    drawOval(pacoLight, topLeft = Offset(cx - 10f * u, headCy + 4.2f * u), size = Size(20f * u, 11.7f * u))
    // Nose
    drawCircle(Color(0xFFEA580C), radius = 1.67f * u, center = Offset(cx, headCy + 6.7f * u))
    
    // Eyes
    val eyeY = headCy - 4.2f * u
    val eyeSize = 6.7f * u * eyePopScale
    drawCircle(Color.White, radius = eyeSize, center = Offset(cx - 11.7f * u + eyeOff, eyeY))
    drawCircle(Color.White, radius = eyeSize, center = Offset(cx + 11.7f * u + eyeOff, eyeY))
    // Pupils
    drawCircle(Color.Black, radius = 3.3f * u * eyePopScale, center = Offset(cx - 11.7f * u + eyeOff, eyeY))
    drawCircle(Color.Black, radius = 3.3f * u * eyePopScale, center = Offset(cx + 11.7f * u + eyeOff, eyeY))
    // Sparkle
    drawCircle(Color.White, radius = 1.67f * u, center = Offset(cx - 10f * u + eyeOff, eyeY - 1.67f * u))
    drawCircle(Color.White, radius = 1.67f * u, center = Offset(cx + 13.3f * u + eyeOff, eyeY - 1.67f * u))

    // 5. PAWS & PROPS
    val pawColor = pacoLight
    val feetY = cy + 37.5f * u + bob
    drawCircle(pawColor, radius = 6.7f * u, center = Offset(cx - 12.5f * u, feetY)) // Left Foot
    drawCircle(pawColor, radius = 6.7f * u, center = Offset(cx + 12.5f * u, feetY)) // Right Foot

    when(pose) {
        BrishPose.PLANNER -> {
             val pencilAngle = 45f
             rotate(pencilAngle, pivot = Offset(cx + 20.8f * u, cy + 8.3f * u)) {
                 drawRect(Color(0xFFFFC107), topLeft = Offset(cx + 16.7f * u, cy), size = Size(8.3f * u, 25f * u))
                 drawRect(Color(0xFFF87171), topLeft = Offset(cx + 16.7f * u, cy), size = Size(8.3f * u, 4.2f * u))
             }
             drawCircle(pawColor, radius = 5f * u, center = Offset(cx + 20.8f * u, cy + 12.5f * u + bob))
             drawCircle(pawColor, radius = 5f * u, center = Offset(cx - 12.5f * u, cy + 12.5f * u + bob))
        }
        BrishPose.HABITS -> {
             drawCircle(pawColor, radius = 5f * u, center = Offset(cx - 20.8f * u, cy + bob))
             drawCircle(pawColor, radius = 5f * u, center = Offset(cx + 20.8f * u, cy + bob))
        }
        BrishPose.FOCUS -> {
             val hpColor = Color(0xFF3B82F6)
             drawArc(
                 hpColor,
                 startAngle = 180f, sweepAngle = 180f, useCenter = false,
                 topLeft = Offset(cx - 28.3f * u, headCy - 16.7f * u),
                 size = Size(56.7f * u, 50f * u),
                 style = Stroke(5f * u)
             )
             drawOval(hpColor, topLeft = Offset(cx - 30f * u, headCy - 4.2f * u), size = Size(8.3f * u, 16.7f * u))
             drawOval(hpColor, topLeft = Offset(cx + 21.7f * u, headCy - 4.2f * u), size = Size(8.3f * u, 16.7f * u))
             
             drawCircle(pawColor, radius = 5f * u, center = Offset(cx - 12.5f * u, cy + 16.7f * u + bob))
             drawCircle(pawColor, radius = 5f * u, center = Offset(cx + 12.5f * u, cy + 16.7f * u + bob))
        }
        else -> {
             drawCircle(pawColor, radius = 5f * u, center = Offset(cx - 8.3f * u, cy + 16.7f * u + bob))
             drawCircle(pawColor, radius = 5f * u, center = Offset(cx + 8.3f * u, cy + 16.7f * u + bob))
        }
    }
    }
}
