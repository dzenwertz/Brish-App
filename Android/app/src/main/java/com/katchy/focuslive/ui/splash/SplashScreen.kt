package com.katchy.focuslive.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katchy.focuslive.BuildConfig
import com.katchy.focuslive.R
import com.katchy.focuslive.ui.components.brishSharedElement
import com.katchy.focuslive.ui.theme.BrishCream
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.BrishYellowDeep
import com.katchy.focuslive.ui.theme.MotionTokens
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashFinished: () -> Unit, quickStart: Boolean = false) {
    val logoScale = remember { Animatable(0.82f) }
    val logoAlpha = remember { Animatable(0f) }
    val wordReveal = remember { Animatable(0f) }
    val cornerProgress = remember { Animatable(0f) }
    val reduced = rememberReducedMotion()
    val dark = isDarkTheme()
    val latestOnFinished by rememberUpdatedState(onSplashFinished)

    LaunchedEffect(Unit) {
        if (reduced) {
            logoScale.snapTo(1f)
            logoAlpha.snapTo(1f)
            wordReveal.snapTo(1f)
            cornerProgress.snapTo(1f)
        } else {
            launch { logoScale.animateTo(1f, tween(220, easing = MotionTokens.overshoot)) }
            launch { logoAlpha.animateTo(1f, tween(220, easing = MotionTokens.standard)) }
            launch { cornerProgress.animateTo(1f, tween(MotionTokens.componentMs, easing = MotionTokens.emphasized)) }
            delay(80)
            wordReveal.animateTo(1f, tween(280, easing = MotionTokens.emphasized))
        }
        delay(if (quickStart) 80 else 220)
        latestOnFinished()
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(if (dark) MaterialTheme.colorScheme.background else BrishCream),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(196.dp).offset(
                x = maxWidth * 0.42f * (1f - cornerProgress.value),
                y = -maxHeight * 0.38f * (1f - cornerProgress.value)
            ).align(Alignment.TopEnd).clip(CircleShape).background(BrishYellowDeep)
        )
        Box(
            Modifier.size(246.dp).offset(
                x = -maxWidth * 0.38f * (1f - cornerProgress.value),
                y = maxHeight * 0.4f * (1f - cornerProgress.value)
            ).align(Alignment.BottomStart).clip(CircleShape).background(BrishYellow)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_brish_logo),
                contentDescription = "Logo de Brish",
                modifier = Modifier.size(88.dp).scale(logoScale.value).then(
                    Modifier.graphicsLayer { alpha = logoAlpha.value }.brishSharedElement("brish-logo")
                )
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Brish",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp, letterSpacing = (-1).sp),
                fontWeight = FontWeight.SemiBold,
                color = if (dark) MaterialTheme.colorScheme.onBackground else BrishInk,
                modifier = Modifier.drawWithContent {
                    clipRect(top = size.height * (1f - wordReveal.value), bottom = size.height) {
                        this@drawWithContent.drawContent()
                    }
                }
            )
        }
        Text(
            "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp)
        )
    }
}
