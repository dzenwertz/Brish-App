package com.katchy.focuslive.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.ui.components.BrishArt
import com.katchy.focuslive.ui.components.BrishArtKind
import com.katchy.focuslive.ui.components.BrishPill
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.DraftRecoveryNotice
import com.katchy.focuslive.ui.components.areaStyle
import com.katchy.focuslive.ui.components.rememberPersistentDraftText
import com.katchy.focuslive.ui.components.BrishTextField
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.BrishButtonBrush
import com.katchy.focuslive.ui.theme.BrishGoldShadow
import com.katchy.focuslive.ui.theme.BrishHeroBrush
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishOnYellow
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.brishCascade
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import com.katchy.focuslive.ui.util.bounceClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Spanish = Locale("es", "PE")

@Composable
fun WeeklyReviewScreen(
    onClose: () -> Unit,
    onPlanWeek: () -> Unit,
    viewModel: WeeklyReviewViewModel = hiltViewModel()
) {
    val review by viewModel.review.collectAsState()
    val error by viewModel.error.collectAsState()
    var step by rememberSaveable { mutableStateOf(0) }
    val reduced = rememberReducedMotion()
    BackHandler { if (step > 0) step-- else onClose() }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrishRoundButton(Icons.Rounded.Close, "Cerrar revisión", onClose, size = 48.dp)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.semantics { contentDescription = "Paso ${step + 1} de 3" }) {
                repeat(3) { i ->
                    val color by animateColorAsState(
                        when {
                            i < step -> BrishInk
                            i == step -> BrishYellow
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }, label = "step"
                    )
                    Box(Modifier.width(28.dp).height(7.dp).clip(CircleShape).background(color))
                }
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }
        val r = review
        if (r == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { BrishArt(BrishArtKind.Calendar, Modifier.size(140.dp)) }
            return@Column
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (reduced) fadeIn() togetherWith fadeOut() else {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(BrishMotion.soft()) { it / 3 * dir } + fadeIn()) togetherWith (slideOutHorizontally(BrishMotion.soft()) { -it / 3 * dir } + fadeOut())
                }
            },
            modifier = Modifier.weight(1f),
            label = "reviewStep"
        ) { s ->
            when (s) {
                0 -> SummaryStep(r)
                1 -> LeftoversStep(r.leftovers, onToday = viewModel::moveToToday, onLetGo = viewModel::letGo)
                else -> NextWeekStep(r)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
        PrimaryAction(
            label = when (step) { 0 -> "Ver pendientes"; 1 -> "Mirar la próxima semana"; else -> "Planificar en Agenda" },
            onClick = { if (step < 2) step++ else onPlanWeek() }
        )
    }
}

@Composable
private fun StepTitle(overline: String, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(overline, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp, lineHeight = 36.sp), color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun SummaryStep(r: WeeklyReview) {
    val fmt = SimpleDateFormat("d MMM", Spanish)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepTitle("Del ${fmt.format(Date(r.days.first())).replace(".", "")} al ${fmt.format(Date(r.days.last())).replace(".", "")}", "Así fue tu semana")
        Row(Modifier.brishCascade(0), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(
                Modifier.weight(1f).brishShadow(BrishShapes.card, elevation = 14.dp, tone = BrishGoldShadow, strength = 1.4f)
                    .clip(BrishShapes.card).background(BrishHeroBrush).brishEdgeHighlight().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CountUp(r.done, suffix = "/${r.total}")
                Text("bloques cumplidos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrishOnYellow)
            }
            Column(Modifier.weight(1f).brishCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (r.habitPercent != null) CountUp(r.habitPercent, suffix = "%", color = MaterialTheme.colorScheme.onSurface)
                else Text("—", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Text("de tus hábitos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.fillMaxWidth().brishCascade(1).brishCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Horas por área", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (r.minutesByArea.isEmpty()) {
                Text("Cuando completes bloques con hora, aquí verás en qué se fue tu tiempo.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val max = r.minutesByArea.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
            r.minutesByArea.forEachIndexed { i, (area, minutes) ->
                AreaBar(area, minutes, minutes.toFloat() / max, delayMs = 90L * i)
            }
        }
        val weekKey = "week-${r.days.firstOrNull() ?: 0L}"
        ReflectionCard(weekKey = weekKey, modifier = Modifier.brishCascade(2))
    }
}

@Composable
private fun CountUp(value: Int, suffix: String, color: Color = BrishInk) {
    var target by remember { mutableStateOf(0) }
    LaunchedEffect(value) { target = value }
    val shown by animateFloatAsState(target.toFloat(), tween(700), label = "count")
    Text("${shown.roundToInt()}$suffix", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp, color = color)
}

@Composable
private fun AreaBar(area: String, minutes: Int, fraction: Float, delayMs: Long) {
    val style = areaStyle(area)
    var grow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs); grow = true }
    val f by animateFloatAsState(if (grow) fraction else 0f, BrishMotion.soft(), label = "bar")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(area, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(72.dp), maxLines = 1)
        Box(Modifier.weight(1f).height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape).background(style.content))
        }
        Text(if (minutes >= 60) "${minutes / 60} h" else "$minutes min", fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun LeftoversStep(leftovers: List<Task>, onToday: (Task) -> Unit, onLetGo: (Task) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepTitle(if (leftovers.isEmpty()) "Nada quedó atrás" else "${leftovers.size} sin terminar", "Quedaron pendientes")
        if (leftovers.isEmpty()) {
            Column(Modifier.fillMaxWidth().brishCard().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BrishArt(BrishArtKind.Plant, Modifier.size(120.dp))
                Text("Cerraste todo lo que planeaste. Buen trabajo.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            return@Column
        }
        Text(
            "Desliza a la derecha para pasarlo a hoy, a la izquierda para soltarlo (se elimina).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        CardStack(
            tasks = leftovers,
            onRight = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onToday(it) },
            onLeft = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLetGo(it) }
        )
    }
}

/** Pila de tarjetas con física: la de arriba sigue al dedo, gira un poco y sale volando al soltarla. */
@Composable
private fun CardStack(tasks: List<Task>, onRight: (Task) -> Unit, onLeft: (Task) -> Unit) {
    val top = tasks.first()
    val scope = rememberCoroutineScope()
    val offset = remember(top.id) { Animatable(0f) }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth().height(230.dp)) {
        val width = with(density) { maxWidth.toPx() }
        val threshold = width * 0.32f
        tasks.getOrNull(1)?.let { next ->
            val lift = (abs(offset.value) / threshold).coerceIn(0f, 1f)
            LeftoverCard(next, Modifier.graphicsLayer {
                val s = 0.94f + 0.06f * lift
                scaleX = s; scaleY = s
                translationY = (1f - lift) * 18.dp.toPx()
                alpha = 0.7f + 0.3f * lift
            })
        }
        val hint = (offset.value / threshold).coerceIn(-1f, 1f)
        LeftoverCard(
            top,
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .graphicsLayer { rotationZ = offset.value / width * 14f }
                .pointerInput(top.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offset.value > threshold -> { offset.animateTo(width * 1.4f, tween(220)); onRight(top) }
                                    offset.value < -threshold -> { offset.animateTo(-width * 1.4f, tween(220)); onLeft(top) }
                                    else -> offset.animateTo(0f, BrishMotion.lively())
                                }
                            }
                        },
                        onHorizontalDrag = { change, amount -> change.consume(); scope.launch { offset.snapTo(offset.value + amount) } }
                    )
                },
            hint = hint,
            onRight = { scope.launch { offset.animateTo(width * 1.4f, tween(260)); onRight(top) } },
            onLeft = { scope.launch { offset.animateTo(-width * 1.4f, tween(260)); onLeft(top) } }
        )
    }
}

@Composable
private fun LeftoverCard(task: Task, modifier: Modifier, hint: Float = 0f, onRight: (() -> Unit)? = null, onLeft: (() -> Unit)? = null) {
    val area = areaStyle(task.category)
    val day = SimpleDateFormat("EEEE d", Spanish).format(Date(task.timestamp)).replaceFirstChar { it.uppercase() }
    Column(
        modifier.fillMaxWidth().height(210.dp).brishCard().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(task.title.ifBlank { "Sin título" }, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            BrishPill(area.name, area.background, area.content)
        }
        Text("Era para el $day${task.time?.let { " a las $it" } ?: ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        if (onRight != null && onLeft != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).height(48.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f + 0.4f * (-hint).coerceAtLeast(0f)))
                        .bounceClick(onClick = onLeft),
                    contentAlignment = Alignment.Center
                ) { Text("← Soltar", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Box(
                    Modifier.weight(1f).height(48.dp).clip(CircleShape)
                        .background(BrishYellow.copy(alpha = 0.55f + 0.45f * hint.coerceAtLeast(0f)))
                        .bounceClick(onClick = onRight),
                    contentAlignment = Alignment.Center
                ) { Text("Pasar a hoy →", fontWeight = FontWeight.ExtraBold, color = BrishInk) }
            }
        }
    }
}

@Composable
private fun NextWeekStep(r: WeeklyReview) {
    val busiest = r.nextDays.maxByOrNull { it.second }
    val max = (busiest?.second ?: 0).coerceAtLeast(1)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepTitle("Lo que viene", "Tu próxima semana")
        Row(
            Modifier.fillMaxWidth().height(200.dp).brishCard().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            r.nextDays.forEachIndexed { i, (day, count) ->
                var grow by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(60L * i); grow = true }
                val f by animateFloatAsState(if (grow) count.toFloat() / max else 0f, BrishMotion.soft(), label = "nextBar")
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.fillMaxWidth().fillMaxHeight(0.75f * f + 0.02f).clip(CircleShape).background(if (day == busiest?.first && count > 0) BrishYellow else AreaHealth.copy(alpha = 0.35f)))
                    Spacer(Modifier.height(6.dp))
                    Text(SimpleDateFormat("EEEEE", Spanish).format(Date(day)).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Column(Modifier.fillMaxWidth().brishCascade(1).brishCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val total = r.nextDays.sumOf { it.second }
            Text(if (total == 0) "Semana en blanco" else "$total bloques planificados", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                when {
                    total == 0 -> "Empieza por lo fijo: trabajo, gimnasio, compras. Luego llena los huecos."
                    busiest != null && busiest.second >= 5 -> "El ${SimpleDateFormat("EEEE", Spanish).format(Date(busiest.first))} viene cargado. Deja un hueco libre para imprevistos."
                    else -> "Buen equilibrio. Revisa que haya tiempo para descansar."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val nextWeekKey = "next-${r.nextDays.firstOrNull()?.first ?: 0L}"
        ReflectionCard(
            weekKey = nextWeekKey,
            title = "Enfoque de la próxima semana",
            placeholder = "¿Cuál es tu prioridad principal para esta semana?",
            modifier = Modifier.brishCascade(2)
        )
    }
}

@Composable
private fun ReflectionCard(
    weekKey: String,
    title: String = "Reflexión de la semana",
    placeholder: String = "¿Qué salió bien y qué quieres ajustar?",
    modifier: Modifier = Modifier
) {
    val answerDraft = rememberPersistentDraftText("weekly-review:answer:$weekKey")
    Column(modifier.fillMaxWidth().brishCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        DraftRecoveryNotice(answerDraft)
        BrishTextField(
            value = answerDraft.value,
            onValueChange = { answerDraft.value = it },
            placeholder = placeholder,
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            shape = BrishShapes.card,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(58.dp)
            .brishShadow(BrishShapes.pill, elevation = 14.dp, tone = BrishGoldShadow, strength = 1.4f)
            .clip(BrishShapes.pill).background(BrishButtonBrush).brishEdgeHighlight()
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(label, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "reviewCta") {
            Text("$it →", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BrishInk)
        }
    }
}
