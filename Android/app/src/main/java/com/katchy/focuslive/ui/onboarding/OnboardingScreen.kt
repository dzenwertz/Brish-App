package com.katchy.focuslive.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.ui.components.BrishArt
import com.katchy.focuslive.ui.components.BrishArtKind
import com.katchy.focuslive.ui.components.BrishTextField
import com.katchy.focuslive.ui.components.DraftRecoveryNotice
import com.katchy.focuslive.ui.components.brishSharedElement
import com.katchy.focuslive.ui.components.rememberPersistentDraftText
import com.katchy.focuslive.ui.theme.BrishButtonBrush
import com.katchy.focuslive.ui.theme.BrishGoldShadow
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.MotionTokens
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private data class Step(val art: BrishArtKind, val tag: String, val title: String, val body: String)

private val Steps = listOf(
    Step(BrishArtKind.Sun, "BIENVENIDA", "Tu día, tu semana y tus ideas", "Brish junta tu horario, tus notas y tus hábitos en un solo lugar, sin ruido."),
    Step(BrishArtKind.Capture, "CAPTURA", "Guarda todo en segundos", "Toca ＋ y escribe como hablas: «gym mañana 7am #salud». Brish detecta la fecha, la hora y el área."),
    Step(BrishArtKind.Calendar, "AGENDA", "Tu horario, claro", "Mira tu día o tu semana completa, encuentra huecos libres y desliza entre pestañas con el dedo."),
    Step(BrishArtKind.Plant, "HÁBITOS", "Constancia sin presión", "Metas por semana y contadores simples. Sin rachas ni puntos: solo tu progreso.")
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onRegister: () -> Unit = onFinish,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { Steps.size })
    val scope = rememberCoroutineScope()
    val reduced = rememberReducedMotion()
    val focus = LocalFocusManager.current
    val nameDraft = rememberPersistentDraftText("onboarding:name")
    var name by nameDraft::value
    val last = pagerState.currentPage == Steps.lastIndex

    fun finish() {
        focus.clearFocus()
        if (name.isNotBlank()) viewModel.saveUserName(name.trim())
        nameDraft.clearAfterSave()
        onFinish()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(com.katchy.focuslive.R.drawable.ic_brish_logo),
                contentDescription = "Logo de Brish",
                modifier = Modifier.padding(start = 8.dp, end = 8.dp).size(30.dp).brishSharedElement("brish-logo")
            )
            Text(
                "Brish",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 12.dp).weight(1f)
            )
            TextButton(onClick = { finish() }, enabled = !last) {
                Text(if (last) "" else "Omitir", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
            val step = Steps[page]
            val isCurrent = page == pagerState.currentPage
            var pageEntered by remember(page) { mutableStateOf(false) }
            LaunchedEffect(isCurrent) {
                pageEntered = isCurrent
            }
            val artScale by animateFloatAsState(
                targetValue = if (pageEntered) 1f else 0.6f,
                animationSpec = if (reduced) tween(MotionTokens.reducedMotionMs)
                else tween(MotionTokens.screenMs, easing = MotionTokens.overshoot),
                label = "onboardingArtScale"
            )
            val slidePx = with(LocalDensity.current) { 24.dp.roundToPx() }
            // Paralaje: la ilustración se mueve más lento que el texto al deslizar.
            val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BrishArt(
                    step.art,
                    Modifier
                        .size(240.dp)
                        .scale(artScale)
                        .graphicsLayer {
                            if (!reduced) {
                                translationX = offset * size.width * 0.35f
                                alpha = 1f - offset.absoluteValue.coerceIn(0f, 1f) * 0.6f
                            }
                        }
                )
                Spacer(Modifier.height(28.dp))
                Box(
                    Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(step.tag, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.height(14.dp))
                AnimatedContent(
                    targetState = step.title,
                    transitionSpec = {
                        if (reduced) {
                            fadeIn(tween(MotionTokens.reducedMotionMs)) togetherWith fadeOut(tween(MotionTokens.reducedMotionMs))
                        } else {
                            (slideInHorizontally(tween(MotionTokens.componentMs, easing = MotionTokens.emphasized)) { slidePx } +
                                fadeIn(tween(MotionTokens.componentMs))) togetherWith
                                (slideOutHorizontally(tween((MotionTokens.componentMs * MotionTokens.exitRatio).toInt(), easing = MotionTokens.exit)) { -slidePx } +
                                    fadeOut(tween((MotionTokens.componentMs * MotionTokens.exitRatio).toInt())))
                        }
                    },
                    label = "onboardingTitle"
                ) { title ->
                    Text(title, style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp, lineHeight = 36.sp), color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(10.dp))
                AnimatedContent(
                    targetState = step.body,
                    transitionSpec = { fadeIn(tween(MotionTokens.componentMs)) togetherWith fadeOut(tween((MotionTokens.componentMs * MotionTokens.exitRatio).toInt())) },
                    label = "onboardingSubtitle"
                ) { body ->
                    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                if (page == Steps.lastIndex) {
                    Spacer(Modifier.height(22.dp))
                    DraftRecoveryNotice(nameDraft, Modifier.fillMaxWidth())
                    BrishTextField(
                        value = name,
                        onValueChange = { name = it.take(30) },
                        singleLine = true,
                        label = "¿Cómo te llamas? (opcional)",
                        shape = BrishShapes.pill,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { finish() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StretchIndicator(pagerState)
            PrimaryButton(
                label = if (last) "Entrar a mi cuenta" else "Siguiente",
                highlighted = false,
                onClick = {
                    if (last) finish()
                    else scope.launch {
                        if (reduced) pagerState.scrollToPage(pagerState.currentPage + 1)
                        else pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = MotionTokens.sheetSpring())
                    }
                }
            )
            TextButton(onClick = {
                if (name.isNotBlank()) viewModel.saveUserName(name.trim())
                nameDraft.clearAfterSave()
                onRegister()
            }) {
                Text("¿No tienes cuenta? Regístrate", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Puntos donde el activo se estira hacia el siguiente mientras arrastras. */
@Composable
private fun StretchIndicator(state: PagerState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(state.pageCount) { i ->
            val distance = ((state.currentPage + state.currentPageOffsetFraction) - i).absoluteValue.coerceAtMost(1f)
            val width = 8.dp + 22.dp * (1f - distance)
            val color by animateColorAsState(if (distance < 0.5f) BrishInk else MaterialTheme.colorScheme.outlineVariant, label = "dot")
            Box(Modifier.width(width).height(8.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun PrimaryButton(label: String, highlighted: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) BrishMotion.PressScale else 1f, BrishMotion.lively(), label = "press")
    val ink = Brush.verticalGradient(listOf(BrishInk, BrishInk))
    Box(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale)
            .brishShadow(BrishShapes.pill, elevation = 14.dp, tone = if (highlighted) BrishGoldShadow else BrishInk, strength = 1.3f)
            .clip(BrishShapes.pill)
            .background(if (highlighted) BrishButtonBrush else ink)
            .brishEdgeHighlight(alpha = if (highlighted) 0.7f else 0.12f)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(label, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "cta") { text ->
            Text(text, color = if (highlighted) BrishInk else Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
