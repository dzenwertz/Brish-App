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
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.SubtaskItem
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.ui.components.BrishTopHeader
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.InAppNotificationBus
import com.katchy.focuslive.ui.util.bounceClick
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PlannerScreen(
    onBack: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: PlannerViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasks by viewModel.tasksForSelectedDate.collectAsState()
    val error by viewModel.error.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    LaunchedEffect(error) { error?.let { InAppNotificationBus.post(it) } }

    var showAddTaskSheet by remember { mutableStateOf(false) }
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

    val week by viewModel.weekTasks.collectAsState()
    AgendaContent(
        selectedDate = selectedDate,
        tasks = tasks,
        week = week,
        onSelectDate = { viewModel.selectDate(it) },
        onAdd = { start, end ->
            draftTaskTime = start?.let { hhmm(it) }
            draftTaskEndTime = end?.let { hhmm(it) }
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            showAddTaskSheet = true
        },
        onEdit = { taskToEdit = it },
        onToggle = { viewModel.toggleTask(it) },
        onFocus = { viewModel.beginFocus(it); onNavigateToFocus() },
        onMove = { task, start -> viewModel.moveTask(task, start) }
    )

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
                                initialSubtasks = draftTaskSubtasks,
                                onSuccess = {
                                    draftTaskTitle = ""
                                    draftTaskTime = null
                                    draftTaskEndTime = null
                                    draftTaskPriority = "MEDIUM"
                                    draftTaskRepeatMode = "NONE"
                                    draftTaskSubtasks = emptyList()
                                    showAddTaskSheet = false
                                    InAppNotificationBus.post("Tarea añadida")
                                }
                            )
                        }
                    },
                    enabled = !isSaving && draftTaskTitle.isNotBlank() && draftTimeError == null,
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
                    initialTask.repeatMode == "CUSTOM" && initialTask.repeatDays == listOf(2, 3, 4, 5, 6) -> "WEEKDAYS"
                    initialTask.repeatMode in listOf("WEEKLY", "CUSTOM") -> "EXISTING"
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
                            viewModel.deleteTask(deletedTask) {
                                taskToEdit = null
                                InAppNotificationBus.post(
                                    message = "Tarea eliminada",
                                    actionLabel = "DESHACER",
                                    onAction = { viewModel.restoreTask(deletedTask) }
                                )
                            }
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

                if (editRepeatMode == "EXISTING") {
                    Text("Se conservan los días de repetición configurados.", fontSize = 12.sp)
                }
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
                            "EXISTING" -> Pair(initialTask.repeatMode, initialTask.repeatDays)
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
                            ),
                            onSuccess = {
                                taskToEdit = null
                                InAppNotificationBus.post("Tarea actualizada")
                            }
                        )
                    },
                    enabled = !isSaving && editTitle.isNotBlank() && editTimeError == null,
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

}

@Composable
fun TimelineHourRow(
    hour: String,
    tasks: List<Task>,
    isCurrentHour: Boolean = false,
    isDark: Boolean,
    onToggleComplete: (Task) -> Unit,
    onToggleSubtask: (Task, String) -> Unit,
    onAddSubtask: (Task, String) -> Unit,
    onDeleteSubtask: (Task, String) -> Unit,
    onEditTask: (Task) -> Unit,
    onFocus: (Task) -> Unit
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
                        onToggle = { onToggleComplete(task) },
                        onToggleSubtask = { subId -> onToggleSubtask(task, subId) },
                        onAddSubtask = { text -> onAddSubtask(task, text) },
                        onDeleteSubtask = { subId -> onDeleteSubtask(task, subId) },
                        onEdit = { onEditTask(task) },
                        onFocus = { onFocus(task) }
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
    onToggle: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onAddSubtask: (String) -> Unit,
    onDeleteSubtask: (String) -> Unit,
    onEdit: () -> Unit,
    onFocus: () -> Unit
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

                if (!task.isCompleted) {
                    IconButton(onClick = onFocus, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Enfocarme en esta actividad", tint = accentColor)
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
                    // Subtasks header
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
                            text = "No hay subtareas aún. Escribe una abajo.",
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
