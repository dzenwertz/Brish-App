package com.katchy.focuslive.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katchy.focuslive.BuildConfig
import com.katchy.focuslive.R
import androidx.compose.ui.graphics.Brush
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.BrishYellowLight
import com.katchy.focuslive.ui.theme.isDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.88f) }
    val alpha = remember { Animatable(0f) }
    val isDark = isDarkTheme()
    val latestOnFinished by rememberUpdatedState(onSplashFinished)

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = 1f, stiffness = 400f)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
        }
        delay(500)
        latestOnFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
                .padding(24.dp)
        ) {
            // Brand Logo Container
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.ic_brish_logo),
                contentDescription = "Logo de Brish",
                modifier = Modifier
                    .size(96.dp)
                    .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = Color(0xFFD6A014), spotColor = Color(0xFFD6A014))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Brand Title
            Text(
                text = "Brish",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Minimal Subtitle
            Text(
                text = "Tu día, tu semana y tus ideas.",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        )
    }
}
