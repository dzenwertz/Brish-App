package com.katchy.focuslive.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.ui.components.BrishTopHeader
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.InAppNotificationBus
import com.katchy.focuslive.ui.util.bounceClick

@Composable
fun HomeScreen(
    isGuest: Boolean = false,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToCourses: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.homeState.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val timeLeftSeconds by viewModel.timeLeft.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val activeTaskId by viewModel.activeTaskId.collectAsState()
    val workDuration by viewModel.workDuration.collectAsState(initial = 25L)
    val breakDuration by viewModel.breakDuration.collectAsState(initial = 5L)
    val isHabitsEnabled by viewModel.isHabitsEnabled.collectAsState(initial = true)

    val isBreak = timerState == com.katchy.focuslive.data.manager.TimerManager.TimerState.BREAK
    // Total duration in SECONDS (matching timeLeft units from TimerManager)
    val totalTimeSeconds = if (isBreak) breakDuration * 60L else workDuration * 60L

    val isDark = isDarkTheme()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    var showTaskPicker by remember { mutableStateOf(false) }
    var showPomodoroSettingsDialog by remember { mutableStateOf(false) }
    var showQuickAddTaskDialog by remember { mutableStateOf(false) }

    // Lifecycle Observer to refresh onResume
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Android 13+ Notification Permission
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var startTimerAfterPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (startTimerAfterPermission) {
            startTimerAfterPermission = false
            viewModel.toggleTimer()
        }
    }

    // Active Task: Ensure only incomplete tasks are chosen
    val activeTask = remember(state.tasks, activeTaskId) {
        state.tasks.find { it.id == activeTaskId && !it.isCompleted }
            ?: state.tasks.firstOrNull { !it.isCompleted }
    }

    // Progress calculation (both values in seconds)
    val progress = if (totalTimeSeconds > 0) {
        (totalTimeSeconds - timeLeftSeconds).toFloat() / totalTimeSeconds.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "pomodoroProgress"
    )

    // timeLeftSeconds is already in seconds — display directly
    val minutes = (timeLeftSeconds / 60).toString().padStart(2, '0')
    val seconds = (timeLeftSeconds % 60).toString().padStart(2, '0')

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BrishTopHeader(
                title = "Brish",
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToChat = onNavigateToChat
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tu escritorio", style = MaterialTheme.typography.headlineMedium)
                    Text("Elige una tarea. Dale tu atención.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onNavigateToPlanner) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = "Abrir agenda", modifier = Modifier.size(21.dp))
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) Color(0xFF15171E) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFE9ECF1)),
                modifier = Modifier.fillMaxWidth()
            ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

            // 1. Circular Pomodoro Ring with Settings Button
            Box(
                modifier = Modifier
                    .size(184.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val ringBgColor = if (isDark) Color(0xFF1E212B) else Color(0xFFE5E7EB)
                val ringProgressColor = if (isBreak) Color(0xFF10B981) else if (isDark) Color.White else Color(0xFF111827)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 5.dp.toPx()
                    drawCircle(
                        color = ringBgColor,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = ringProgressColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$minutes:$seconds",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-1).sp
                        ),
                        color = if (isBreak) Color(0xFF10B981) else MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            isBreak -> "Descanso"
                            isTimerRunning -> "En curso"
                            else -> "A tu ritmo"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isBreak) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Tag Pill Indicator (e.g. Focus vs Descanso)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (isBreak) Color(0xFF10B981).copy(alpha = 0.12f) else if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6),
                    border = BorderStroke(
                        1.dp,
                        if (isBreak) Color(0xFF10B981).copy(alpha = 0.3f) else if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E7EB)
                    )
                ) {
                    Text(
                        text = if (isBreak) "$breakDuration min · Descanso" else "$workDuration min · Concentración",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = if (isBreak) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Settings quick-pill to adjust timers
                IconButton(
                    onClick = { showPomodoroSettingsDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E7EB), CircleShape)
                        .bounceClick()
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = "Ajustar Pomodoro",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 2. Timer Control Buttons (Start / Reset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start / Pause Button (Solid Pill)
                Button(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            startTimerAfterPermission = true
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.toggleTimer()
                        }
                    },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bounceClick()
                ) {
                    Text(
                        text = if (isTimerRunning) "Pausar" else "Comenzar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Reset Button (Outlined Pill)
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.resetTimer()
                    },
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.25f) else Color(0xFFD1D5DB)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier
                        .width(112.dp)
                        .height(48.dp)
                        .bounceClick()
                ) {
                    Text(
                        text = "Reiniciar",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            // La tarea y el temporizador forman una sola unidad de enfoque.
            Surface(
                onClick = { showTaskPicker = true },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.CenterFocusStrong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isTimerRunning) "Enfoque actual" else "Tu siguiente paso",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (activeTask?.priority == "HIGH") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ALTA",
                                        color = Color(0xFFEF4444),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (state.tasks.isNotEmpty() && state.tasks.all { it.isCompleted }) "¡Todo completado por hoy!" else activeTask?.title ?: "Elegir tarea de hoy",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            }
            }

            val pendingTasksCount = state.tasks.count { !it.isCompleted }
            if (state.upcomingDeliveries.isNotEmpty()) {
                TodaySection(
                    title = "Próximas entregas",
                    actionLabel = "Cursos",
                    onAction = onNavigateToCourses,
                    onAddClick = onNavigateToCourses,
                    isDark = isDark
                ) {
                    state.upcomingDeliveries.forEach { delivery ->
                        TodayCheckRow(
                            title = delivery.task.title,
                            subtitle = if (!delivery.task.hasDueDate) "${delivery.courseName} · Sin fecha límite" else "${delivery.courseName} · ${java.text.SimpleDateFormat("dd MMM", java.util.Locale("es", "PE")).format(java.util.Date(delivery.task.dueDate))}",
                            priority = delivery.task.priority,
                            isOverdue = delivery.task.hasDueDate && delivery.task.dueDate < System.currentTimeMillis(),
                            checked = false,
                            isDark = isDark,
                            onToggle = { viewModel.toggleTask(delivery.task) }
                        )
                    }
                }
            }
            TodaySection(
                title = if (state.tasks.isEmpty()) "Tareas de hoy" else "Tareas de hoy ($pendingTasksCount)",
                actionLabel = "Agenda",
                onAction = onNavigateToPlanner,
                onAddClick = { showQuickAddTaskDialog = true },
                isDark = isDark
            ) {
                if (state.tasks.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nada pendiente para hoy",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Añade una tarea rápida o planifica en Agenda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showQuickAddTaskDialog = true },
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                                contentColor = if (isDark) Color(0xFF111827) else Color.White
                            ),
                            modifier = Modifier.height(36.dp).bounceClick()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Añadir Tarea", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    val todayMidnight = remember(state.tasks) {
                        val cal = java.util.Calendar.getInstance()
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }

                    state.tasks.take(6).forEach { task ->
                        val isOverdue = task.repeatMode == "NONE" && !task.isCompleted && task.timestamp < todayMidnight
                        TodayCheckRow(
                            title = task.title,
                            subtitle = task.time,
                            priority = task.priority,
                            isOverdue = isOverdue,
                            checked = task.isCompleted,
                            isDark = isDark,
                            onToggle = { viewModel.toggleTask(task) }
                        )
                    }

                    if (state.tasks.size > 6) {
                        TextButton(
                            onClick = onNavigateToPlanner,
                            modifier = Modifier.fillMaxWidth().bounceClick(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ver ${state.tasks.size - 6} tareas más en Agenda",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF06B6D4)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFF06B6D4)
                                )
                            }
                        }
                    }
                }
            }

            if (isHabitsEnabled) {
                val pendingHabitsCount = state.todayHabits.count { !viewModel.isHabitDoneToday(it) }
                TodaySection(
                    title = if (state.todayHabits.isEmpty()) "Hábitos de hoy" else "Hábitos de hoy ($pendingHabitsCount)",
                    actionLabel = "Hábitos",
                    onAction = onNavigateToHabits,
                    isDark = isDark
                ) {
                    if (state.todayHabits.isEmpty()) {
                        Text(
                            text = "No hay hábitos para hoy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.todayHabits.forEach { habit ->
                            TodayCheckRow(
                                title = "${habit.icon}  ${habit.title}",
                                subtitle = null,
                                checked = viewModel.isHabitDoneToday(habit),
                                isDark = isDark,
                                onToggle = { viewModel.toggleHabitToday(habit) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    // Task Picker Dialog
    if (showTaskPicker) {
        SmoothDialog(onDismissRequest = { showTaskPicker = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF15171E) else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFE5E7EB)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 450.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Elegir Tarea para Enfocar",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showTaskPicker = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val pendingTasks = state.tasks.filter { !it.isCompleted }
                    if (pendingTasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tienes tareas pendientes en tu agenda para hoy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(pendingTasks, key = { it.id }) { task ->
                                Surface(
                                    onClick = {
                                        viewModel.setActiveTask(task.id)
                                        showTaskPicker = false
                                        InAppNotificationBus.post("Tarea en foco: ${task.title}")
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6),
                                    modifier = Modifier.fillMaxWidth().bounceClick()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.WorkOutline,
                                            contentDescription = null,
                                            tint = if (isDark) Color.White else Color(0xFF111827),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!task.description.isNullOrBlank()) {
                                                Text(
                                                    text = task.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Pomodoro Duration Quick Config Dialog
    if (showPomodoroSettingsDialog) {
        var selectedWork by remember(workDuration) { mutableLongStateOf(workDuration) }
        var selectedBreak by remember(breakDuration) { mutableLongStateOf(breakDuration) }

        SmoothDialog(onDismissRequest = { showPomodoroSettingsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF15171E) else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFE5E7EB)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configurar Pomodoro",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showPomodoroSettingsDialog = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Tiempo de Enfoque (minutos)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15L, 25L, 45L, 60L).forEach { mins ->
                            Surface(
                                onClick = { selectedWork = mins },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedWork == mins) {
                                    if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                                } else {
                                    if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .bounceClick()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${mins}m",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedWork == mins) {
                                            if (isDark) Color(0xFF111827) else Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text("Enfoque personalizado · $selectedWork min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = selectedWork.toFloat().coerceIn(5f, 120f),
                        onValueChange = { selectedWork = it.toLong() },
                        valueRange = 5f..120f,
                        steps = 114
                    )

                    Text(
                        text = "Tiempo de Descanso (minutos)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5L, 10L, 15L, 20L).forEach { mins ->
                            Surface(
                                onClick = { selectedBreak = mins },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedBreak == mins) {
                                    if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                                } else {
                                    if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .bounceClick()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${mins}m",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedBreak == mins) {
                                            if (isDark) Color(0xFF111827) else Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Descanso personalizado · $selectedBreak min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = selectedBreak.toFloat().coerceIn(1f, 30f),
                        onValueChange = { selectedBreak = it.toLong() },
                        valueRange = 1f..30f,
                        steps = 28
                    )

                    Button(
                        onClick = {
                            viewModel.setWorkDuration(selectedWork)
                            viewModel.setBreakDuration(selectedBreak)
                            viewModel.resetTimer()
                            showPomodoroSettingsDialog = false
                            InAppNotificationBus.post("Pomodoro ajustado: $selectedWork min / $selectedBreak min")
                        },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                            contentColor = if (isDark) Color(0xFF111827) else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick()
                    ) {
                        Text("Aplicar Cambios", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showQuickAddTaskDialog) {
        QuickAddTaskDialog(
            isDark = isDark,
            onDismiss = { showQuickAddTaskDialog = false },
            onSave = { title, priority, time ->
                viewModel.addTask(title, priority, time)
                InAppNotificationBus.post("Tarea añadida")
            }
        )
    }
}

@Composable
private fun QuickAddTaskDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, priority: String, time: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var time by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    SmoothDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF15171E) else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFE5E7EB)
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Añadir Tarea para Hoy",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("¿Qué necesitas hacer hoy?") },
                    placeholder = { Text("Ej: Enviar reporte de ventas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Prioridad",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("LOW", "Baja", Color(0xFF64748B)),
                        Triple("MEDIUM", "Media", Color(0xFFF59E0B)),
                        Triple("HIGH", "Alta", Color(0xFFEF4444))
                    ).forEach { (pKey, pLabel, pColor) ->
                        val isSelected = priority == pKey
                        Surface(
                            onClick = { priority = pKey },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) pColor.copy(alpha = 0.2f) else if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6),
                            border = BorderStroke(1.dp, if (isSelected) pColor else Color.Transparent),
                            modifier = Modifier.weight(1f).height(36.dp).bounceClick()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = pLabel,
                                    color = if (isSelected) pColor else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val c = java.util.Calendar.getInstance()
                            android.app.TimePickerDialog(
                                context,
                                { _, h, m ->
                                    time = String.format(java.util.Locale.US, "%02d:%02d", h, m)
                                },
                                c.get(java.util.Calendar.HOUR_OF_DAY),
                                c.get(java.util.Calendar.MINUTE),
                                true
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(time?.let { "Hora: $it" } ?: "Asignar Hora", fontSize = 12.sp)
                    }

                    if (time != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { time = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Quitar hora", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(title.trim(), priority, time)
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp).bounceClick()
                ) {
                    Text("Añadir Tarea", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun TodaySection(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    isDark: Boolean,
    onAddClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onAddClick != null) {
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier.size(32.dp).bounceClick()
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Añadir tarea rápida",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(actionLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF15171E) else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun TodayCheckRow(
    title: String,
    subtitle: String?,
    priority: String? = null,
    isOverdue: Boolean = false,
    checked: Boolean,
    isDark: Boolean,
    onToggle: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .bounceClick(scaleDown = 0.98f)
            .clickable {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onToggle()
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = if (isDark) Color.White else Color(0xFF111827)
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (checked) TextDecoration.LineThrough else null
                    ),
                    color = if (checked) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isOverdue && !checked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ATRASADA",
                            color = Color(0xFFF59E0B),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                if (priority == "HIGH") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ALTA",
                            color = Color(0xFFEF4444),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (checked) 0.5f else 0.8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
