package com.katchy.focuslive.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.ui.components.BrishTopHeader
import com.katchy.focuslive.ui.components.brishMutedPanelColor
import com.katchy.focuslive.ui.components.brishPanelBorder
import com.katchy.focuslive.ui.components.brishPanelColor
import com.katchy.focuslive.ui.components.brishPrimaryActionColor
import com.katchy.focuslive.ui.components.brishPrimaryActionContentColor
import com.katchy.focuslive.ui.util.bounceClick
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    isGuest: Boolean = false,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onOpenCourse: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.homeState.collectAsState()
    val running by viewModel.isTimerRunning.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val creatingTask by viewModel.isCreatingTask.collectAsState()
    val creationError by viewModel.taskCreationError.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(creationError) {
        creationError?.let { snackbarHost.showSnackbar(it) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.onResume()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BrishTopHeader(title = "Hoy", onNavigateToSettings = onNavigateToSettings)
            AnimatedVisibility(visible = isGuest, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    TextButton(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
                        Text("Inicia sesión para sincronizar tus datos")
                    }
                }
            }
            HomeContent(
                state = state,
                focusLabel = if (running) "Continuar · ${formatTimer(timeLeft)}" else "Empezar enfoque",
                focusRunning = running,
                onNavigateToFocus = onNavigateToFocus,
                onNavigateToPlanner = onNavigateToPlanner,
                onOpenCourse = onOpenCourse,
                onToggleTask = viewModel::toggleTask,
                onFocusTask = { viewModel.setActiveTask(it.id); onNavigateToFocus() },
                onAddTask = { title, onSaved -> viewModel.addTask(title, onSaved = onSaved) },
                creatingTask = creatingTask,
                creationError = creationError,
                onClearCreationError = viewModel::clearTaskCreationError
            )
        }
        SnackbarHost(snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

internal fun formatTimer(seconds: Long): String =
    String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)

@Composable
fun HomeContent(
    state: HomeViewModel.HomeState,
    focusLabel: String = "Empezar enfoque",
    focusRunning: Boolean = false,
    onNavigateToFocus: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onOpenCourse: (String) -> Unit = {},
    onToggleTask: (Task) -> Unit = {},
    onFocusTask: (Task) -> Unit = {},
    onAddTask: (String, () -> Unit) -> Unit = { _, _ -> },
    creatingTask: Boolean = false,
    creationError: String? = null,
    onClearCreationError: () -> Unit = {}
) {
    var showAddTask by remember { mutableStateOf(false) }
    val pendingToday = state.tasks.count { !it.isCompleted }
    val completedToday = state.tasks.size - pendingToday
    val dateLabel = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault()).format(Date())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(dateLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    when {
                        pendingToday == 0 && state.upcomingDeliveries.isEmpty() -> "Tu día está despejado"
                        pendingToday == 1 -> "Tienes 1 actividad para hoy"
                        else -> "Tienes $pendingToday actividades para hoy"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (completedToday > 0) "$completedToday completadas · continúa a tu ritmo" else "Organiza lo importante y empieza por una sola cosa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            FocusHeroCard(label = focusLabel, running = focusRunning, onClick = onNavigateToFocus)
        }

        item {
            HomeSectionHeader(
                title = "Agenda de hoy",
                subtitle = if (state.tasks.isEmpty()) "Nada programado todavía" else "${state.tasks.size} actividades",
                action = "Ver agenda",
                onAction = onNavigateToPlanner
            )
        }

        if (state.tasks.isEmpty()) {
            item {
                EmptyHomeCard(
                    icon = Icons.Rounded.EventAvailable,
                    title = "Tu agenda está libre",
                    description = "Añade una actividad cuando tengas algo que hacer hoy."
                )
            }
        } else {
            items(state.tasks, key = { "agenda-${it.id}" }) { task ->
                HomeTaskItem(task = task, onToggle = { onToggleTask(task) }, onFocus = { onFocusTask(task) })
            }
        }

        item {
            FilledTonalButton(
                onClick = { onClearCreationError(); showAddTask = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Añadir actividad")
            }
        }

        item {
            HomeSectionHeader(
                title = "Entregas próximas",
                subtitle = if (state.upcomingDeliveries.isEmpty()) "Todo al día" else "${state.upcomingDeliveries.size} con fecha",
                action = null,
                onAction = {}
            )
        }

        if (state.upcomingDeliveries.isEmpty()) {
            item {
                EmptyHomeCard(
                    icon = Icons.Rounded.School,
                    title = "Sin entregas pendientes",
                    description = "Las próximas entregas de tus cursos aparecerán aquí."
                )
            }
        } else {
            items(state.upcomingDeliveries, key = { "delivery-${it.task.id}" }) { delivery ->
                val overdue = dayStart(delivery.task.dueDate) < dayStart(System.currentTimeMillis())
                Card(
                    onClick = { delivery.task.courseId?.let(onOpenCourse) },
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = brishPanelColor()),
                    border = brishPanelBorder()
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                if (overdue) Icons.Rounded.PriorityHigh else Icons.Rounded.Assignment,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp),
                                tint = if (overdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                delivery.task.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(delivery.courseName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                (if (overdue) "Vencida · " else "Entrega · ") + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(delivery.task.dueDate)),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onFocusTask(delivery.task) }) {
                            Icon(Icons.Rounded.Timer, contentDescription = "Enfocar ${delivery.task.title}")
                        }
                    }
                }
            }
        }
    }

    if (showAddTask) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!creatingTask) showAddTask = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Actividad para hoy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        enabled = !creatingTask,
                        label = { Text("¿Qué necesitas hacer?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    creationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank() && !creatingTask, onClick = { onAddTask(title.trim()) { showAddTask = false } }) {
                    Text(if (creatingTask) "Guardando…" else "Guardar")
                }
            },
            dismissButton = { TextButton(enabled = !creatingTask, onClick = { showAddTask = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun FocusHeroCard(label: String, running: Boolean, onClick: () -> Unit) {
    val accent = Color(0xFF06B6D4)
    Surface(
        modifier = Modifier.fillMaxWidth().bounceClick(scaleDown = 0.985f, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = brishPanelColor(),
        border = brishPanelBorder(),
        shadowElevation = 0.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(13.dp), color = brishMutedPanelColor()) {
                    Icon(Icons.Rounded.CenterFocusStrong, contentDescription = null, modifier = Modifier.padding(10.dp).size(22.dp), tint = accent)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (running) "Sesión en progreso" else "Modo enfoque", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (running) "Tu temporizador sigue activo" else "Pomodoro, sonido y cero distracciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(100.dp), color = brishMutedPanelColor()) {
                    Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.padding(8.dp).size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AnimatedContent(
                targetState = label,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.98f)) togetherWith (fadeOut() + scaleOut(targetScale = 1.02f))
                },
                label = "focus-label"
            ) { value ->
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = accent)
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(title: String, subtitle: String, action: String?, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun EmptyHomeCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = brishPanelColor(),
        border = brishPanelBorder(),
        shadowElevation = 0.dp
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = brishMutedPanelColor()) {
                Icon(icon, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.padding(9.dp).size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun HomeTaskItem(task: Task, onToggle: () -> Unit, onFocus: (() -> Unit)? = null) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) brishMutedPanelColor() else brishPanelColor()
        ),
        border = brishPanelBorder()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                task.time?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!task.isCompleted && onFocus != null) {
                FilledIconButton(
                    onClick = onFocus,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = brishPrimaryActionColor(),
                        contentColor = brishPrimaryActionContentColor()
                    )
                ) {
                    Icon(Icons.Rounded.Timer, contentDescription = "Enfocar ${task.title}")
                }
            }
        }
    }
}
