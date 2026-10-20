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
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishWarning
import com.katchy.focuslive.ui.theme.BrishYellowSoft
import com.katchy.focuslive.ui.util.InAppNotificationBus
import com.katchy.focuslive.ui.util.bounceClick
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PlannerScreen(
    onBack: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onOpenReview: () -> Unit = {},
    onOpenNote: (String) -> Unit = {},
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
    val linkedNotes by viewModel.notesForSelectedDate.collectAsState()
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
        onMove = { task, start -> viewModel.moveTask(task, start) },
        onReview = onOpenReview,
        linkedNotes = linkedNotes,
        onOpenNote = onOpenNote
    )

    // Add Task Sheet
    if (showAddTaskSheet) {
        val draftTimeError = plannerTimeError(draftTaskTime, draftTaskEndTime)
        ModalBottomSheet(
            onDismissRequest = { showAddTaskSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
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
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(BrishYellowSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.EventNote, contentDescription = null, tint = BrishInk, modifier = Modifier.size(21.dp))
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
                    shape = RoundedCornerShape(18.dp),
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
                            contentColor = MaterialTheme.colorScheme.onSurface
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
                            contentColor = MaterialTheme.colorScheme.onSurface
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
                AreaSelector(draftTaskCategory) { draftTaskCategory = it }
                Spacer(modifier = Modifier.height(12.dp))
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
                        Triple("LOW", "Baja", Color(0xFF6E655A)),
                        Triple("MEDIUM", "Media", BrishWarning),
                        Triple("HIGH", "Alta", MaterialTheme.colorScheme.error)
                    ).forEach { (pKey, pLabel, pColor) ->
                        val isSelected = draftTaskPriority == pKey
                        Surface(
                            onClick = { draftTaskPriority = pKey },
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) pColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
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
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .bounceClick()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = rLabel,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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
                        Icon(Icons.Rounded.AddCircle, contentDescription = "Agregar subtarea", tint = MaterialTheme.colorScheme.onSurface)
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
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
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
        var editCategory by remember(initialTask.id) { mutableStateOf(initialTask.category) }
        var editEndTime by remember(initialTask.id) { mutableStateOf(initialTask.endTime) }
        var newSubtaskText by remember { mutableStateOf("") }
        val editTimeError = plannerTimeError(editTime, editEndTime)

        ModalBottomSheet(
            onDismissRequest = { taskToEdit = null },
            sheetState = editSheetState,
            containerColor = MaterialTheme.colorScheme.background
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
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(BrishYellowSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(21.dp))
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
                            contentColor = MaterialTheme.colorScheme.onSurface
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
                            contentColor = MaterialTheme.colorScheme.onSurface
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
                AreaSelector(editCategory) { editCategory = it }
                Spacer(modifier = Modifier.height(12.dp))
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
                        Triple("LOW", "Baja", Color(0xFF6E655A)),
                        Triple("MEDIUM", "Media", BrishWarning),
                        Triple("HIGH", "Alta", MaterialTheme.colorScheme.error)
                    ).forEach { (pKey, pLabel, pColor) ->
                        val isSelected = editPriority == pKey
                        Surface(
                            onClick = { editPriority = pKey },
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) pColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
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
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .bounceClick()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = rLabel,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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
                                tint = if (subtask.isCompleted) AreaHealth else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
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
                            .background(MaterialTheme.colorScheme.primary)
                            .bounceClick()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Agregar subtarea", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
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
                                category = editCategory,
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
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold)
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
