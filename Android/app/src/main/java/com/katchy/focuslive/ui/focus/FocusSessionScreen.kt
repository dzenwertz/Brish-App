package com.katchy.focuslive.ui.focus

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katchy.focuslive.data.manager.TimerManager
import com.katchy.focuslive.ui.components.BrishCardSurface
import com.katchy.focuslive.ui.components.BrishCelebrationBurst
import com.katchy.focuslive.ui.components.BrishChoiceChip
import com.katchy.focuslive.ui.components.BrishPrimaryButton
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.BrishSecondaryButton
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.components.areaStyle
import com.katchy.focuslive.ui.components.brishSharedBounds
import com.katchy.focuslive.ui.home.HomeViewModel
import com.katchy.focuslive.ui.home.planToday
import com.katchy.focuslive.ui.theme.BrishButtonBrush
import com.katchy.focuslive.ui.theme.BrishGoldShadow
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishOrbCoral
import com.katchy.focuslive.ui.theme.BrishWarning
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.BrishYellowDeep
import com.katchy.focuslive.ui.theme.BrishYellowLight
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.brishCascade
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import com.katchy.focuslive.ui.util.bounceClick
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionScreen(
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val remaining by viewModel.timeLeft.collectAsState()
    val running by viewModel.isTimerRunning.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val work by viewModel.workDuration.collectAsState()
    val rest by viewModel.breakDuration.collectAsState()
    val zen by viewModel.isZenModeEnabled.collectAsState()
    val sound by viewModel.ambientSound.collectAsState()
    val customSoundUri by viewModel.customSoundUri.collectAsState()
    val activeTaskId by viewModel.activeTaskId.collectAsState()
    val homeState by viewModel.homeState.collectAsState()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val reduced = rememberReducedMotion()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var requestDnd by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var celebrationTrigger by remember { mutableIntStateOf(0) }
    val drag = remember { Animatable(0f) }

    val customSoundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.setCustomSoundUri(uri.toString())
            viewModel.setAmbientSound(TimerManager.AmbientSound.CUSTOM)
        }
    }
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume() }
        owner.lifecycle.addObserver(observer)
        viewModel.onResume()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(viewModel) { viewModel.zenModeEffect.collect { requestDnd = true } }
    LaunchedEffect(viewModel) {
        viewModel.sessionCompletedEvent.collect {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            celebrationTrigger++
        }
    }

    val isWork = timerState == TimerManager.TimerState.WORK
    val sessionSeconds = ((if (isWork) work else rest) * 60L).coerceAtLeast(1L)
    val total = maxOf(sessionSeconds, remaining)
    val progress by animateFloatAsState((1f - remaining.toFloat() / total).coerceIn(0f, 1f), tween(600), label = "ring")
    val activeTask = homeState.focusTasks.firstOrNull { it.id == activeTaskId }
    val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
    val nextUp = remember(homeState.tasks, activeTaskId, nowMinutes) {
        planToday(homeState.tasks, nowMinutes, todayMillis = System.currentTimeMillis()).next.firstOrNull { it.id != activeTaskId }
    }
    val dismissPx = with(density) { 160.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .brishSharedBounds("focus-hero", RoundedCornerShape(0.dp))
            .offset { IntOffset(0, drag.value.roundToInt()) }
            .graphicsLayer { alpha = 1f - (drag.value / (dismissPx * 3)).coerceIn(0f, 0.4f) }
            .background(MaterialTheme.colorScheme.background)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> scope.launch { drag.snapTo((drag.value + delta).coerceAtLeast(0f)) } },
                onDragStopped = { velocity ->
                    if (drag.value > dismissPx || velocity > 2000f) onNavigateBack() else drag.animateTo(0f, BrishMotion.lively())
                }
            )
    ) {
        AmbientOrbs(active = running && !reduced)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrishRoundButton(Icons.Rounded.Close, "Cerrar enfoque", onNavigateBack, size = 48.dp)
                Text(
                    if (isWork) "ENFOQUE" else "DESCANSO",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    Modifier.height(48.dp).brishShadow(CircleShape, elevation = 8.dp, strength = 0.8f).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface).clickable(role = Role.Button) { showSettings = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    Text(soundLabel(sound), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (activeTask != null) {
                    val area = areaStyle(activeTask.category)
                    val time = listOfNotNull(activeTask.time, activeTask.endTime).joinToString(" – ")
                    Text(listOf(area.name, time).filter { it.isNotBlank() }.joinToString(" · "), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrishWarning)
                    Text(activeTask.title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, lineHeight = 32.sp), color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                    activeTask.subtasks.firstOrNull { !it.isCompleted }?.let {
                        Text("Ahora: ${it.title}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                } else {
                    Text(if (isWork) "Una cosa a la vez" else "Recarga un momento", style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp), color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        if (isWork) "Elige en qué trabajar ›" else "Respira, estira, toma agua.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clip(CircleShape).clickable(enabled = isWork) { showSettings = true }.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            BrishCelebrationBurst(trigger = celebrationTrigger) {
                TimerRing(progress = progress, remaining = remaining, sessionMinutes = sessionSeconds / 60, glowing = running && !reduced)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                SoftCircleButton("+5", "Añadir 5 minutos") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.addFiveMinutes() }
                Box(
                    Modifier.size(92.dp)
                        .brishShadow(CircleShape, elevation = 18.dp, tone = BrishGoldShadow, strength = if (running) 2f else 1.4f)
                        .clip(CircleShape).background(BrishButtonBrush).brishEdgeHighlight(inset = 22.dp)
                        .bounceClick { haptics.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.toggleTimer() }
                        .semantics { contentDescription = if (running) "Pausar" else "Iniciar" },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(running, transitionSpec = { (scaleIn(initialScale = 0.85f) + fadeIn(tween(160))) togetherWith fadeOut(tween(100)) }, label = "playPause") { r ->
                        Icon(if (r) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = BrishInk, modifier = Modifier.size(38.dp))
                    }
                }
                BrishCelebrationBurst(trigger = celebrationTrigger) {
                    SoftCircleButton("Fin", if (isWork) "Terminar y descansar" else "Terminar descanso") {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isWork) celebrationTrigger++
                        viewModel.skipSession()
                    }
                }
            }

            if (activeTask != null && activeTask.subtasks.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().brishCascade(2).brishCard().padding(horizontal = 18.dp, vertical = 6.dp)) {
                    activeTask.subtasks.forEach { sub ->
                        SubtaskRow(sub.title, sub.isCompleted) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleSubtask(activeTask, sub.id)
                        }
                    }
                }
            }

            nextUp?.let {
                Text(
                    "Siguiente: ${it.title}${it.time?.let { t -> " a las $t" } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Text("Desliza hacia abajo para cerrar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("Tu sesión", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                SheetLabel("¿En qué te enfocas?")
                ChipRow {
                    BrishChoiceChip("Sin tarea", activeTaskId == null, { viewModel.setActiveTask(null) })
                    homeState.focusTasks.take(12).forEach { task ->
                        BrishChoiceChip(task.title, activeTaskId == task.id, { viewModel.setActiveTask(task.id) })
                    }
                }
                SheetLabel("Sonido")
                ChipRow {
                    TimerManager.AmbientSound.entries.filter { it != TimerManager.AmbientSound.CUSTOM }.forEach { s ->
                        BrishChoiceChip(soundLabel(s), sound == s, { viewModel.setAmbientSound(s) })
                    }
                    if (customSoundUri != null) {
                        BrishChoiceChip("Mi sonido", sound == TimerManager.AmbientSound.CUSTOM, { viewModel.setAmbientSound(TimerManager.AmbientSound.CUSTOM) })
                    }
                    BrishChoiceChip("Elegir archivo…", false, { customSoundPicker.launch(arrayOf("audio/*")) })
                }
                SheetLabel("Duración")
                Stepper("Enfoque", work, enabled = !running) { viewModel.setWorkDuration(it) }
                Stepper("Descanso", rest, enabled = !running) { viewModel.setBreakDuration(it) }
                if (running) Text("Pausa para cambiar la duración.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().brishCard().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("No molestar", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("Silencia notificaciones mientras te enfocas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(zen, { viewModel.toggleZenMode() }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = androidx.compose.ui.graphics.Color.White, checkedBorderColor = androidx.compose.ui.graphics.Color.Transparent))
                }
                Text(
                    "Reiniciar el temporizador",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button) { viewModel.resetTimer(); showSettings = false }.padding(horizontal = 6.dp, vertical = 8.dp)
                )
            }
        }
    }

    if (requestDnd) {
        SmoothDialog(onDismissRequest = { requestDnd = false }) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("Permitir No molestar", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Activa el acceso de Brish a No molestar para silenciar interrupciones durante el enfoque.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton(
                        text = "Cancelar",
                        onClick = { requestDnd = false },
                        modifier = Modifier.weight(1f)
                    )
                    BrishPrimaryButton(
                        text = "Abrir ajustes",
                        onClick = {
                            requestDnd = false
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Dos orbes cálidos que derivan despacio solo mientras corre el tiempo. Degradados radiales, sin desenfoque real. */
@Composable
private fun AmbientOrbs(active: Boolean) {
    val t = rememberInfiniteTransition(label = "orbs")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
    val shown by animateFloatAsState(if (active) 1f else 0.45f, tween(900), label = "orbAlpha")
    Canvas(Modifier.fillMaxSize()) {
        val d = if (active) drift else 0.5f
        val c1 = Offset(size.width * (0.05f + 0.08f * d), size.height * (0.32f - 0.03f * d))
        val c2 = Offset(size.width * (1.0f - 0.07f * d), size.height * (0.5f + 0.04f * d))
        drawCircle(Brush.radialGradient(listOf(com.katchy.focuslive.ui.theme.BrishAccentSoft.copy(alpha = 0.42f * shown), Color.Transparent), c1, size.width * 0.55f), size.width * 0.55f, c1)
        drawCircle(Brush.radialGradient(listOf(BrishOrbCoral.copy(alpha = 0.30f * shown), Color.Transparent), c2, size.width * 0.5f), size.width * 0.5f, c2)
    }
}

@Composable
private fun TimerRing(progress: Float, remaining: Long, sessionMinutes: Long, glowing: Boolean) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val face = MaterialTheme.colorScheme.surface
    val breathe = rememberInfiniteTransition(label = "halo")
    val halo by breathe.animateFloat(0.55f, 0.92f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "haloAlpha")
    val ringScale by breathe.animateFloat(0.988f, 1.022f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "ringScale")
    val strokePulse by breathe.animateFloat(0f, 2.5f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "strokePulse")
    val activeScale = if (glowing) ringScale else 1f
    Box(
        Modifier
            .size(280.dp)
            .graphicsLayer { scaleX = activeScale; scaleY = activeScale }
            .semantics { contentDescription = "Quedan ${remaining / 60} minutos de $sessionMinutes" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val baseStroke = 14.dp.toPx()
            val activeStroke = baseStroke + if (glowing) strokePulse.dp.toPx() else 0f
            val r = size.minDimension / 2 - baseStroke - 4.dp.toPx()
            if (glowing) {
                drawCircle(Brush.radialGradient(listOf(com.katchy.focuslive.ui.theme.BrishAccentSoft.copy(alpha = 0.55f * halo), Color.Transparent), center, r * 1.08f), r * 1.08f, center)
            }
            drawCircle(face, r, center)
            drawCircle(track, r, center, style = Stroke(baseStroke))
            drawArc(
                brush = Brush.sweepGradient(listOf(BrishYellowLight, BrishYellowDeep, BrishYellowLight), center),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(activeStroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Odometer(String.format("%02d:%02d", remaining / 60, remaining % 60))
            Text("de $sessionMinutes min", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Cada dígito rueda hacia arriba cuando cambia. */
@Composable
private fun Odometer(text: String) {
    Row {
        text.forEachIndexed { i, ch ->
            AnimatedContent(
                targetState = ch,
                transitionSpec = { slideInVertically(BrishMotion.soft()) { -it } + fadeIn() togetherWith slideOutVertically(BrishMotion.soft()) { it } + fadeOut() },
                label = "digit$i"
            ) { c ->
                Text(c.toString(), fontSize = 62.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-2).sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SoftCircleButton(label: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(72.dp).brishShadow(CircleShape, elevation = 8.dp, strength = 0.8f).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant).bounceClick(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SubtaskRow(title: String, done: Boolean, onToggle: () -> Unit) {
    val bg by animateColorAsState(if (done) BrishYellow else Color.Transparent, BrishMotion.lively(), label = "sub")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics { stateDescription = if (done) "Hecha" else "Pendiente" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BrishCelebrationBurst(active = done) {
            Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(bg).border(2.dp, if (done) BrishYellow else MaterialTheme.colorScheme.outline, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center) {
                if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = BrishInk, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = if (done) FontWeight.Medium else FontWeight.Bold,
            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (done) TextDecoration.LineThrough else null
        )
    }
}

@Composable
private fun SheetLabel(text: String) = Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun Stepper(label: String, minutes: Long, enabled: Boolean, onChange: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().brishCard().padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        StepButton("−", enabled && minutes > 5) { onChange((minutes - 5).coerceAtLeast(5)) }
        Text("$minutes min", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(76.dp), textAlign = TextAlign.Center)
        StepButton("+", enabled && minutes < 120) { onChange((minutes + 5).coerceAtMost(120)) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f))
    }
}

private fun soundLabel(sound: TimerManager.AmbientSound): String = when (sound) {
    TimerManager.AmbientSound.NONE -> "Silencio"
    TimerManager.AmbientSound.RAIN -> "Lluvia"
    TimerManager.AmbientSound.FIRE -> "Fuego"
    TimerManager.AmbientSound.CAFE -> "Café"
    TimerManager.AmbientSound.AMBIENT -> "Ambiente"
    TimerManager.AmbientSound.OCEAN -> "Océano"
    TimerManager.AmbientSound.CUSTOM -> "Mi sonido"
}
