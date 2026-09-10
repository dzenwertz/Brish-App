package com.katchy.focuslive.ui.components.mascot

import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.katchy.focuslive.ui.components.BrishPose

fun DrawScope.drawLlama(
    pose: BrishPose,
    mainColor: Color,
    secondaryColor: Color,
    level: Int,
    isPremiumCrown: Boolean,
    bodyOffset: Float,
    breathe: Float,
    headBobVal: Float,
    eyePanVal: Float,
    chewVal: Float,
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
    val furColor = mainColor
    val woolColor = secondaryColor
    val hoofColor = Color(0xFF5D4037)

    // Translate animations to relative scale
    val bob = bodyOffset * (u / 3f)
    val headBob = headBobVal * (u / 3f)
    val eyeX = eyePanVal * (u / 3f)
    val chewY = chewVal * (u / 3f)

    val bodyBaseY = cy + 20.8f * u + bob

    // 1. LEGS (Patitas)
    val legY = bodyBaseY + 8.3f * u
    drawOval(furColor, topLeft = Offset(cx - 26.7f * u, legY), size = Size(11.7f * u, 23.3f * u))
    drawOval(furColor, topLeft = Offset(cx + 15f * u, legY), size = Size(11.7f * u, 23.3f * u))
    drawOval(hoofColor, topLeft = Offset(cx - 26.7f * u, legY + 18.3f * u), size = Size(11.7f * u, 6.7f * u))
    drawOval(hoofColor, topLeft = Offset(cx + 15f * u, legY + 18.3f * u), size = Size(11.7f * u, 6.7f * u))
    
    val frontLegY = bodyBaseY + 12.5f * u
    drawOval(furColor, topLeft = Offset(cx - 18.3f * u, frontLegY), size = Size(11.7f * u, 23.3f * u))
    drawOval(furColor, topLeft = Offset(cx + 6.7f * u, frontLegY), size = Size(11.7f * u, 23.3f * u))
    drawOval(hoofColor, topLeft = Offset(cx - 18.3f * u, frontLegY + 18.3f * u), size = Size(11.7f * u, 6.7f * u))
    drawOval(hoofColor, topLeft = Offset(cx + 6.7f * u, frontLegY + 18.3f * u), size = Size(11.7f * u, 6.7f * u))

    // 2. NECK & BODY (Unified Shape)
    val llamaPath = Path().apply {
        moveTo(cx, bodyBaseY + 16.7f * u)
        quadraticTo(cx + 40f * u, bodyBaseY + 16.7f * u, cx + 35f * u, bodyBaseY - 16.7f * u)
        quadraticTo(cx + 25f * u, bodyBaseY - 33.3f * u, cx + 16.7f * u, bodyBaseY - 66.7f * u + headBob)
        lineTo(cx - 16.7f * u, bodyBaseY - 66.7f * u + headBob)
        quadraticTo(cx - 25f * u, bodyBaseY - 33.3f * u, cx - 35f * u, bodyBaseY - 16.7f * u)
        quadraticTo(cx - 40f * u, bodyBaseY + 16.7f * u, cx, bodyBaseY + 16.7f * u)
        close()
    }
    drawPath(llamaPath, woolColor)
    
    // Chest Wool
    drawCircle(woolColor, radius = 21.7f * u, center = Offset(cx, bodyBaseY - 16.7f * u))

    // 3. OUTFITS (Body Layer)
    when(pose) {
        BrishPose.DEFAULT -> {
            val ponchoPath = Path().apply {
                moveTo(cx - 18.3f * u, bodyBaseY - 37.5f * u)
                lineTo(cx + 18.3f * u, bodyBaseY - 37.5f * u)
                quadraticTo(cx + 41.7f * u, bodyBaseY - 8.3f * u, cx + 40f * u, bodyBaseY + 12.5f * u)
                lineTo(cx, bodyBaseY + 29.2f * u)
                lineTo(cx - 40f * u, bodyBaseY + 12.5f * u)
                quadraticTo(cx - 41.7f * u, bodyBaseY - 8.3f * u, cx - 18.3f * u, bodyBaseY - 37.5f * u)
                close()
            }
            drawPath(ponchoPath, Color(0xFFD32F2F))
            
            val stripePath1 = Path().apply {
                 moveTo(cx - 31.7f * u, bodyBaseY - 8.3f * u)
                 quadraticTo(cx, bodyBaseY + 4.2f * u, cx + 31.7f * u, bodyBaseY - 8.3f * u)
            }
            drawPath(stripePath1, Color(0xFFFFC107), style = Stroke(5f * u))
            
            val stripePath2 = Path().apply {
                 moveTo(cx - 23.3f * u, bodyBaseY + 8.3f * u)
                 quadraticTo(cx, bodyBaseY + 16.7f * u, cx + 23.3f * u, bodyBaseY + 8.3f * u)
            }
            drawPath(stripePath2, Color(0xFF388E3C), style = Stroke(5f * u))
        }
        BrishPose.PLANNER -> {
             val jerseyPath = Path().apply {
                 moveTo(cx - 21.7f * u, bodyBaseY - 33.3f * u)
                 quadraticTo(cx, bodyBaseY - 29.2f * u, cx + 21.7f * u, bodyBaseY - 33.3f * u)
                 quadraticTo(cx + 35f * u, bodyBaseY - 8.3f * u, cx + 31.7f * u, bodyBaseY + 12.5f * u)
                 quadraticTo(cx, bodyBaseY + 20.8f * u, cx - 31.7f * u, bodyBaseY + 12.5f * u)
                 quadraticTo(cx - 35f * u, bodyBaseY - 8.3f * u, cx - 21.7f * u, bodyBaseY - 33.3f * u)
                 close()
             }
             drawPath(jerseyPath, Color.White)
             
             val sashPath = Path().apply {
                 moveTo(cx - 21.7f * u, bodyBaseY - 33.3f * u)
                 lineTo(cx - 8.3f * u, bodyBaseY - 33.3f * u)
                 cubicTo(cx - 4.2f * u, bodyBaseY - 16.7f * u, 
                         cx + 16.7f * u, bodyBaseY - 4.2f * u,
                         cx + 30f * u, bodyBaseY + 12.5f * u)
                 lineTo(cx + 15f * u, bodyBaseY + 13.3f * u)
                 cubicTo(cx + 4.2f * u, bodyBaseY, 
                         cx - 16.7f * u, bodyBaseY - 16.7f * u,
                         cx - 21.7f * u, bodyBaseY - 33.3f * u)
                 close()
             }
             drawPath(sashPath, Color(0xFFD32F2F))
             
             drawArc(Color.White, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(cx - 36.7f * u, bodyBaseY - 25f * u), size = Size(11.7f * u, 16.7f * u))
             drawArc(Color.White, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(cx + 25f * u, bodyBaseY - 25f * u), size = Size(11.7f * u, 16.7f * u))
        }
        else -> {}
    }

    // 4. HEAD
    val headTipY = bodyBaseY - 83.3f * u + headBob
    val headW = 36.7f * u * eyePopScale
    val headH = 37.5f * u * eyePopScale
    drawOval(furColor, topLeft = Offset(cx - headW / 2f, headTipY), size = Size(headW, headH))
    
    // Snout (Protruding)
    drawOval(Color(0xFFFDE68A), topLeft = Offset(cx - 15f * u, headTipY + 20.8f * u + chewY), size = Size(30f * u, 20.8f * u))
    
    // Nose
    drawOval(Color(0xFF92400E), topLeft = Offset(cx - 5f * u, headTipY + 26.7f * u + chewY), size = Size(10f * u, 6.7f * u))
    // Mouth Line
    drawLine(Color(0xFF92400E), start = Offset(cx, headTipY + 30f * u + chewY), end = Offset(cx, headTipY + 35f * u + chewY), strokeWidth = 1.67f * u)
    
    // EARS
    val earY = headTipY + 4.2f * u
    val leftEarPath = Path().apply {
        moveTo(cx - 12.5f * u, earY)
        quadraticTo(cx - 20.8f * u, earY - 12.5f * u, cx - 16.7f * u, earY - 29.2f * u)
        quadraticTo(cx - 8.3f * u, earY - 12.5f * u, cx - 4.2f * u, earY)
        close()
    }
    drawPath(leftEarPath, furColor)
    val rightEarPath = Path().apply {
        moveTo(cx + 12.5f * u, earY)
        quadraticTo(cx + 20.8f * u, earY - 12.5f * u, cx + 16.7f * u, earY - 29.2f * u)
        quadraticTo(cx + 8.3f * u, earY - 12.5f * u, cx + 4.2f * u, earY)
        close()
    }
    drawPath(rightEarPath, furColor)
    
    // 5. FACE ELEMENTS
    val eyeRadius = 6.7f * u * eyePopScale
    val leftEyeCenter = Offset(cx - 12.5f * u, headTipY + 15f * u)
    val rightEyeCenter = Offset(cx + 12.5f * u, headTipY + 15f * u)
    if (pose == BrishPose.FOCUS) {
        // Cool Sunglasses (Black Aviators)
        val glassW = 16.7f * u
        val glassH = 11.7f * u
        drawOval(Color.Black, topLeft = Offset(cx - 18.3f * u, headTipY + 12.5f * u), size = Size(glassW, glassH))
        drawOval(Color.Black, topLeft = Offset(cx + 1.7f * u, headTipY + 12.5f * u), size = Size(glassW, glassH))
        drawLine(Color.Black, start = Offset(cx - 1.7f * u, headTipY + 15f * u), end = Offset(cx + 1.7f * u, headTipY + 15f * u), strokeWidth = 1.67f * u)
        // Glint
        drawLine(Color.White, start = Offset(cx - 15f * u, headTipY + 15f * u), end = Offset(cx - 10f * u, headTipY + 18.3f * u), strokeWidth = 1.67f * u)
    } else {
        // Regular Eyes
        drawCircle(Color.White, radius = eyeRadius, center = leftEyeCenter)
        drawCircle(Color.White, radius = eyeRadius, center = rightEyeCenter)
        drawCircle(Color.Black, radius = 2.5f * u, center = leftEyeCenter)
        drawCircle(Color.Black, radius = 2.5f * u, center = rightEyeCenter)
        // Eyelashes/Sassy
        drawLine(Color.Black, start = Offset(cx - 18.3f * u, headTipY + 12.5f * u), end = Offset(cx - 23.3f * u, headTipY + 10f * u), strokeWidth = 1.67f * u)
        drawLine(Color.Black, start = Offset(cx + 18.3f * u, headTipY + 12.5f * u), end = Offset(cx + 23.3f * u, headTipY + 10f * u), strokeWidth = 1.67f * u)
    }
    
    // 6. HEAD GEAR (Chullo)
    if (pose == BrishPose.DEFAULT || pose == BrishPose.FINANCE) {
         val hatY = headTipY - 10f * u 
         val chulloColor = Color(0xFFEF4444)
         val chulloPath = Path().apply {
             moveTo(cx - 18.3f * u, hatY + 8.3f * u)
             quadraticTo(cx, hatY - 16.7f * u, cx + 18.3f * u, hatY + 8.3f * u)
             lineTo(cx + 20f * u, hatY + 25f * u)
             lineTo(cx + 12.5f * u, hatY + 20.8f * u)
             lineTo(cx - 12.5f * u, hatY + 20.8f * u)
             lineTo(cx - 20f * u, hatY + 25f * u)
             close()
         }
         drawPath(chulloPath, chulloColor)
         // Patterns
         drawLine(Color(0xFFF59E0B), start = Offset(cx - 12.5f * u, hatY + 4.2f * u), end = Offset(cx + 12.5f * u, hatY + 4.2f * u), strokeWidth = 2.5f * u)
         // Pompom
         drawCircle(Color(0xFF3B82F6), radius = 4.2f * u, center = Offset(cx, hatY - 12.5f * u))
         // Tassels
         drawLine(chulloColor, start = Offset(cx - 20f * u, hatY + 25f * u), end = Offset(cx - 23.3f * u, hatY + 37.5f * u), strokeWidth = 1.67f * u)
         drawLine(chulloColor, start = Offset(cx + 20f * u, hatY + 25f * u), end = Offset(cx + 23.3f * u, hatY + 37.5f * u), strokeWidth = 1.67f * u)
    }

    // 7. PROPS
    when(pose) {
         BrishPose.NOTES -> {
             val charangoColor = Color(0xFFD4B483)
             val cX = cx + 25f * u
             val cY = bodyBaseY - 8.3f * u
             rotate(-30f, pivot = Offset(cX, cY)) {
                 drawOval(charangoColor, topLeft = Offset(cX - 12.5f * u, cY), size = Size(25f * u, 33.3f * u))
                 drawCircle(Color.Black, radius = 4.2f * u, center = Offset(cX, cY + 16.7f * u))
                 drawRect(Color(0xFF5D4037), topLeft = Offset(cX - 3.3f * u, cY - 25f * u), size = Size(6.7f * u, 25f * u))
                 drawRect(charangoColor, topLeft = Offset(cX - 5f * u, cY - 33.3f * u), size = Size(10f * u, 8.3f * u))
             }
             drawCircle(furColor, radius = 5f * u, center = Offset(cx + 12.5f * u, bodyBaseY))
         }
         BrishPose.FINANCE -> {
             // Sol Coin
             drawCircle(Color(0xFFFFD700), radius = 10f * u, center = Offset(cx + 20.8f * u, bodyBaseY))
             drawCircle(Color(0xFFF59E0B), radius = 8.3f * u, center = Offset(cx + 20.8f * u, bodyBaseY), style = Stroke(0.83f * u))
             drawLine(Color(0xFFF59E0B), start = Offset(cx + 20.8f * u, bodyBaseY - 4.2f * u), end = Offset(cx + 20.8f * u, bodyBaseY + 4.2f * u), strokeWidth = 1.67f * u)
             drawLine(Color(0xFFF59E0B), start = Offset(cx + 16.7f * u, bodyBaseY - 4.2f * u), end = Offset(cx + 25f * u, bodyBaseY + 4.2f * u), strokeWidth = 1.67f * u)
         }
         BrishPose.HABITS -> {
             // Sweatband
              drawRect(Color(0xFF3B82F6), topLeft = Offset(cx - 18.3f * u, headTipY + 4.2f * u), size = Size(36.7f * u, 5f * u))
         }
         else -> {}
    }
    }
}
