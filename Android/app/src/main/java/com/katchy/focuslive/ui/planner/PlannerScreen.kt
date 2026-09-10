@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.katchy.focuslive.ui.planner

import android.app.TimePickerDialog as AndroidTimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.SubtaskItem
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.ui.components.BrishTopHeader
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.InAppNotificationBus
import com.katchy.focuslive.ui.util.bounceClick
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PlannerScreen(
    onBack: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: PlannerViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasks by viewModel.tasksForSelectedDate.collectAsState()
    val subtaskLoadingStates by viewModel.subtaskLoadingStates.collectAsState()
    val isBrainDumpLoading by viewModel.isBrainDumpLoading.collectAsState()
    val brainDumpError by viewModel.brainDumpError.collectAsState()
    val isPrioritizing by viewModel.isPrioritizing.collectAsState()

    var showAddTaskSheet by remember { mutableStateOf(false) }
    var showBrainDumpDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") } // "ALL", "PENDING", "HIGH"

    // Draft states for new task
    var draftTaskTitle by rememberSaveable { mutableStateOf("") }
    var draftTaskCategory by rememberSaveable { mutableStateOf("General") }
    var draftTaskPriority by rememberSaveable { mutableStateOf("MEDIUM") }
    var draftTaskRepeatMode by rememberSaveable { mutableStateOf("NONE") }
    var draftTaskTime by rememberSaveable { mutableStateOf<String?>(null) }
    var draftTaskEndTime by rememberSaveable { mutableStateOf<String?>(null) }
    var draftNewSubtask by rememberSaveable { mutableStateOf("") }
    var draftTaskSubtasks by rememberSaveable { mutableStateOf(listOf<String>()) }
    var brainDumpText by rememberSaveable { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    val isDark = isDarkTheme()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.syncWithDeviceDate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val monthYearText = remember(selectedDate) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(Date(selectedDate)).replaceFirstChar { it.uppercase() }
    }

    // 7-day strip centered on selected date
    val weekDays = remember(selectedDate) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        (0..6).map {
            val dayCal = cal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_WEEK, it)
            dayCal.timeInMillis
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BrishTopHeader(
                title = "Agenda",
                onBack = onBack,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToChat = onNavigateToChat
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI organizer is secondary to the primary add action.
                FloatingActionButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showBrainDumpDialog = true
                    },
                    containerColor = if (isDark) Color(0xFF1E212B) else Color.White,
                    contentColor = if (isDark) Color.White else Color(0xFF111827),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .bounceClick()
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = "Organizar con Brish AI", modifier = Modifier.size(20.dp))
                }

                // Add Task FAB
                FloatingActionButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showAddTaskSheet = true
                    },
                    containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                    contentColor = if (isDark) Color(0xFF111827) else Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .size(54.dp)
                        .bounceClick()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Añadir Tarea", modifier = Modifier.size(24.dp))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Month & Week Calendar Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF15171E) else Color.White,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Month & Navigation Arrows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthYearText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSameDay(selectedDate, System.currentTimeMillis())) {
                                Text(
                                    text = "Hoy",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0891B2),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        viewModel.selectDate(System.currentTimeMillis())
                                    },
                                    modifier = Modifier.size(30.dp).bounceClick()
                                ) {
                                    Icon(Icons.Rounded.Today, contentDescription = "Volver a hoy", modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(
                                onClick = {
                                    val prevWeek = Calendar.getInstance().apply {
                                        timeInMillis = selectedDate
                                        add(Calendar.DAY_OF_YEAR, -7)
                                    }
                                    viewModel.selectDate(prevWeek.timeInMillis)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Semana anterior",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val nextWeek = Calendar.getInstance().apply {
                                        timeInMillis = selectedDate
                                        add(Calendar.DAY_OF_YEAR, 7)
                                    }
                                    viewModel.selectDate(nextWeek.timeInMillis)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Semana siguiente",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 7 Days Week Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dayLetters = listOf("L", "M", "M", "J", "V", "S", "D")
                        weekDays.forEachIndexed { index, dateMillis ->
                            val isSelected = isSameDay(dateMillis, selectedDate)
                            val dayNumFormat = SimpleDateFormat("d", Locale.getDefault())
                            val dayNum = dayNumFormat.format(Date(dateMillis))

                            val dayBgColor by animateColorAsState(
                                targetValue = if (isSelected) {
                                    if (isDark) Color.White else Color(0xFF111827)
                                } else Color.Transparent,
                                animationSpec = tween(160),
                                label = "dayBg_$index"
                            )
                            val dayTextColor by animateColorAsState(
                                targetValue = if (isSelected) {
                                    if (isDark) Color.Black else Color.White
                                } else MaterialTheme.colorScheme.onSurface,
                                animationSpec = tween(160),
                                label = "dayText_$index"
                            )
                            val dayLabelColor by animateColorAsState(
                                targetValue = if (isSelected) {
                                    if (isDark) Color.Black else Color.White
                                } else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(160),
                                label = "dayLabel_$index"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(dayBgColor)
                                    .bounceClick(scaleDown = 0.92f) {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        viewModel.selectDate(dateMillis)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = dayLetters[index],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = dayLabelColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayNum,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = dayTextColor,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quick Filter Chips & Prioritize with AI Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pendingCount = tasks.count { !it.isCompleted }
                    val highCount = tasks.count { it.priority == "HIGH" && !it.isCompleted }

                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Todas (${tasks.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) Color.White else Color(0xFF111827),
                            selectedLabelColor = if (isDark) Color.Black else Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" },
                        label = { Text("Pendientes ($pendingCount)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) Color.White else Color(0xFF111827),
                            selectedLabelColor = if (isDark) Color.Black else Color.White
                        )
                    )

                    if (highCount > 0 || selectedFilter == "HIGH") {
                        FilterChip(
                            selected = selectedFilter == "HIGH",
                            onClick = { selectedFilter = "HIGH" },
                            label = { Text("Urgentes ($highCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEF4444),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // AI Prioritize Button (Eisenhower Matrix)
                if (tasks.size >= 2) {
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.prioritizeTasksWithAI()
                            InAppNotificationBus.post("Priorizando tareas con IA...")
                        },
                        enabled = !isPrioritizing,
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .bounceClick(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color.White else Color(0xFF111827)
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE5E7EB))
                    ) {
                        if (isPrioritizing) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Priorizando...", fontSize = 10.sp)
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF06B6D4))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Priorizar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Filtered Tasks Calculation & Timeline Schedule List
            val filteredTasks = remember(tasks, selectedFilter) {
                when (selectedFilter) {
                    "PENDING" -> tasks.filter { !it.isCompleted }
                    "HIGH" -> tasks.filter { it.priority == "HIGH" && !it.isCompleted }
                    else -> tasks
                }
            }

            // Brish shows a useful student-day by default, but expands when an
            // existing task falls outside it so scheduled items never disappear.
            val scheduledHours = filteredTasks.mapNotNull { plannerHour(it.time) }
            val firstHour = minOf(6, scheduledHours.minOrNull() ?: 6)
            val lastHour = maxOf(23, scheduledHours.maxOrNull() ?: 23)
            val hours = (firstHour..lastHour).toList()
            val timelineState = rememberLazyListState()

            // Al cambiar de fecha o iniciar el día, mostrar siempre de forma limpia desde el inicio
            LaunchedEffect(selectedDate) {
                timelineState.scrollToItem(0)
            }

            LazyColumn(
                state = timelineState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Empty State Motivational Card
                if (tasks.isEmpty()) {
                    item(key = "empty_state") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDark) Color(0xFF15171E) else Color.White,
                            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF1E212B) else Color(0xFFE8EEF8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.EventAvailable,
                                        contentDescription = null,
                                        tint = if (isDark) Color.White else Color(0xFF111827),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Día sin pendientes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No tienes tareas para esta fecha. Planifica tu día o disfruta tu tiempo libre.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showAddTaskSheet = true },
                                    shape = RoundedCornerShape(100.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                                    ),
                                    modifier = Modifier.height(38.dp).bounceClick()
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Planificar Tarea", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (filteredTasks.isEmpty()) {
                    item(key = "filter_empty_state") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF15171E) else Color.White,
                            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No hay tareas con este filtro",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = { selectedFilter = "ALL" }) {
                                    Text("Ver todas las tareas", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 1. Tareas del día sin horario fijo (al inicio para fácil visibilidad)
                val anytimeTasks = filteredTasks.filter { it.time.isNullOrBlank() || plannerHour(it.time) == null }
                if (anytimeTasks.isNotEmpty()) {
                    item(key = "anytime") {
                        TimelineHourRow(
                            hour = "Día",
                            tasks = anytimeTasks,
                            isCurrentHour = false,
                            isDark = isDark,
                            isLoadingAi = { subtaskLoadingStates[it] == true },
                            onToggleComplete = { viewModel.toggleTask(it) },
                            onToggleSubtask = { task, subId -> viewModel.toggleSubtask(task, subId) },
                            onAddSubtask = { task, text -> viewModel.addSubtask(task, text) },
                            onDeleteSubtask = { task, subId -> viewModel.deleteSubtask(task, subId) },
                            onGenerateSubtasksAi = { viewModel.generateSubtasksWithAI(it) },
                            onEditTask = { taskToEdit = it }
                        )
                    }
                }

                // 2. Franjas horarias ordenadas cronológicamente desde la mañana
                hours.forEach { hourValue ->
                    val hourStr = String.format(java.util.Locale.US, "%02d:00", hourValue)
                    val matchingTasks = filteredTasks.filter { task ->
                        plannerHour(task.time) == hourValue
                    }

                    item(key = hourStr) {
                        TimelineHourRow(
                            hour = hourStr,
                            tasks = matchingTasks,
                            isCurrentHour = isSameDay(selectedDate, System.currentTimeMillis()) && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == hourValue,
                            isDark = isDark,
                            isLoadingAi = { subtaskLoadingStates[it] == true },
                            onToggleComplete = { viewModel.toggleTask(it) },
                            onToggleSubtask = { task, subId -> viewModel.toggleSubtask(task, subId) },
                            onAddSubtask = { task, text -> viewModel.addSubtask(task, text) },
                            onDeleteSubtask = { task, subId -> viewModel.deleteSubtask(task, subId) },
                            onGenerateSubtasksAi = { viewModel.generateSubtasksWithAI(it) },
                            onEditTask = { taskToEdit = it }
                        )
                    }
                }
            }
        }
    }

    // Add Task Sheet
    if (showAddTaskSheet) {
        val draftTimeError = plannerTimeError(draftTaskTime, draftTaskEndTime)
        ModalBottomSheet(
            onDismissRequest = { showAddTaskSheet = false },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF15171E) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.EventNote, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column {
                            Text("Nueva tarea", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "PE")).format(Date(selectedDate)).replaceFirstChar { it.uppercase() },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            showAddTaskSheet = false
                            showBrainDumpDialog = true
                        }
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF06B6D4))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ayuda IA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = draftTaskTitle,
                    onValueChange = { draftTaskTitle = it },
                    label = { Text("¿Qué necesitas hacer?") },
                    placeholder = { Text("Ej. Terminar informe semanal") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Horario · opcional",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Time picker row (Start & End Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Time Picker
                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            AndroidTimePickerDialog(
                                context,
                                { _, h, m ->
                                    draftTaskTime = String.format(java.util.Locale.US, "%02d:%02d", h, m)
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color.White else Color(0xFF111827)
                        )
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(draftTaskTime?.let { "Inicio: $it" } ?: "Hora Inicio", fontSize = 12.sp)
                    }

                    // End Time Picker
                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            AndroidTimePickerDialog(
                                context,
                                { _, h, m ->
                                    draftTaskEndTime = String.format(java.util.Locale.US, "%02d:%02d", h, m)
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color.White else Color(0xFF111827)
                        )
                    ) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(draftTaskEndTime?.let { "Fin: $it" } ?: "Hora Fin", fontSize = 12.sp)
                    }

                    if (draftTaskTime != null || draftTaskEndTime != null) {
                        IconButton(
                            onClick = {
                                draftTaskTime = null
                                draftTaskEndTime = null
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Quitar horas", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (draftTimeError != null) {
                    Text(
                        draftTimeError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 5.dp)
                    )
                } else if (draftTaskTime != null) {
                    Text(
                        if (draftTaskEndTime == null) "La tarea quedará fijada a las $draftTaskTime." else "Bloque de $draftTaskTime a $draftTaskEndTime.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Priority Selector Row
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
                        val isSelected = draftTaskPriority == pKey
                        Surface(
                            onClick = { draftTaskPriority = pKey },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) pColor.copy(alpha = 0.2f) else if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6),
                            border = BorderStroke(1.dp, if (isSelected) pColor else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .bounceClick()
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

                Spacer(modifier = Modifier.height(12.dp))

                // Recurrence Selector Row
                Text(
                    text = "Repetición",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair("NONE", "Una vez"),
                        Pair("DAILY", "Diario"),
                        Pair("WEEKDAYS", "L - V")
                    ).forEach { (rKey, rLabel) ->
                        val isSelected = draftTaskRepeatMode == rKey
                        Surface(
                            onClick = { draftTaskRepeatMode = rKey },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) (if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)) else if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .bounceClick()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = rLabel,
                                    color = if (isSelected) (if (isDark) Color(0xFF111827) else Color.White) else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Subtasks preview in Add sheet
                Text(
                    text = "Subtareas (${draftTaskSubtasks.size})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                draftTaskSubtasks.forEachIndexed { idx, sub ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(sub, modifier = Modifier.weight(1f), fontSize = 13.sp)
                        IconButton(
                            onClick = { draftTaskSubtasks = draftTaskSubtasks.filterIndexed { i, _ -> i != idx } },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftNewSubtask,
                        onValueChange = { draftNewSubtask = it },
                        placeholder = { Text("Añadir paso o subtarea...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (draftNewSubtask.isNotBlank()) {
                                draftTaskSubtasks = draftTaskSubtasks + draftNewSubtask.trim()
                                draftNewSubtask = ""
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.AddCircle, contentDescription = "Agregar subtarea", tint = if (isDark) Color.White else Color(0xFF111827))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (draftTaskTitle.isNotBlank()) {
                            val (mode, repeatDays) = when (draftTaskRepeatMode) {
                                "DAILY" -> Pair("DAILY", emptyList<Int>())
                                "WEEKDAYS" -> Pair("CUSTOM", listOf(2, 3, 4, 5, 6))
                                else -> Pair("NONE", emptyList<Int>())
                            }
                            viewModel.addTask(
                                title = draftTaskTitle.trim(),
                                repeatMode = mode,
                                repeatDays = repeatDays,
                                time = draftTaskTime,
                                endTime = draftTaskEndTime,
                                category = draftTaskCategory,
                                priority = draftTaskPriority,
                                initialSubtasks = draftTaskSubtasks
                            )
                            draftTaskTitle = ""
                            draftTaskTime = null
                            draftTaskEndTime = null
                            draftTaskPriority = "MEDIUM"
                            draftTaskRepeatMode = "NONE"
                            draftTaskSubtasks = emptyList()
                            showAddTaskSheet = false
                            InAppNotificationBus.post("Tarea añadida")
                        }
                    },
                    enabled = draftTaskTitle.isNotBlank() && draftTimeError == null,
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Crear tarea", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    // Task Detail & Edit Sheet
    taskToEdit?.let { initialTask ->
        val liveTask = tasks.find { it.id == initialTask.id } ?: initialTask
        var editTitle by remember(initialTask.id) { mutableStateOf(initialTask.title) }
        var editPriority by remember(initialTask.id) { mutableStateOf(initialTask.priority) }
        var editRepeatMode by remember(initialTask.id) {
            mutableStateOf(
                when {
                    initialTask.repeatMode == "DAILY" -> "DAILY"
                    initialTask.repeatMode == "CUSTOM" || initialTask.repeatDays.isNotEmpty() -> "WEEKDAYS"
                    else -> "NONE"
                }
            )
        }
        var editTime by remember(initialTask.id) { mutableStateOf(initialTask.time) }
        var editEndTime by remember(initialTask.id) { mutableStateOf(initialTask.endTime) }
        var newSubtaskText by remember { mutableStateOf("") }
        val editTimeError = plannerTimeError(editTime, editEndTime)

        ModalBottomSheet(
            onDismissRequest = { taskToEdit = null },
            sheetState = editSheetState,
            containerColor = if (isDark) Color(0xFF15171E) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFE8EEF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.EditCalendar, contentDescription = null, tint = if (isDark) Color.White else Color(0xFF111827), modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column {
                            Text("Editar tarea", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Ajusta horario, prioridad o pasos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(
                        onClick = {
                            val deletedTask = liveTask
                            viewModel.deleteTask(deletedTask)
                            taskToEdit = null
                            InAppNotificationBus.post(
                                message = "Tarea eliminada",
                                actionLabel = "DESHACER",
                                onAction = { viewModel.restoreTask(deletedTask) }
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Horario · opcional",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Start & End Time picker row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            AndroidTimePickerDialog(
                                context,
                                { _, h, m ->
                                    editTime = String.format(java.util.Locale.US, "%02d:%02d", h, m)
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color.White else Color(0xFF111827)
                        )
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(editTime?.let { "Inicio: $it" } ?: "Hora Inicio", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            AndroidTimePickerDialog(
                                context,
                                { _, h, m ->
                                    editEndTime = String.format(java.util.Locale.US, "%02d:%02d", h, m)
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color.White else Color(0xFF111827)
                        )
                    ) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(editEndTime?.let { "Fin: $it" } ?: "Hora Fin", fontSize = 12.sp)
                    }

                    if (editTime != null || editEndTime != null) {
                        IconButton(
                            onClick = {
                                editTime = null
                                editEndTime = null
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Quitar horas", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (editTimeError != null) {
                    Text(
                        editTimeError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 5.dp)
                    )
                } else if (editTime != null) {
                    Text(
                        if (editEndTime == null) "La tarea quedará fijada a las $editTime." else "Bloque de $editTime a $editEndTime.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Priority Selector Row
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
                        val isSelected = editPriority == pKey
                        Surface(
                            onClick = { editPriority = pKey },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) pColor.copy(alpha = 0.2f) else if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6),
                            border = BorderStroke(1.dp, if (isSelected) pColor else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .bounceClick()
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

                Spacer(modifier = Modifier.height(12.dp))

                // Recurrence Selector Row
                Text(
                    text = "Repetición",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair("NONE", "Una vez"),
                        Pair("DAILY", "Diario"),
                        Pair("WEEKDAYS", "L - V")
                    ).forEach { (rKey, rLabel) ->
                        val isSelected = editRepeatMode == rKey
                        Surface(
                            onClick = { editRepeatMode = rKey },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) (if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)) else if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .bounceClick()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = rLabel,
                                    color = if (isSelected) (if (isDark) Color(0xFF111827) else Color.White) else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Subtasks Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subtareas (${liveTask.subtasks.count { it.isCompleted }}/${liveTask.subtasks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { viewModel.generateSubtasksWithAI(liveTask) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isDark) Color.White else Color(0xFF111827)
                        )
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isDark) Color.White else Color(0xFF111827))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sugerir con IA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                liveTask.subtasks.forEach { subtask ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.toggleSubtask(liveTask, subtask.id)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (subtask.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (subtask.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = subtask.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null
                            ),
                            color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp
                        )
                        IconButton(
                            onClick = { viewModel.deleteSubtask(liveTask, subtask.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Eliminar", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }

                // Add quick subtask row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = {
                            Text(
                                "Añadir subtarea...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0xFF1E212B) else Color.White,
                            unfocusedContainerColor = if (isDark) Color(0xFF1E212B) else Color.White,
                            focusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFD1D5DB),
                            unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E7EB)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                viewModel.addSubtask(liveTask, newSubtaskText.trim())
                                newSubtaskText = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827))
                            .bounceClick()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Agregar subtarea", tint = if (isDark) Color(0xFF111827) else Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val (mode, repeatDays) = when (editRepeatMode) {
                            "DAILY" -> Pair("DAILY", emptyList<Int>())
                            "WEEKDAYS" -> Pair("CUSTOM", listOf(2, 3, 4, 5, 6))
                            else -> Pair("NONE", emptyList<Int>())
                        }
                        viewModel.updateTask(
                            liveTask.copy(
                                title = editTitle.trim(),
                                time = editTime,
                                endTime = editEndTime,
                                priority = editPriority,
                                repeatMode = mode,
                                repeatDays = repeatDays
                            )
                        )
                        taskToEdit = null
                        InAppNotificationBus.post("Tarea actualizada")
                    },
                    enabled = editTitle.isNotBlank() && editTimeError == null,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    )
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Brain Dump con IA Dialog
    if (showBrainDumpDialog) {
        SmoothDialog(
            onDismissRequest = { if (!isBrainDumpLoading) showBrainDumpDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = if (isDark) Color(0xFF15171E) else Color.White,
                shadowElevation = if (isDark) 0.dp else 12.dp,
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E212B) else Color(0xFFE8EEF8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isDark) Color.White else Color(0xFF111827),
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Organizar pendientes",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Con Brish AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!isBrainDumpLoading) {
                            IconButton(onClick = { showBrainDumpDialog = false }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Escribe tus pendientes como te salgan. Brish identificará tareas, fechas y horarios antes de añadirlos a tu agenda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = brainDumpText,
                        onValueChange = { brainDumpText = it },
                        placeholder = {
                            Text(
                                "Ej. Mañana a las 10 entregar el informe; el viernes pagar servicios y llamar al dentista.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isBrainDumpLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) Color.White else Color(0xFF111827),
                            unfocusedBorderColor = if (isDark) Color.White.copy(alpha = .10f) else Color(0xFFE5E7EB),
                            focusedContainerColor = if (isDark) Color(0xFF1E212B) else Color(0xFFF7F8FA),
                            unfocusedContainerColor = if (isDark) Color(0xFF1E212B) else Color(0xFFF7F8FA)
                        )
                    )

                    if (brainDumpError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = brainDumpError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.generateBrainDump(brainDumpText) {
                                brainDumpText = ""
                                showBrainDumpDialog = false
                                InAppNotificationBus.post("Tareas organizadas en tu agenda")
                            }
                        },
                        enabled = brainDumpText.isNotBlank() && !isBrainDumpLoading,
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
                        if (isBrainDumpLoading) {
                            CircularProgressIndicator(
                                color = if (isDark) Color(0xFF111827) else Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analizando y organizando...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Organizar mi agenda", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineHourRow(
    hour: String,
    tasks: List<Task>,
    isCurrentHour: Boolean = false,
    isDark: Boolean,
    isLoadingAi: (String) -> Boolean,
    onToggleComplete: (Task) -> Unit,
    onToggleSubtask: (Task, String) -> Unit,
    onAddSubtask: (Task, String) -> Unit,
    onDeleteSubtask: (Task, String) -> Unit,
    onGenerateSubtasksAi: (Task) -> Unit,
    onEditTask: (Task) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Hour Label on the left
        Column(modifier = Modifier.width(44.dp)) {
            Text(
                text = hour,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = if (isCurrentHour) Color(0xFF0891B2) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isCurrentHour) {
                Text("Ahora", color = Color(0xFF0891B2), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Right side: Dashed timeline divider or task cards
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (tasks.isEmpty()) {
                // Subtle horizontal dashed line when no tasks
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                ) {
                    drawLine(
                        color = if (isCurrentHour) Color(0xFF06B6D4) else if (isDark) Color(0xFF262933) else Color(0xFFE5E7EB),
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            } else {
                tasks.forEach { task ->
                    TimelineTaskCard(
                        task = task,
                        isDark = isDark,
                        isAiLoading = isLoadingAi(task.id),
                        onToggle = { onToggleComplete(task) },
                        onToggleSubtask = { subId -> onToggleSubtask(task, subId) },
                        onAddSubtask = { text -> onAddSubtask(task, text) },
                        onDeleteSubtask = { subId -> onDeleteSubtask(task, subId) },
                        onGenerateSubtasksAi = { onGenerateSubtasksAi(task) },
                        onEdit = { onEditTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineTaskCard(
    task: Task,
    isDark: Boolean,
    isAiLoading: Boolean,
    onToggle: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onAddSubtask: (String) -> Unit,
    onDeleteSubtask: (String) -> Unit,
    onGenerateSubtasksAi: () -> Unit,
    onEdit: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var inlineSubtaskInput by remember { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current

    val accentColor = remember(task.category, task.priority) {
        when {
            task.priority == "HIGH" -> Color(0xFFEF4444)
            task.category.contains("Estudio", ignoreCase = true) -> Color(0xFF3B82F6)
            task.category.contains("Salud", ignoreCase = true) -> Color(0xFF10B981)
            task.category.contains("Gym", ignoreCase = true) || task.category.contains("Ejercicio", ignoreCase = true) -> Color(0xFF06B6D4)
            else -> Color(0xFF10B981)
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (task.isCompleted) {
            if (isDark) Color(0xFF101116).copy(alpha = 0.5f) else Color(0xFFF9FAFB)
        } else {
            if (isDark) Color(0xFF15171E) else Color.White
        },
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Task Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { isExpanded = !isExpanded },
                        onLongClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onEdit()
                        }
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Accent Bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (task.isCompleted) Color.Gray.copy(alpha = 0.4f) else accentColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Task Title & Tags
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                        ),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Subtasks Pill (Clickable badge)
                        val completedSub = task.subtasks.count { it.isCompleted }
                        Surface(
                            onClick = { isExpanded = !isExpanded },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFE5E7EB)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (task.subtasks.isEmpty()) Icons.Rounded.Add else Icons.Rounded.Checklist,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (task.subtasks.isEmpty()) "Subtareas" else "$completedSub/${task.subtasks.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val timeDisplay = remember(task.time, task.endTime) {
                            when {
                                !task.time.isNullOrBlank() && !task.endTime.isNullOrBlank() -> "${task.time} - ${task.endTime}"
                                !task.time.isNullOrBlank() -> task.time
                                !task.endTime.isNullOrBlank() -> "Hasta ${task.endTime}"
                                else -> null
                            }
                        }

                        if (timeDisplay != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)
                            ) {
                                Text(
                                    text = timeDisplay,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (task.priority == "HIGH") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "ALTA",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFEF4444),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (task.priority == "LOW") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)
                            ) {
                                Text(
                                    text = "BAJA",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (task.repeatMode != "NONE") {
                            Icon(
                                Icons.Rounded.Repeat,
                                contentDescription = "Recurrente",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Checkbox on right
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onToggle()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        if (task.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = "Completar tarea",
                        tint = if (task.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Inline Expandable Subtasks List
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(150)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(tween(150)) + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF12131A) else Color(0xFFF9FAFB))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Subtasks Header with AI button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SUBTAREAS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAiLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = if (isDark) Color.White else Color(0xFF111827)
                                )
                            } else {
                                TextButton(
                                    onClick = onGenerateSubtasksAi,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (isDark) Color.White else Color(0xFF111827)
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (isDark) Color.White else Color(0xFF111827)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("IA Pasos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Editar", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtasks items
                    if (task.subtasks.isEmpty()) {
                        Text(
                            text = "No hay subtareas aún. Escribe una abajo o pulsa 'IA Pasos'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        task.subtasks.forEach { subtask ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        onToggleSubtask(subtask.id)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (subtask.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (subtask.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = subtask.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
                                        fontSize = 13.sp
                                    ),
                                    color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDeleteSubtask(subtask.id) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Eliminar",
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Inline add subtask input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inlineSubtaskInput,
                            onValueChange = { inlineSubtaskInput = it },
                            placeholder = {
                                Text(
                                    "Añadir subtarea...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (isDark) Color(0xFF1E212B) else Color.White,
                                unfocusedContainerColor = if (isDark) Color(0xFF1E212B) else Color.White,
                                focusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFD1D5DB),
                                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E7EB)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inlineSubtaskInput.isNotBlank()) {
                                    onAddSubtask(inlineSubtaskInput.trim())
                                    inlineSubtaskInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827))
                                .bounceClick()
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Añadir",
                                tint = if (isDark) Color(0xFF111827) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isSameDay(date1: Long, date2: Long): Boolean {
    return com.katchy.focuslive.util.HabitUtils.isSameDay(date1, date2)
}

private fun plannerHour(time: String?): Int? = time
    ?.trim()
    ?.substringBefore(":")
    ?.toIntOrNull()
    ?.takeIf { it in 0..23 }

private fun plannerTimeMinutes(time: String?): Int? {
    if (time.isNullOrBlank()) return null
    val parts = time.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun plannerTimeError(start: String?, end: String?): String? {
    if (end != null && start == null) return "Elige primero una hora de inicio."
    if (start != null && plannerTimeMinutes(start) == null) return "La hora de inicio no es válida."
    if (end != null && plannerTimeMinutes(end) == null) return "La hora de fin no es válida."
    val startMinutes = plannerTimeMinutes(start)
    val endMinutes = plannerTimeMinutes(end)
    return if (startMinutes != null && endMinutes != null && endMinutes <= startMinutes) {
        "La hora de fin debe ser posterior al inicio."
    } else null
}
