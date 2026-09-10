package com.katchy.focuslive.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.R
import com.katchy.focuslive.ui.theme.AntiPrimary
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.bounceClick
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val isDark = isDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF101216) else Color(0xFFFAFAFC))
            .systemBarsPadding()
    ) {
        // --- Skip Button ---
        if (pagerState.currentPage < 2) {
            TextButton(
                onClick = onFinish,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Omitir",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF6B7280)
                )
            }
        } else {
            Spacer(Modifier.height(64.dp))
        }

        // --- Horizontal Pager ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { page ->
            when (page) {
                0 -> OnboardingStepPage(
                    icon = null,
                    isLogo = true,
                    tag = "TU ESPACIO",
                    title = "Estudia con claridad",
                    description = "Tus cursos, entregas y apuntes juntos. Encuentra lo importante y decide tu siguiente paso.",
                    isDark = isDark
                )
                1 -> OnboardingStepPage(
                    icon = Icons.Rounded.HourglassTop,
                    isLogo = false,
                    tag = "ENFOQUE",
                    title = "Una cosa a la vez",
                    description = "Elige una tarea, inicia tu sesión de enfoque y reserva tiempo para descansar. Avanza a tu ritmo.",
                    isDark = isDark
                )
                2 -> OnboardingStepPage(
                    icon = Icons.Rounded.School,
                    isLogo = false,
                    tag = "A TU RITMO",
                    title = "Haz espacio para avanzar",
                    description = "Planifica en Agenda, construye tus hábitos y consulta a Brish IA cuando necesites ayuda para organizarte.",
                    isDark = isDark
                )
            }
        }

        // --- Bottom Controls ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicator Dots
            CleanPagerIndicator(
                pagerState = pagerState,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button (Solid Pill)
            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = pagerState.currentPage + 1,
                                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                            )
                        }
                    } else {
                        onFinish()
                    }
                },
                shape = RoundedCornerShape(100.dp),
                enabled = !pagerState.isScrollInProgress,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color.White else Color(0xFF111827),
                    contentColor = if (isDark) Color.Black else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick()
            ) {
                Text(
                    text = if (pagerState.currentPage < 2) "Continuar" else "Entrar a Brish",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepPage(
    icon: ImageVector?,
    isLogo: Boolean,
    tag: String,
    title: String,
    description: String,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Feature Icon Container
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(if (isDark) Color(0xFF1C1E24) else Color(0xFFEEF2F6))
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0),
                    RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLogo) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_app_logo_vector),
                    contentDescription = "Brish Logo",
                    tint = if (isDark) Color.White else Color(0xFF111827),
                    modifier = Modifier.size(46.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isDark) Color.White else Color(0xFF111827),
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Tag Pill
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = if (isDark) Color(0xFF262C38) else Color(0xFFE8EEF8)
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = if (isDark) Color.White else Color(0xFF1E3A8A),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            color = if (isDark) Color.White else Color(0xFF111827),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280),
            lineHeight = 24.sp,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CleanPagerIndicator(
    pagerState: PagerState,
    isDark: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pagerState.pageCount) { index ->
            val isSelected = pagerState.currentPage == index
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "indicatorWidth"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        if (isSelected) {
                            if (isDark) Color.White else Color(0xFF111827)
                        } else {
                            if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFD1D5DB)
                        }
                    )
            )
        }
    }
}
