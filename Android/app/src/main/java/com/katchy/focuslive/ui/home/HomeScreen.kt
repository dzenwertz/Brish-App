package com.katchy.focuslive.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.R
import com.katchy.focuslive.ui.components.BrishArt
import com.katchy.focuslive.ui.components.BrishArtKind
import com.katchy.focuslive.ui.components.BrishCelebrationBurst
import com.katchy.focuslive.ui.components.BrishIconTile
import com.katchy.focuslive.ui.components.BrishGlyph
import com.katchy.focuslive.ui.components.BrishPill
import com.katchy.focuslive.ui.components.BrishPullToRefreshBox
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.BrishScreenHeader
import com.katchy.focuslive.ui.components.BrishSectionTitle
import com.katchy.focuslive.ui.components.LocalTabReselectSignal
import com.katchy.focuslive.ui.components.areaStyle
import com.katchy.focuslive.ui.components.brishSharedBounds
import com.katchy.focuslive.ui.components.brishSharedElement
import com.katchy.focuslive.ui.theme.brishCascade
import com.katchy.focuslive.ui.habits.HabitLogic
import com.katchy.focuslive.ui.habits.HabitsViewModel
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.AreaHealthBg
import com.katchy.focuslive.ui.theme.AreaPersonal
import com.katchy.focuslive.ui.theme.AreaPersonalBg
import com.katchy.focuslive.ui.theme.AreaWork
import com.katchy.focuslive.ui.theme.AreaWorkBg
import com.katchy.focuslive.ui.theme.BrishGoldShadow
import com.katchy.focuslive.ui.theme.BrishHeroBrush
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishNow
import com.katchy.focuslive.ui.theme.BrishOnYellow
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishWarmWhite
import com.katchy.focuslive.ui.theme.BrishWarning
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.BrishYellowSoft
import com.katchy.focuslive.ui.theme.MotionTokens
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import com.katchy.focuslive.ui.util.bounceClick
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Spanish = Locale("es", "PE")

@Composable
fun HomeScreen(
    isGuest: Boolean = false,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onOpenHabits: () -> Unit = {},
    onCapture: () -> Unit = {},
    onReview: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    habitsViewModel: HabitsViewModel = hiltViewModel()
) {
    val state by viewModel.homeState.collectAsState()
    val running by viewModel.isTimerRunning.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val workDuration by viewModel.workDuration.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val habits by habitsViewModel.habits.collectAsState()
    val error by viewModel.taskCreationError.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it) } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume() }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.onResume()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // La pantalla solo cambia una vez por minuto; el avance por segundo vive dentro de la tarjeta Ahora
    // (antes toda la pantalla de Hoy se recomponía cada segundo).
    val nowMinutes by produceState(LocalTime.now().let { it.hour * 60 + it.minute }) {
        while (true) {
            val now = LocalTime.now()
            delay((60 - now.second) * 1_000L - now.nano / 1_000_000 + 50L)
            value = LocalTime.now().let { it.hour * 60 + it.minute }
        }
    }
    val plan = remember(state.tasks, nowMinutes) { planToday(state.tasks, nowMinutes, 0, System.currentTimeMillis()) }
    val timerTotalSeconds = (workDuration * 60L).coerceAtLeast(1L)
    val timerProgress = remember(timeLeft, timerTotalSeconds) {
        (1f - timeLeft.toFloat() / maxOf(timerTotalSeconds, timeLeft)).coerceIn(0f, 1f)
    }
    val today = LocalDate.now()
    val todaysHabits = remember(habits, today) { habits.orEmpty().filter { HabitLogic.scheduledOn(it, today) } }

    BrishPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        HomeContent(
            plan = plan,
            userName = userName,
            isGuest = isGuest,
            timerRunning = running,
            timerLabel = formatTimer(timeLeft),
            timerProgress = timerProgress,
            habits = todaysHabits,
            today = today,
            onLogin = onNavigateToLogin,
            onSettings = onNavigateToSettings,
            onPlanner = onNavigateToPlanner,
            onHabits = onOpenHabits,
            onCapture = onCapture,
            onReview = onReview,
            onResumeFocus = onNavigateToFocus,
            onFocusTask = { viewModel.setActiveTask(it.id); onNavigateToFocus() },
            onToggleTask = viewModel::toggleTask,
            onPostponeTask = { task ->
                viewModel.postponeTask(task) {
                    scope.launch {
                        val res = snackbar.showSnackbar(
                            message = "«${task.title}» se movió a mañana",
                            actionLabel = "Deshacer",
                            duration = SnackbarDuration.Short
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            viewModel.undoPostponeTask()
                        }
                    }
                }
            },
            onTapHabit = { habitsViewModel.tap(it) }
        )
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = com.katchy.focuslive.ui.main.LocalBottomBarInset.current)
        )
    }
}

internal fun formatTimer(seconds: Long): String =
    String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)

private fun hm(minutes: Int) = String.format(Locale.US, "%02d:%02d", minutes / 60 % 24, minutes % 60)

private fun duration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

@Composable
fun HomeContent(
    plan: TodayPlan,
    userName: String = "",
    isGuest: Boolean = false,
    timerRunning: Boolean = false,
    timerLabel: String = "",
    timerProgress: Float = 0f,
    habits: List<Habit> = emptyList(),
    today: LocalDate = LocalDate.now(),
    onLogin: () -> Unit = {},
    onSettings: () -> Unit = {},
    onPlanner: () -> Unit = {},
    onHabits: () -> Unit = {},
    onCapture: () -> Unit = {},
    onReview: () -> Unit = {},
    onResumeFocus: () -> Unit = {},
    onFocusTask: (Task) -> Unit = {},
    onToggleTask: (Task) -> Unit = {},
    onPostponeTask: (Task) -> Unit = {},
    onTapHabit: (Habit) -> Unit = {}
) {
    val dateLabel = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Spanish)).replaceFirstChar { it.uppercase() }
    }
    var showDone by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val reselectSignal = LocalTabReselectSignal.current
    LaunchedEffect(reselectSignal) {
        if (reselectSignal > 0) {
            listState.animateScrollToItem(0)
        }
    }
    // Mantiene visible la fila mientras corre la animación de pop + tachado + pliegue con resorte
    var animatingCompleteIds by remember { mutableStateOf(setOf<String>()) }
    var collapsedIds by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()

    fun triggerCompleteWithCollapse(task: Task) {
        onToggleTask(task)
        if (!task.isCompleted && !reducedMotion) {
            animatingCompleteIds = animatingCompleteIds + task.id
            scope.launch {
                delay(240)
                collapsedIds = collapsedIds + task.id
                delay(260)
                animatingCompleteIds = animatingCompleteIds - task.id
                collapsedIds = collapsedIds - task.id
            }
        }
    }

    val visiblePending = remember(plan.pending, plan.done, animatingCompleteIds) {
        val hold = plan.done.filter { it.id in animatingCompleteIds }
        (plan.pending + hold).distinctBy { it.id }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp + com.katchy.focuslive.ui.main.LocalBottomBarInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            entering(0, "header") {
                BrishScreenHeader(
                    title = "Hoy",
                    overline = dateLabel
                ) {
                    // Un solo acceso a Ajustes: el avatar (antes había además un menú que hacía lo mismo).
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .bounceClick(onClick = onSettings)
                            .semantics { contentDescription = "Perfil y ajustes" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(userName.trim().firstOrNull()?.uppercase() ?: "B", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (isGuest) {
                entering(1, "guest") {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BrishYellowSoft)
                            .clickable(role = Role.Button, onClick = onLogin).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Inicia sesión para no perder tus datos si cambias de teléfono.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = BrishInk, modifier = Modifier.weight(1f))
                        Text("Entrar ›", fontWeight = FontWeight.SemiBold, color = BrishInk)
                    }
                }
            }
            entering(2, "now") {
                NowCard(plan, timerRunning, timerLabel, timerProgress, onResumeFocus, onFocusTask, onToggleTask, onCapture)
            }
            // La tarjeta de arriba ya muestra el siguiente bloque cuando no hay uno en curso ni temporizador:
            // no repetirlo en "Después".
            val laterTasks = if (plan.current == null && !timerRunning) plan.next.drop(1) else plan.next
            if (laterTasks.isNotEmpty()) {
                entering(3, "next") {
                    Column(Modifier.fillMaxWidth().brishCard().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)) {
                        BrishSectionTitle("Después", action = "Ver agenda", onAction = onPlanner)
                        laterTasks.take(6).forEach { task ->
                            NextRow(task, onFocus = { onFocusTask(task) })
                        }
                    }
                }
            }
            if (visiblePending.isNotEmpty()) {
                entering(4, "pending") {
                    Column(Modifier.fillMaxWidth().brishCard().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
                        BrishSectionTitle(if (visiblePending.any { it.time != null }) "Pendientes" else "Sin hora")
                        visiblePending.forEach { task ->
                            androidx.compose.runtime.key(task.id) {
                                AnimatedVisibility(
                                    visible = task.id !in collapsedIds,
                                    enter = expandVertically(MotionTokens.sheetSpring()) + fadeIn(tween(150)),
                                    exit = shrinkVertically(MotionTokens.sheetSpring()) + fadeOut(tween(150))
                                ) {
                                    PendingRow(
                                        task = task,
                                        today = today,
                                        forceChecked = task.id in animatingCompleteIds,
                                        onToggle = { triggerCompleteWithCollapse(task) },
                                        onPostpone = { onPostponeTask(task) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            entering(5, "habits") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSectionTitle("Hábitos de hoy", Modifier.padding(horizontal = 4.dp), action = if (habits.isEmpty()) "Crear" else "Ver todos", onAction = onHabits)
                    if (habits.isEmpty()) {
                        Text(
                            "Aún no tienes hábitos. Crea uno con ＋, por ejemplo «leer 20 min todos los días».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    } else {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            habits.forEach { habit -> HabitChip(habit, today, onClick = { onTapHabit(habit) }) }
                        }
                    }
                }
            }
            if (today.dayOfWeek == java.time.DayOfWeek.SUNDAY || today.dayOfWeek == java.time.DayOfWeek.MONDAY) {
                entering(6, "review") {
                    Row(
                        Modifier.fillMaxWidth().brishCard().clip(BrishShapes.card).clickable(role = Role.Button, onClick = onReview).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BrishArt(BrishArtKind.Calendar, Modifier.size(64.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Revisión semanal", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("3 minutos para cerrar la semana y planear la siguiente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("›", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (plan.done.isNotEmpty()) {
                item(key = "done") {
                    Column(Modifier.fillMaxWidth().brishCard().animateContentSize(BrishMotion.soft())) {
                        Row(
                            Modifier.fillMaxWidth().clickable(role = Role.Button) { showDone = !showDone }.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hechas hoy (${plan.done.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            val turn by animateFloatAsState(if (showDone) 180f else 0f, BrishMotion.soft(), label = "doneArrow")
                            Icon(Icons.Rounded.ExpandMore, contentDescription = if (showDone) "Ocultar" else "Mostrar", modifier = Modifier.rotate(turn))
                        }
                        if (showDone) {
                            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                                plan.done.forEach { task -> PendingRow(task, today, onToggle = { onToggleTask(task) }) }
                            }
                        }
                    }
                }
            }
        }
        com.katchy.focuslive.ui.components.BrishCollapsedTitleBar("Hoy", listState, Modifier.align(Alignment.TopCenter))
    }
}

/** Entrada escalonada (máx 6 ítems, 48 ms de paso, 20 dp + fade; respeta Reduce Motion). */
private fun LazyListScope.entering(index: Int, key: String, content: @Composable () -> Unit) {
    item(key = key) {
        Box(Modifier.fillMaxWidth().brishCascade(index, key)) {
            content()
        }
    }
}

@Composable
private fun NowCard(
    plan: TodayPlan,
    timerRunning: Boolean,
    timerLabel: String,
    timerProgress: Float,
    onResumeFocus: () -> Unit,
    onFocusTask: (Task) -> Unit,
    onToggleTask: (Task) -> Unit,
    onCapture: () -> Unit
) {
    val current = plan.current
    val nextUp = plan.next.firstOrNull()
    Column(
        Modifier
            .fillMaxWidth()
            .brishSharedBounds("focus-hero", BrishShapes.hero)
            .brishShadow(BrishShapes.hero, elevation = 18.dp, tone = BrishGoldShadow, strength = 1.6f)
            .clip(BrishShapes.hero)
            .background(BrishHeroBrush)
            .brishEdgeHighlight()
            .padding(18.dp)
            .animateContentSize(BrishMotion.soft()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when {
            current != null -> {
                val start = minutesOf(current.time)!!
                val end = endMinutesOf(current)!!
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(BrishNow))
                    Spacer(Modifier.width(8.dp))
                    Text("AHORA · ${hm(start)} – ${hm(end)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrishOnYellow, modifier = Modifier.weight(1f))
                    Text("quedan ${duration(plan.minutesLeft)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrishOnYellow)
                }
                TaskHeadline(current)
                // Reloj por segundo solo aquí, para que la barra avance en vivo sin recomponer Hoy entero.
                val liveTime by produceState(LocalTime.now()) {
                    while (true) { delay(1_000); value = LocalTime.now() }
                }
                val liveProgress = run {
                    val s = minutesOf(current.time) ?: 0
                    val e = endMinutesOf(current) ?: (s + 60)
                    val exact = liveTime.hour * 60 + liveTime.minute + liveTime.second / 60f
                    ((exact - s) / (e - s).coerceAtLeast(1)).coerceIn(0f, 1f)
                }
                val progress by animateFloatAsState(liveProgress, BrishMotion.soft(), label = "nowProgress")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(CircleShape).background(com.katchy.focuslive.ui.theme.BrishAccent))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrishOnYellow)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroButton("Enfocar", dark = true, icon = Icons.Rounded.PlayArrow, modifier = Modifier.weight(1f)) { onFocusTask(current) }
                    DoneButton(Modifier.weight(1f)) { onToggleTask(current) }
                }
            }
            timerRunning -> {
                val animatedTimerProgress by animateFloatAsState(timerProgress, BrishMotion.soft(), label = "timerLiveProgress")
                Text("ENFOQUE EN CURSO", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrishOnYellow)
                Text(timerLabel, style = MaterialTheme.typography.displayMedium, color = BrishInk)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(Modifier.fillMaxWidth(animatedTimerProgress).height(6.dp).clip(CircleShape).background(com.katchy.focuslive.ui.theme.BrishAccent))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("${(animatedTimerProgress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrishOnYellow)
                }
                HeroButton("Continuar", dark = true, icon = Icons.Rounded.PlayArrow, modifier = Modifier.fillMaxWidth(), onClick = onResumeFocus)
            }
            nextUp != null -> {
                val start = minutesOf(nextUp.time)!!
                val now = LocalTime.now().let { it.hour * 60 + it.minute }
                Text("SIGUIENTE · ${hm(start)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrishOnYellow)
                TaskHeadline(nextUp)
                Text("Empieza en ${duration((start - now).coerceAtLeast(1))}. Tienes tiempo libre hasta entonces.", style = MaterialTheme.typography.bodyMedium, color = BrishOnYellow)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroButton("Adelantar", dark = true, icon = Icons.Rounded.PlayArrow, modifier = Modifier.weight(1f)) { onFocusTask(nextUp) }
                    HeroButton("Capturar", dark = false, modifier = Modifier.weight(1f), onClick = onCapture)
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (plan.pending.isEmpty()) "Tu día está libre" else "Sin bloques con hora", style = MaterialTheme.typography.titleLarge, color = BrishInk)
                        Text(
                            if (plan.pending.isEmpty()) "Nada programado. Captura algo o disfruta tu tiempo." else "Elige un pendiente y enfócate en él.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrishOnYellow
                        )
                    }
                    BrishArt(BrishArtKind.Sun, Modifier.size(96.dp))
                }
                HeroButton("Capturar algo", dark = true, modifier = Modifier.fillMaxWidth(), onClick = onCapture)
            }
        }
    }
}

@Composable
private fun TaskHeadline(task: Task) {
    val area = areaStyle(task.category)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        BrishIconTile(area.icon, area.background, area.content, size = 44.dp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleLarge, color = BrishInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BrishPill(area.name, area.background, area.content)
                if (task.subtasks.isNotEmpty()) {
                    BrishPill("${task.subtasks.count { it.isCompleted }} de ${task.subtasks.size} subtareas", MaterialTheme.colorScheme.surfaceVariant, BrishOnYellow)
                }
            }
        }
    }
}

@Composable
private fun HeroButton(label: String, dark: Boolean, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier
            .height(46.dp)
            .clip(BrishShapes.button)
            .background(if (dark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .bounceClick(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (dark) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = if (dark) Color.White else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/** «Marcar hecho» se transforma en «✓ Hecho» antes de que la tarjeta cambie al siguiente bloque. */
@Composable
private fun DoneButton(modifier: Modifier = Modifier, onDone: () -> Unit) {
    var done by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(done) {
        if (done) { delay(550); onDone(); done = false }
    }
    Row(
        modifier.height(46.dp).clip(BrishShapes.button).background(MaterialTheme.colorScheme.surfaceVariant)
            .bounceClick { if (!done) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); done = true } },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(done, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith fadeOut() }, label = "done") { isDone ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = if (isDone) AreaHealth else BrishInk, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isDone) "Hecho" else "Marcar hecho", color = if (isDone) AreaHealth else BrishInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun NextRow(task: Task, onFocus: () -> Unit) {
    val area = areaStyle(task.category)
    var expanded by rememberSaveable(task.id) { mutableStateOf(false) }
    val start = minutesOf(task.time) ?: 0
    val length = (endMinutesOf(task) ?: start) - start
    Column(Modifier.fillMaxWidth().animateContentSize(BrishMotion.soft())) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrishIconTile(area.icon, area.background, area.content, size = 30.dp)
            Text(hm(start), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(44.dp))
            Text(task.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(duration(length), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(expanded, enter = expandVertically(BrishMotion.soft()) + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(start = 46.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                task.subtasks.forEach { sub ->
                    Text("· ${sub.title}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else null)
                }
                if (task.description.isNotBlank()) Text(task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Enfocar ahora ›",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(role = Role.Button, onClick = onFocus).padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PendingRow(
    task: Task,
    today: LocalDate,
    forceChecked: Boolean = false,
    onToggle: () -> Unit,
    onPostpone: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()
    var celebrationTrigger by remember(task.id) { mutableIntStateOf(0) }
    val checked = task.isCompleted || forceChecked
    val strikeProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(if (reducedMotion) MotionTokens.reducedMotionMs else 220, easing = MotionTokens.emphasized),
        label = "strikeProgress"
    )
    val overdueFrom = remember(task.timestamp, today) {
        java.time.Instant.ofEpochMilli(task.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate().takeIf { it.isBefore(today) }
    }
    val swipeOffset = remember { Animatable(0f) }
    val thresholdPx = with(density) { 88.dp.toPx() }
    var armedHaptic by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))) {
        if (swipeOffset.value.absoluteValue > 6f) {
            val swipingRight = swipeOffset.value > 0f
            val bg = if (swipingRight) AreaHealthBg else BrishYellowSoft
            val fg = if (swipingRight) AreaHealth else BrishInk
            Row(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (swipingRight) Arrangement.Start else Arrangement.End
            ) {
                Icon(
                    imageVector = if (swipingRight) Icons.Rounded.Check else Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (swipingRight) "Completar" else "Mañana",
                    color = fg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
                .heightIn(min = 50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .then(
                    if (onPostpone != null && !checked) {
                        Modifier.draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                val next = (swipeOffset.value + delta).coerceIn(-thresholdPx * 1.45f, thresholdPx * 1.45f)
                                val crossed = next.absoluteValue >= thresholdPx
                                if (crossed && !armedHaptic) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    armedHaptic = true
                                } else if (!crossed) {
                                    armedHaptic = false
                                }
                                scope.launch { swipeOffset.snapTo(next) }
                            },
                            onDragStopped = {
                                val finalX = swipeOffset.value
                                armedHaptic = false
                                when {
                                    finalX >= thresholdPx -> {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        celebrationTrigger++
                                        swipeOffset.animateTo(0f, MotionTokens.sheetSpring())
                                        onToggle()
                                    }
                                    finalX <= -thresholdPx -> {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        swipeOffset.animateTo(0f, MotionTokens.sheetSpring())
                                        onPostpone()
                                    }
                                    else -> swipeOffset.animateTo(0f, MotionTokens.sheetSpring())
                                }
                            }
                        )
                    } else Modifier
                )
                .clickable(role = Role.Checkbox) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (!checked) celebrationTrigger++
                    onToggle()
                }
                .semantics { stateDescription = if (checked) "Hecha" else "Pendiente" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrishCelebrationBurst(trigger = celebrationTrigger) {
                RoundCheck(checked)
            }
            val strikeColor = MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .drawWithContent {
                        drawContent()
                        if (strikeProgress > 0.02f) {
                            val y = size.height * 0.52f
                            drawLine(
                                color = strikeColor,
                                start = Offset(0f, y),
                                end = Offset(size.width * strikeProgress, y),
                                strokeWidth = 1.8.dp.toPx()
                            )
                        }
                    },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val note = when {
                checked -> null
                overdueFrom != null -> "desde " + overdueFrom.format(DateTimeFormatter.ofPattern("EEE d", Spanish)).replace(".", "")
                task.time != null -> "era ${task.time}"
                else -> null
            }
            note?.let { Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrishWarning) }
        }
    }
}

@Composable
private fun RoundCheck(checked: Boolean) {
    val bg by animateColorAsState(if (checked) BrishInk else Color.Transparent, BrishMotion.lively(), label = "check")
    val checkScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.92f,
        animationSpec = MotionTokens.pressSpring(),
        label = "checkPop"
    )
    Box(
        Modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = checkScale
                scaleY = checkScale
            }
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, if (checked) BrishInk else MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(checked, enter = scaleIn(tween(220, easing = MotionTokens.overshoot)) + fadeIn(), exit = fadeOut()) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun HabitChip(habit: Habit, today: LocalDate, onClick: () -> Unit) {
    val title = HabitLogic.title(habit)
    val target = HabitLogic.target(habit)
    val count = HabitLogic.countOn(habit, today)
    val done = count >= target
    val t = title.lowercase()
    val (icon, bg, fg) = when {
        listOf("agua", "beber").any { it in t } -> Triple(Icons.Rounded.WaterDrop, AreaWorkBg, AreaWork)
        listOf("dormir", "sueño").any { it in t } -> Triple(Icons.Rounded.Bedtime, AreaPersonalBg, AreaPersonal)
        listOf("gym", "gimnasio", "correr", "entrenar").any { it in t } -> Triple(Icons.Rounded.FitnessCenter, AreaHealthBg, AreaHealth)
        listOf("leer", "lectura", "libro").any { it in t } -> Triple(Icons.AutoMirrored.Rounded.MenuBook, AreaHealthBg, AreaHealth)
        else -> Triple(Icons.Rounded.CheckCircle, BrishYellowSoft, BrishInk)
    }
    val haptics = LocalHapticFeedback.current
    var celebrationTrigger by remember(habit.id) { mutableStateOf(0) }
    Row(
        Modifier
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .bounceClick {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (!done) celebrationTrigger++
                onClick()
            }
            .semantics { stateDescription = if (done) "Hecho hoy" else "Pendiente hoy" }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BrishGlyph(icon, fg, 22.dp)
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
            Text(if (target > 1) "$count/$target" else if (done) "Hecho" else "Hoy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg.copy(alpha = 0.8f))
        }
        BrishCelebrationBurst(trigger = celebrationTrigger, particleColor = fg) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(if (done) fg else Color.Transparent)
                    .border(2.dp, fg.copy(alpha = if (done) 1f else 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}
