package com.katchy.focuslive.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import com.katchy.focuslive.data.model.MascotType
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import com.katchy.focuslive.ui.components.mascot.drawPoppin
import com.katchy.focuslive.ui.components.mascot.drawLlama
import com.katchy.focuslive.ui.components.mascot.drawPaco
import com.katchy.focuslive.ui.components.mascot.drawPanda
import com.katchy.focuslive.ui.components.mascot.drawTurtle
import com.katchy.focuslive.ui.components.mascot.BrishEasings
import com.katchy.focuslive.ui.util.MascotEvent

enum class BrishPose {
    DEFAULT,    // Home (Fish)
    PLANNER,    // Holding Bat/Stick (Threatening)
    FINANCE,    // Holding Money Bag
    NOTES,      // Holding Spray Can (Graffiti)
    HABITS,     // Holding Dumbbell (Workout)
    FOCUS,      // Headphones (Deep Work)
    CHAT,        // Arms crossed / Listening
    GARDENING   // Watering plants
}

@Composable
fun BrishMascotAnimation(
    modifier: Modifier = Modifier,
    pose: BrishPose = BrishPose.DEFAULT,
    mascotType: MascotType = MascotType.POPPIN,
    level: Int = 1,
    isPremium: Boolean = false,
    reaction: com.katchy.focuslive.ui.util.MascotEvent? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot")
    
    // --- ENTRANCE ANIMATIONS ---
    val jumpAnim = remember { Animatable(0f) }
    val handWaveAnim = remember { Animatable(0f) }
    
    // --- CELEBRATION REACTION ANIMATIONS ---
    val celebrationJump = remember { Animatable(0f) }
    val celebrationScaleX = remember { Animatable(1f) }
    val celebrationScaleY = remember { Animatable(1f) }
    val celebrationRotation = remember { Animatable(0f) }
    val starAlpha = remember { Animatable(0f) }
    val starRotation = remember { Animatable(0f) }
    val starSpread = remember { Animatable(0f) }
    val eyePopScale = remember { Animatable(1f) }

    // Trigger celebration whenever a reaction event arrives
    LaunchedEffect(reaction) {
        reaction ?: return@LaunchedEffect

        // Reset
        celebrationJump.snapTo(0f)
        celebrationScaleX.snapTo(1f)
        celebrationScaleY.snapTo(1f)
        celebrationRotation.snapTo(0f)
        starAlpha.snapTo(0f)
        starRotation.snapTo(0f)
        starSpread.snapTo(0f)
        eyePopScale.snapTo(1f)

        when (mascotType) {
            MascotType.PANDA -> {
                // --- PANDA: Acrobatic 360 Spin Flip ---
                launch { celebrationScaleX.animateTo(1.15f, animationSpec = tween(120, easing = FastOutSlowInEasing)) }
                launch { celebrationScaleY.animateTo(0.75f, animationSpec = tween(120, easing = FastOutSlowInEasing)) }
                celebrationRotation.animateTo(-15f, animationSpec = tween(120, easing = FastOutSlowInEasing))

                eyePopScale.animateTo(1.4f, animationSpec = tween(100, easing = BrishEasings.EaseOutBackOvershoot))

                launch { celebrationScaleX.animateTo(0.9f, animationSpec = tween(350, easing = BrishEasings.EaseOutQuart)) }
                launch { celebrationScaleY.animateTo(1.15f, animationSpec = tween(350, easing = BrishEasings.EaseOutQuart)) }
                launch { celebrationRotation.animateTo(360f, animationSpec = tween(400, easing = BrishEasings.EaseOutQuart)) }
                launch {
                    starAlpha.animateTo(1f, animationSpec = tween(80))
                    starSpread.animateTo(1f, animationSpec = tween(400, easing = BrishEasings.EaseOutQuart))
                }

                celebrationJump.animateTo(-65f, animationSpec = tween(350, easing = BrishEasings.EaseOutQuart))
                celebrationJump.animateTo(0f, animationSpec = tween(380, easing = BrishEasings.EaseOutBackOvershoot))

                celebrationRotation.snapTo(0f)
                eyePopScale.animateTo(1f, animationSpec = tween(200, easing = BrishEasings.EaseOutQuart))
            }
            MascotType.TURTLE -> {
                // --- TURTLE (Torty): Ninja Shell Dance ---
                launch { celebrationScaleX.animateTo(1.22f, animationSpec = tween(140, easing = FastOutSlowInEasing)) }
                launch { celebrationScaleY.animateTo(0.70f, animationSpec = tween(140, easing = FastOutSlowInEasing)) }
                celebrationRotation.animateTo(25f, animationSpec = tween(140, easing = FastOutSlowInEasing))

                eyePopScale.animateTo(1.25f, animationSpec = tween(100))

                launch { celebrationRotation.animateTo(-25f, animationSpec = tween(160, easing = BrishEasings.EaseOutQuart)) }
                launch { celebrationRotation.animateTo(15f, animationSpec = tween(160, easing = BrishEasings.EaseOutQuart)) }
                launch { celebrationRotation.animateTo(0f, animationSpec = tween(120)) }

                celebrationJump.animateTo(-45f, animationSpec = tween(250, easing = BrishEasings.EaseOutQuart))
                launch {
                    starAlpha.animateTo(1f, animationSpec = tween(80))
                    starSpread.animateTo(1f, animationSpec = tween(350, easing = BrishEasings.EaseOutQuart))
                }

                celebrationJump.animateTo(0f, animationSpec = tween(350, easing = BrishEasings.EaseOutBackOvershoot))
                eyePopScale.animateTo(1f, animationSpec = tween(200))
            }
            MascotType.PACO -> {
                // --- PACO (Alpaca): High Springy Jump & Wiggle ---
                launch { celebrationScaleX.animateTo(1.15f, animationSpec = tween(100)) }
                launch { celebrationScaleY.animateTo(0.80f, animationSpec = tween(100)) }
                
                launch { celebrationScaleX.animateTo(0.85f, animationSpec = tween(250)) }
                launch { celebrationScaleY.animateTo(1.25f, animationSpec = tween(250)) }
                launch {
                    celebrationRotation.animateTo(-18f, animationSpec = tween(120))
                    celebrationRotation.animateTo(18f, animationSpec = tween(120))
                    celebrationRotation.animateTo(0f, animationSpec = tween(100))
                }

                celebrationJump.animateTo(-70f, animationSpec = tween(300, easing = BrishEasings.EaseOutQuart))
                launch {
                    starAlpha.animateTo(1f, animationSpec = tween(80))
                    starSpread.animateTo(1f, animationSpec = tween(400))
                }

                celebrationJump.animateTo(0f, animationSpec = tween(400, easing = BrishEasings.EaseOutBackOvershoot))
            }
            MascotType.LLAMA -> {
                // --- LLAMA: Regal Crown Shine & Elegant Bounce ---
                launch { celebrationScaleX.animateTo(1.08f, animationSpec = tween(120)) }
                launch { celebrationScaleY.animateTo(0.82f, animationSpec = tween(120)) }
                celebrationRotation.animateTo(10f, animationSpec = tween(120))

                launch { celebrationScaleX.animateTo(0.92f, animationSpec = tween(280)) }
                launch { celebrationScaleY.animateTo(1.20f, animationSpec = tween(280)) }
                launch { celebrationRotation.animateTo(-12f, animationSpec = tween(280)) }

                celebrationJump.animateTo(-50f, animationSpec = tween(280, easing = BrishEasings.EaseOutQuart))
                launch {
                    starAlpha.animateTo(1f, animationSpec = tween(80))
                    starSpread.animateTo(1f, animationSpec = tween(380))
                }

                celebrationJump.animateTo(0f, animationSpec = tween(400, easing = BrishEasings.EaseOutBackOvershoot))
                celebrationRotation.animateTo(0f, animationSpec = tween(200))
            }
            else -> {
                // --- POPPIN (Pez Coach): Double Elastic Jump ---
                launch {
                    celebrationScaleX.animateTo(1.15f, animationSpec = tween(140, easing = FastOutSlowInEasing))
                }
                launch {
                    celebrationScaleY.animateTo(0.75f, animationSpec = tween(140, easing = FastOutSlowInEasing))
                }
                celebrationRotation.animateTo(-8f, animationSpec = tween(140, easing = FastOutSlowInEasing))

                eyePopScale.animateTo(1.3f, animationSpec = tween(100, easing = BrishEasings.EaseOutBackOvershoot))

                launch {
                    celebrationScaleX.animateTo(0.92f, animationSpec = tween(280, easing = BrishEasings.EaseOutQuart))
                }
                launch {
                    celebrationScaleY.animateTo(1.18f, animationSpec = tween(280, easing = BrishEasings.EaseOutQuart))
                }
                launch {
                    celebrationRotation.animateTo(12f, animationSpec = tween(280, easing = BrishEasings.EaseOutQuart))
                }
                celebrationJump.animateTo(
                    targetValue = -55f,
                    animationSpec = tween(280, easing = BrishEasings.EaseOutQuart)
                )

                launch {
                    starAlpha.animateTo(1f, animationSpec = tween(80))
                    starSpread.animateTo(1f, animationSpec = tween(400, easing = BrishEasings.EaseOutQuart))
                }

                launch {
                    celebrationScaleX.animateTo(1.08f, animationSpec = tween(240, easing = BrishEasings.EaseOutBackOvershoot))
                    celebrationScaleX.animateTo(0.98f, animationSpec = tween(120, easing = BrishEasings.EaseOutQuart))
                    celebrationScaleX.animateTo(1f, animationSpec = tween(100, easing = BrishEasings.EaseOutQuart))
                }
                launch {
                    celebrationScaleY.animateTo(0.88f, animationSpec = tween(240, easing = BrishEasings.EaseOutBackOvershoot))
                    celebrationScaleY.animateTo(1.03f, animationSpec = tween(120, easing = BrishEasings.EaseOutQuart))
                    celebrationScaleY.animateTo(1f, animationSpec = tween(100, easing = BrishEasings.EaseOutQuart))
                }
                launch {
                    celebrationRotation.animateTo(-5f, animationSpec = tween(240, easing = BrishEasings.EaseOutBackOvershoot))
                    celebrationRotation.animateTo(2f, animationSpec = tween(120, easing = BrishEasings.EaseOutQuart))
                    celebrationRotation.animateTo(0f, animationSpec = tween(100, easing = BrishEasings.EaseOutQuart))
                }
                celebrationJump.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(460, easing = BrishEasings.EaseOutBackOvershoot)
                )

                eyePopScale.animateTo(1f, animationSpec = tween(200, easing = BrishEasings.EaseOutQuart))

                delay(80)
                launch {
                    celebrationScaleY.animateTo(1.08f, animationSpec = tween(150, easing = BrishEasings.EaseOutQuart))
                    celebrationScaleY.animateTo(1f, animationSpec = tween(150, easing = BrishEasings.EaseOutBackOvershoot))
                }
                launch {
                    celebrationScaleX.animateTo(0.96f, animationSpec = tween(150, easing = BrishEasings.EaseOutQuart))
                    celebrationScaleX.animateTo(1f, animationSpec = tween(150, easing = BrishEasings.EaseOutBackOvershoot))
                }
                celebrationJump.animateTo(
                    targetValue = -18f,
                    animationSpec = tween(150, easing = BrishEasings.EaseOutQuart)
                )
                celebrationJump.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(200, easing = BrishEasings.EaseOutBackOvershoot)
                )
            }
        }

        starRotation.animateTo(
            targetValue = 540f,
            animationSpec = tween(900, easing = BrishEasings.EaseOutQuart)
        )
        starAlpha.animateTo(0f, animationSpec = tween(350, easing = BrishEasings.EaseOutQuart))
    }
    
    // Trigger entrance animation whenever pose changes
    LaunchedEffect(pose) {
        // Reset
        jumpAnim.snapTo(0f)
        handWaveAnim.snapTo(0f)
        
        // Sequence: Jump Up -> Land -> Hand Wave
        val jumpDuration = 300
        
        // Jump Up
        jumpAnim.animateTo(
            targetValue = -20f,
            animationSpec = tween(durationMillis = jumpDuration / 2, easing = BrishEasings.EaseOutQuart)
        )
        // Land (elastic overshoot for Duolingo-like feel)
        jumpAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = jumpDuration / 2, easing = BrishEasings.EaseOutBackOvershoot)
        )
        
        // Hand Wave (Raise hand momentarily)
        handWaveAnim.animateTo(
            targetValue = -130f, // Raise arm up high
            animationSpec = tween(400)
        )
        delay(200)
        handWaveAnim.animateTo(
            targetValue = 0f, // Return to normal
            animationSpec = tween(400)
        )
    }

    // --- IDLE ANIMATIONS ---
    
    // Breathing/Bobbing animation
    val bodyBob by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f, 
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = BrishEasings.EaseInOutCubicSoft),
            repeatMode = RepeatMode.Reverse
        ), label = "bodyBob"
    )
    
    // Combine jump and bob
    val bodyOffset = bodyBob + jumpAnim.value
    
    // Blinking animation
    val eyeScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                1f at 0
                1f at 2800
                0.1f at 2900
                1f at 3000
            },
            repeatMode = RepeatMode.Restart
        ), label = "eyeBlink"
    )

    // Arm animation (Generic idle)
    val armRotationIdle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "armRot"
    )
    
    // Eating Animation (Specific to DEFAULT pose)
    // Arm moves from side to mouth
    val eatingArmRotation by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = -45f, // Move towards mouth
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0
                0f at 1000 // Hold down
                (-50f) at 1300 // Move up quickly
                (-40f) at 1500 // Munch
                (-50f) at 1700 // Munch
                0f at 2000 // Return down
            },
            repeatMode = RepeatMode.Restart
        ), label = "eating"
    )

    // GARDENING ANIMATION: Pouring Water
    val wateringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 25f, // Tilt forward
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "watering"
    )

    // Water Droplets (Y offset)
    val dropletOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "droplets"
    )

    // Determine finalized arm rotation based on state
    // If waving (entrance), use wave. 
    // If not waving: Use specific animation per pose.
    val isWaving = handWaveAnim.value < -10f // Threshold to detect active wave
    
    val currentArmRotation = if (handWaveAnim.value != 0f) {
        handWaveAnim.value // Priority to entrance wave
    } else {
         when (pose) {
             BrishPose.DEFAULT -> eatingArmRotation
             BrishPose.GARDENING -> wateringRotation
             else -> armRotationIdle
         }
    }
    
    // Mouth open/shut for eating
    val beakOpen by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0
                0f at 1300
                5f at 1400 // Open
                0f at 1500 // Close
                5f at 1600 // Open
                0f at 1700 // Close
                0f at 2000
            },
            repeatMode = RepeatMode.Restart
        ), label = "beak"
    )

    // --- PACO SPECIFIC ANIMATIONS ---
    // 1. Bounce (Default)
    val pacoBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pacoBounce"
    )

    // 2. Balance sway (Planner)
    val pacoBalance by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pacoBalance"
    )

    // 3. Roll (Habits) - X offset
    val pacoRoll by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pacoRoll"
    )

    // 4. Shake (Notes)
    val pacoShake by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pacoShake"
    )

    // --- PANDA SPECIFIC ANIMATIONS ---
    // 1. Head Nod (Default - Beat)
    val pandaHeadNod by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pandaNod"
    )
    
    // 2. DJ Scratch Hand (Focus)
    val pandaDJHand by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pandaDJ"
    )

    // 3. Foot Tap (Planner - Waiting)
    val pandaFootTap by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f, // Lift toe
        animationSpec = infiniteRepeatable(
            animation = keyframes { 
                durationMillis = 800
                0f at 0
                (-5f) at 200
                0f at 400
                0f at 800
            },
            repeatMode = RepeatMode.Restart
        ), label = "pandaTap"
    )

    // --- POPPIN SPECIFIC ANIMATIONS (Coach) ---
    // 1. Whistle Blow (Default)
    val poppinWhistle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f, // Scale/Move whistle
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0
                0f at 1000
                10f at 1200 // Blow
                0f at 1400
                10f at 1600 // Blow
                0f at 3000
            },
            repeatMode = RepeatMode.Restart
        ), label = "poppinWhistle"
    )
    
    // 2. Stopwatch Tick (Habits)
    val poppinTicking by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "poppinTick"
    )

    // --- GLOBAL ANIMATIONS (REFINED - NO BOUNCE) ---
    // 1. Subtle Breathe (Replaces Body Bob)
    val breatheAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f, // Very subtle up/down
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = BrishEasings.EaseInOutSineSoft),
            repeatMode = RepeatMode.Reverse
        ), label = "breathe"
    )

    // 2. Reading Eye Pan (For Planner/Notes)
    val eyePanAnim by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "eyePan"
    )

    // 3. Head Bob (For Music/Focus)
    val headBobAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600 // ~100 BPM
                0f at 0
                5f at 300
                0f at 600
            },
            repeatMode = RepeatMode.Restart
        ), label = "headBob"
    )
    
    // 4. Weight Lift (For Habits)
    val liftAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0
                0f at 500
                -15f at 1000 // Lift
                -15f at 1500 // Hold
                0f at 2000 // Drop
            },
            repeatMode = RepeatMode.Restart
        ), label = "lift"
    )

    BoxWithConstraints(
        modifier = modifier
            .graphicsLayer {
                translationY = celebrationJump.value
                scaleX = celebrationScaleX.value
                scaleY = celebrationScaleY.value
                rotationZ = celebrationRotation.value
                transformOrigin = TransformOrigin(0.5f, 1f) // scale/rotate from feet
            },
        contentAlignment = Alignment.Center
    ) {
        val scale = if (maxWidth > 0.dp && maxHeight > 0.dp && maxWidth != Dp.Infinity && maxHeight != Dp.Infinity) {
            minOf(maxWidth / 120.dp, maxHeight / 140.dp)
        } else {
            1f
        }
        Canvas(
            modifier = Modifier
                .size(120.dp, 140.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val bottomY = size.height - 10f
            
            val bodyOffset = breatheAnim

            // --- LEVEL SHINE (Aura) & PREMIUM CROWN ---
            if (isPremium) {
                // Gold shining premium aura
                drawCircle(Color(0xFFFBBF24).copy(alpha = 0.18f), radius = 55.dp.toPx(), center = Offset(centerX, size.height / 2))
                
                // Draw floating golden crown above mascot's head
                val crownY = 10f + bodyOffset
                val crownW = 16.dp.toPx()
                val crownH = 10.dp.toPx()
                val crownPath = Path().apply {
                    moveTo(centerX - crownW / 2, crownY + crownH)
                    lineTo(centerX - crownW / 2 - 2f, crownY + 2f)
                    lineTo(centerX - crownW / 4, crownY + crownH / 2 + 2f)
                    lineTo(centerX, crownY)
                    lineTo(centerX + crownW / 4, crownY + crownH / 2 + 2f)
                    lineTo(centerX + crownW / 2 + 2f, crownY + 2f)
                    lineTo(centerX + crownW / 2, crownY + crownH)
                    close()
                }
                drawPath(crownPath, Color(0xFFFBBF24))
                
                // Draw crown jewels
                drawCircle(Color(0xFFEF4444), radius = 1.2.dp.toPx(), center = Offset(centerX, crownY + 1f))
                drawCircle(Color(0xFF3B82F6), radius = 1f, center = Offset(centerX - crownW / 2 - 2f, crownY + 2f))
                drawCircle(Color(0xFF3B82F6), radius = 1f, center = Offset(centerX + crownW / 2 + 2f, crownY + 2f))
            } else if (level >= 10) {
                val auraColor = if (level >= 20) Color(0xFFFFD700).copy(alpha = 0.3f) else Color(0xFFC0C0C0).copy(alpha = 0.3f)
                drawCircle(auraColor, radius = 60.dp.toPx(), center = Offset(centerX, size.height / 2))
            }
            
            // --- PREPARE COLORS ---
            val mainColor = when(mascotType) {
                MascotType.POPPIN -> Color(0xFF1F2937) // Black
                MascotType.KITTY -> Color(0xFF9CA3AF) // Gray
                MascotType.PACO -> Color(0xFFF97316) // Orange
                MascotType.PANDA -> Color.White // Panda Body is White
                MascotType.CEBRIC -> Color(0xFFFEF3C7) // Cream
                MascotType.TURTLE -> Color(0xFF10B981) // Emerald Green
                MascotType.LLAMA -> Color(0xFFD4B483) // Tan/Beige
                MascotType.TIGRICH -> Color(0xFFF97316) // Orange Skin
                MascotType.PINGUCH -> Color(0xFF1E293B) // Dark Navy Blue
                MascotType.LUPO -> Color(0xFF475569) // Slate Gray
                MascotType.FOXY -> Color(0xFFF97316) // Fox Red
                MascotType.KOKO -> Color(0xFF94A3B8) // Slate Gray
                MascotType.DRACO -> Color(0xFF0D9488) // Cyber Teal
            }
            val secondaryColor = when(mascotType) {
                MascotType.POPPIN -> Color.White // Belly
                MascotType.KITTY -> Color(0xFFE5E7EB) // Lighter Gray Belly
                MascotType.PACO -> Color(0xFFFDBA74) // Lighter Orange Belly
                MascotType.PANDA -> Color(0xFF1F2937) // Panda Limbs/Ears are Black
                MascotType.CEBRIC -> Color(0xFF92400E) // Brown Spots
                MascotType.TURTLE -> Color(0xFF047857) // Dark Green Shell
                MascotType.LLAMA -> Color(0xFFFEF3C7) // Cream Wool
                MascotType.TIGRICH -> Color(0xFF1E293B) // Black Stripes
                MascotType.PINGUCH -> Color(0xFFF1F5F9) // Light Grey Belly
                MascotType.LUPO -> Color(0xFF818CF8) // Glowing Indigo
                MascotType.FOXY -> Color(0xFFFEF3C7) // Light Cream
                MascotType.KOKO -> Color(0xFFF1F5F9) // White/Light Gray Inner
                MascotType.DRACO -> Color(0xFFFBBF24) // Yellow/Gold
            }

            // --- DRAW BODY & HEAD ---
            when(mascotType) {
                MascotType.POPPIN -> {
                    drawPoppin(
                        pose = pose,
                        mainColor = mainColor,
                        secondaryColor = secondaryColor,
                        level = level,
                        isPremiumCrown = isPremium,
                        bodyOffset = bodyOffset,
                        breathe = breatheAnim,
                        jump = celebrationJump.value,
                        handWave = if (handWaveAnim.value != 0f) handWaveAnim.value else 0f,
                        eyeScale = eyeScale,
                        blinkExtra = 0f,
                        beakOpen = beakOpen,
                        armRotation = currentArmRotation,
                        whistleScale = poppinWhistle,
                        tickRotation = poppinTicking,
                        pupilOffsetX = 0f,
                        pupilOffsetY = 0f,
                        drawLevelAura = false,
                        eyePopScale = eyePopScale.value
                    )
                }
                MascotType.TURTLE -> {
                    drawTurtle(
                        pose = pose,
                        mainColor = mainColor,
                        secondaryColor = secondaryColor,
                        level = level,
                        isPremiumCrown = isPremium,
                        bodyOffset = bodyOffset,
                        breathe = breatheAnim,
                        headBobVal = if (pose == BrishPose.FOCUS) headBobAnim else 0f,
                        eyePanVal = if (pose == BrishPose.PLANNER) eyePanAnim else 0f,
                        liftVal = if (pose == BrishPose.HABITS) liftAnim else 0f,
                        armRotation = currentArmRotation,
                        eyePopScale = eyePopScale.value
                    )
                }
                MascotType.LLAMA -> {
                    drawLlama(
                        pose = pose,
                        mainColor = mainColor,
                        secondaryColor = secondaryColor,
                        level = level,
                        isPremiumCrown = isPremium,
                        bodyOffset = bodyOffset,
                        breathe = breatheAnim,
                        headBobVal = if (pose == BrishPose.FOCUS) headBobAnim else 0f,
                        eyePanVal = if (pose == BrishPose.PLANNER) eyePanAnim else 0f,
                        chewVal = if (pose == BrishPose.DEFAULT) breatheAnim else 0f,
                        armRotation = currentArmRotation,
                        eyePopScale = eyePopScale.value
                    )
                }
                MascotType.PACO -> {
                    drawPaco(
                        pose = pose,
                        mainColor = mainColor,
                        secondaryColor = secondaryColor,
                        level = level,
                        isPremiumCrown = isPremium,
                        bodyOffset = bodyOffset,
                        breathe = breatheAnim,
                        headBobVal = if (pose == BrishPose.FOCUS) headBobAnim else 0f,
                        eyePanVal = if (pose == BrishPose.PLANNER) eyePanAnim else 0f,
                        armRotation = currentArmRotation,
                        eyePopScale = eyePopScale.value
                    )
                }
                MascotType.PANDA -> {
                    drawPanda(
                        pose = pose,
                        mainColor = mainColor,
                        secondaryColor = secondaryColor,
                        level = level,
                        isPremiumCrown = isPremium,
                        bodyOffset = bodyOffset,
                        breathe = breatheAnim,
                        pandaHeadNod = pandaHeadNod,
                        eyePanVal = if (pose == BrishPose.PLANNER) eyePanAnim else 0f,
                        armRotation = currentArmRotation,
                        eyePopScale = eyePopScale.value
                    )
                }
                // Hidden mascots fallback to Poppin
                MascotType.KITTY,
                MascotType.CEBRIC,
                MascotType.TIGRICH,
                MascotType.PINGUCH,
                MascotType.LUPO,
                MascotType.FOXY,
                MascotType.KOKO,
                MascotType.DRACO -> {
                    drawPoppin(
                        pose = pose,
                        mainColor = mainColor,
                        secondaryColor = secondaryColor,
                        level = level,
                        isPremiumCrown = isPremium,
                        bodyOffset = bodyOffset,
                        breathe = breatheAnim,
                        jump = celebrationJump.value,
                        handWave = if (handWaveAnim.value != 0f) handWaveAnim.value else 0f,
                        eyeScale = eyeScale,
                        blinkExtra = 0f,
                        beakOpen = beakOpen,
                        armRotation = currentArmRotation,
                        whistleScale = poppinWhistle,
                        tickRotation = poppinTicking,
                        pupilOffsetX = 0f,
                        pupilOffsetY = 0f,
                        drawLevelAura = false,
                        eyePopScale = eyePopScale.value
                    )
                }
            }

            // --- LEVEL CROWN (Level 20+) ---
            if (level >= 20 && !isPremium) {
                val crownY = bodyOffset
                val crownPath = Path().apply {
                    moveTo(centerX - 15.dp.toPx(), 10.dp.toPx() + crownY)
                    lineTo(centerX - 15.dp.toPx(), 0.dp.toPx() + crownY)
                    lineTo(centerX - 5.dp.toPx(), 10.dp.toPx() + crownY)
                    lineTo(centerX, 0.dp.toPx() + crownY)
                    lineTo(centerX + 5.dp.toPx(), 10.dp.toPx() + crownY)
                    lineTo(centerX + 15.dp.toPx(), 0.dp.toPx() + crownY)
                    lineTo(centerX + 15.dp.toPx(), 10.dp.toPx() + crownY)
                    close()
                }
                drawPath(crownPath, Color(0xFFFFD700)) // Gold
            } else if (level >= 5 && !isPremium) {
                drawCircle(Color(0xFFCD7F32), radius = 5.dp.toPx(), center = Offset(size.width - 10.dp.toPx(), size.height - 10.dp.toPx()))
            }

            // --- CELEBRATION SPARKLES & CONFETTI ---
            if (starAlpha.value > 0.01f) {
                val baseRadius = 35.dp.toPx()
                val spreadRadius = baseRadius + (25.dp.toPx() * starSpread.value)

                val starCount = 6
                val starColor = Color(0xFFFFD700).copy(alpha = starAlpha.value)
                for (i in 0 until starCount) {
                    val angle = Math.toRadians(((i * 60) + starRotation.value).toDouble())
                    val sx = centerX + (spreadRadius * kotlin.math.cos(angle)).toFloat()
                    val sy = centerY + (spreadRadius * kotlin.math.sin(angle)).toFloat()
                    drawStar(
                        center = Offset(sx, sy),
                        points = 5,
                        innerRadius = 2.5.dp.toPx(),
                        outerRadius = 6.dp.toPx(),
                        rotationDegrees = starRotation.value + (i * 45f),
                        color = starColor
                    )
                }

                val confettiCount = 8
                val confettiRadius = spreadRadius * 0.7f
                for (i in 0 until confettiCount) {
                    val angle = Math.toRadians(((i * 45) + 22.5f - starRotation.value * 0.5f).toDouble())
                    val cx = centerX + (confettiRadius * kotlin.math.cos(angle)).toFloat()
                    val cy = centerY + (confettiRadius * kotlin.math.sin(angle)).toFloat()
                    val confettiColor = when (i % 3) {
                        0 -> Color(0xFFFF6B6B).copy(alpha = starAlpha.value * 0.8f)
                        1 -> Color(0xFF4ECDC4).copy(alpha = starAlpha.value * 0.8f)
                        else -> Color(0xFFFFE66D).copy(alpha = starAlpha.value * 0.8f)
                    }
                    drawCircle(confettiColor, radius = 3.dp.toPx(), center = Offset(cx, cy))
                }

                val innerCount = 4
                val innerRadius = spreadRadius * 0.4f
                for (i in 0 until innerCount) {
                    val angle = Math.toRadians(((i * 90) + starRotation.value * 1.5f).toDouble())
                    val ix = centerX + (innerRadius * kotlin.math.cos(angle)).toFloat()
                    val iy = centerY + (innerRadius * kotlin.math.sin(angle)).toFloat()
                    drawCircle(Color.White.copy(alpha = starAlpha.value * 0.6f), radius = 2.dp.toPx(), center = Offset(ix, iy))
                }
            }
        }
    }
}

@Composable
fun BrishGardeningScene(
    modifier: Modifier = Modifier,
    mascotType: MascotType = MascotType.POPPIN
) {
    Box(
        modifier = modifier.fillMaxWidth().height(350.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val hillPath = Path().apply {
                moveTo(0f, height) // Bottom left
                lineTo(0f, height * 0.7f) // Start of hill (left)
                quadraticTo(
                    width * 0.5f, height * 0.5f, // Control point (Peak of hill)
                    width, height * 0.7f // End of hill (right)
                )
                lineTo(width, height) // Bottom right
                close()
            }
            drawPath(path = hillPath, color = Color(0xFF4ADE80)) // Vibrant Green
            
            val flowerX = width * 0.75f
            val flowerY = height * 0.65f // On the hill slope roughly
            
            // Stem
            drawLine(color = Color(0xFF166534), start = Offset(flowerX, flowerY), end = Offset(flowerX, flowerY - 40.dp.toPx()), strokeWidth = 4.dp.toPx())
            
            // Petals
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(flowerX - 8.dp.toPx(), flowerY - 45.dp.toPx()))
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(flowerX + 8.dp.toPx(), flowerY - 45.dp.toPx()))
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(flowerX, flowerY - 53.dp.toPx()))
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(flowerX, flowerY - 37.dp.toPx()))
            // Center
            drawCircle(color = Color(0xFFFACC15), radius = 6.dp.toPx(), center = Offset(flowerX, flowerY - 45.dp.toPx()))
        }
        
        // --- MASCOT ---
        // Positioned on the hill
        BrishMascotAnimation(
            modifier = Modifier
                .size(160.dp)
                .offset(x = (-40).dp, y = (-80).dp), // Adjust to stand on hill
            pose = BrishPose.GARDENING,
            mascotType = mascotType
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTriangle(color: Color, center: Offset, size: Float, pointingDown: Boolean = false) {
    val path = Path().apply {
        if (pointingDown) {
            moveTo(center.x - size, center.y - size)
            lineTo(center.x + size, center.y - size)
            lineTo(center.x, center.y + size)
        } else {
            moveTo(center.x, center.y - size)
            lineTo(center.x - size, center.y + size)
            lineTo(center.x + size, center.y + size)
        }
        close()
    }
    drawPath(path, color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
    center: Offset,
    points: Int = 5,
    innerRadius: Float,
    outerRadius: Float,
    rotationDegrees: Float,
    color: Color
) {
    rotate(degrees = rotationDegrees, pivot = center) {
        val path = Path()
        val angleIncrement = Math.PI / points
        for (i in 0 until (2 * points)) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = i * angleIncrement - Math.PI / 2
            val x = center.x + r * kotlin.math.cos(angle).toFloat()
            val y = center.y + r * kotlin.math.sin(angle).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        drawPath(path, color)
    }
}
