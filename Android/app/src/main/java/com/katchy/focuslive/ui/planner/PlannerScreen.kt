@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.katchy.focuslive.ui.planner

import android.app.TimePickerDialog as AndroidTimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.ui.components.BrishChoiceChip
import com.katchy.focuslive.ui.components.BrishPrimaryButton
import com.katchy.focuslive.ui.components.BrishPullToRefreshBox
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.BrishSecondaryButton
import com.katchy.focuslive.ui.components.BrishTextField
import com.katchy.focuslive.ui.components.DraftRecoveryNotice
import com.katchy.focuslive.ui.components.rememberPersistentDraftText
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishMuted
import com.katchy.focuslive.ui.theme.BrishWarning
import com.katchy.focuslive.ui.theme.BrishYellow
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
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    LaunchedEffect(error) { error?.let { InAppNotificationBus.post(it) } }

    var showAddTaskSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    // Draft states for new task
    val addDraftPrefix = "planner:$selectedDate:add"
    val addTitleDraft = rememberPersistentDraftText("$addDraftPrefix:title")
    val addCategoryDraft = rememberPersistentDraftText("$addDraftPrefix:category", "General")
    val addPriorityDraft = rememberPersistentDraftText("$addDraftPrefix:priority", "MEDIUM")
    val addRepeatDraft = rememberPersistentDraftText("$addDraftPrefix:repeat", "NONE")
    val addTimeDraft = rememberPersistentDraftText("$addDraftPrefix:start-time")
    val addEndTimeDraft = rememberPersistentDraftText("$addDraftPrefix:end-time")
    val addSubtaskDraft = rememberPersistentDraftText("$addDraftPrefix:new-subtask")
    val addSubtasksDraft = rememberPersistentDraftText("$addDraftPrefix:subtasks")
    var draftTaskTitle by addTitleDraft::value
    var draftTaskCategory by addCategoryDraft::value
    var draftTaskPriority by addPriorityDraft::value
    var draftTaskRepeatMode by addRepeatDraft::value
    var draftTaskTime by addTimeDraft::value
    var draftTaskEndTime by addEndTimeDraft::value
    var draftNewSubtask by addSubtaskDraft::value
    var draftTaskSubtasks by remember(addDraftPrefix) { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(addSubtasksDraft.value) { draftTaskSubtasks = addSubtasksDraft.value.split('\n').filter(String::isNotBlank) }
    LaunchedEffect(draftTaskSubtasks) { addSubtasksDraft.value = draftTaskSubtasks.joinToString("\n") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

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

    val week by viewModel.weekTasks.collectAsState()
    val linkedNotes by viewModel.notesForSelectedDate.collectAsState()

    BrishPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    ) {
        AgendaContent(
            selectedDate = selectedDate,
            tasks = tasks,
            week = week,
            onSelectDate = { viewModel.selectDate(it) },
            onAdd = { start, end ->
                draftTaskTime = start?.let { hhmm(it) }.orEmpty()
                draftTaskEndTime = end?.let { hhmm(it) }.orEmpty()
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
    }

    // Add Task Sheet
    if (showAddTaskSheet) {
        val draftTimeError = plannerTimeError(draftTaskTime.takeIf(String::isNotBlank), draftTaskEndTime.takeIf(String::isNotBlank))
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
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                DraftRecoveryNotice(addTitleDraft, Modifier.fillMaxWidth())

                BrishTextField(
                    value = draftTaskTitle,
                    onValueChange = { draftTaskTitle = it },
                    placeholder = "Ej. Terminar informe semanal",
                    label = "¿Qué necesitas hacer?"
                )

                Spacer(modifier = Modifier.height(14.dp))

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
                    BrishSecondaryButton(
                        text = draftTaskTime.takeIf(String::isNotBlank)?.let { "Inicio: $it" } ?: "Hora inicio",
                        leadingIcon = Icons.Rounded.Schedule,
                        onClick = {
                            val c = Calendar.getInstance()
                            AndroidTimePickerDialog(
                                context,
                                { _, h, m ->
                                    draftTaskTime = String.format(Locale.US, "%02d:%02d", h, m)
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    BrishSecondaryButton(
                        text = draftTaskEndTime.takeIf(String::isNotBlank)?.let { "Fin: $it" } ?: "Hora fin",
                        leadingIcon = Icons.Rounded.Timer,
                        onClick = {
                            val c = Calendar.getInstance()
                            AndroidTimePickerDialog(
                                context,
                                { _, h, m ->
                                    draftTaskEndTime = String.format(Locale.US, "%02d:%02d", h, m)
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (draftTaskTime.isNotBlank() || draftTaskEndTime.isNotBlank()) {
                        BrishRoundButton(
                            icon = Icons.Rounded.Close,
                            description = "Quitar horas",
                            onClick = {
                                draftTaskTime = ""
                                draftTaskEndTime = ""
                            },
                            size = 40.dp
                        )
                    }
                }

                if (draftTimeError != null) {
                    Text(
                        draftTimeError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 5.dp)
                    )
                } else if (draftTaskTime.isNotBlank()) {
                    Text(
                        if (draftTaskEndTime.isBlank()) "La tarea quedará fijada a las $draftTaskTime." else "Bloque de $draftTaskTime a $draftTaskEndTime.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Area & Priority Selector Row
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
                        Triple("LOW", "Baja", BrishMuted),
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
                                .height(38.dp)
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
                        BrishChoiceChip(
                            label = rLabel,
                            selected = draftTaskRepeatMode == rKey,
                            onClick = { draftTaskRepeatMode = rKey },
                            modifier = Modifier.weight(1f)
                        )
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
                        Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

                DraftRecoveryNotice(addSubtaskDraft, Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrishTextField(
                        value = draftNewSubtask,
                        onValueChange = { draftNewSubtask = it },
                        placeholder = "Añadir paso o subtarea...",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BrishRoundButton(
                        icon = Icons.Rounded.Add,
                        description = "Agregar subtarea",
                        onClick = {
                            if (draftNewSubtask.isNotBlank()) {
                                draftTaskSubtasks = draftTaskSubtasks + draftNewSubtask.trim()
                                addSubtaskDraft.clearAfterSave()
                            }
                        },
                        size = 44.dp,
                        container = BrishYellow
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                BrishPrimaryButton(
                    text = "Crear tarea",
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
                                time = draftTaskTime.takeIf(String::isNotBlank),
                                endTime = draftTaskEndTime.takeIf(String::isNotBlank),
                                category = draftTaskCategory,
                                priority = draftTaskPriority,
                                initialSubtasks = draftTaskSubtasks,
                                onSuccess = {
                                    draftTaskTitle = ""
                                    draftTaskTime = ""
                                    draftTaskEndTime = ""
                                    draftTaskPriority = "MEDIUM"
                                    draftTaskRepeatMode = "NONE"
                                    draftTaskSubtasks = emptyList()
                                    addTitleDraft.clearStored()
                                    addCategoryDraft.clearStored()
                                    addPriorityDraft.clearStored()
                                    addRepeatDraft.clearStored()
                                    addTimeDraft.clearStored()
                                    addEndTimeDraft.clearStored()
                                    addSubtaskDraft.clearAfterSave()
                                    addSubtasksDraft.clearAfterSave()
                                    showAddTaskSheet = false
                                    InAppNotificationBus.post("Tarea añadida")
                                }
                            )
                        }
                    },
                    enabled = !isSaving && draftTaskTitle.isNotBlank() && draftTimeError == null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Task Detail & Edit Sheet
    taskToEdit?.let { initialTask ->
        val liveTask = tasks.find { it.id == initialTask.id } ?: initialTask
        val editPrefix = "planner:${initialTask.id}:edit"
        val editTitleDraft = rememberPersistentDraftText("$editPrefix:title", initialTask.title)
        val editPriorityDraft = rememberPersistentDraftText("$editPrefix:priority", initialTask.priority)
        val initialRepeatMode = when {
            initialTask.repeatMode == "DAILY" -> "DAILY"
            initialTask.repeatMode == "CUSTOM" && initialTask.repeatDays == listOf(2, 3, 4, 5, 6) -> "WEEKDAYS"
            initialTask.repeatMode in listOf("WEEKLY", "CUSTOM") -> "EXISTING"
            else -> "NONE"
        }
        val editRepeatDraft = rememberPersistentDraftText("$editPrefix:repeat", initialRepeatMode)
        val editTimeDraft = rememberPersistentDraftText("$editPrefix:start-time", initialTask.time.orEmpty())
        val editCategoryDraft = rememberPersistentDraftText("$editPrefix:category", initialTask.category)
        val editEndTimeDraft = rememberPersistentDraftText("$editPrefix:end-time", initialTask.endTime.orEmpty())
        val newSubtaskDraft = rememberPersistentDraftText("$editPrefix:new-subtask")
        var editTitle by editTitleDraft::value
        var editPriority by editPriorityDraft::value
        var editRepeatMode by editRepeatDraft::value
        var editTime by editTimeDraft::value
        var editCategory by editCategoryDraft::value
        var editEndTime by editEndTimeDraft::value
        var newSubtaskText by newSubtaskDraft::value
        val editTimeError = plannerTimeError(editTime.takeIf(String::isNotBlank), editEndTime.takeIf(String::isNotBlank))

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
                if (editTitleDraft.recovered || editPriorityDraft.recovered || editRepeatDraft.recovered || editTimeDraft.recovered || editCategoryDraft.recovered || editEndTimeDraft.recovered || newSubtaskDraft.recovered) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Borrador recuperado", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = {
                            editTitleDraft.discard(); editPriorityDraft.discard(); editRepeatDraft.discard()
                            editTimeDraft.discard(); editCategoryDraft.discard(); editEndTimeDraft.discard(); newSubtaskDraft.discard()
                            taskToEdit = null
                        }) { Text("Descartar") }
                    }
                }
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
                            Icon(Icons.Rounded.EditCalendar, contentDescription = null, tint = BrishInk, modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column {
                            Text("Editar tarea", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Ajusta horario, prioridad o pasos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                BrishTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    placeholder = "Título de la tarea",
                    label = "Título"
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
                    BrishSecondaryButton(
                        text = editTime.takeIf(String::isNotBlank)?.let { "Inicio: $it" } ?: "Hora inicio",
                        leadingIcon = Icons.Rounded.Schedule,
                        onClick = {
                            val c = Calendar.getInstance()
                            AndroidTimePickerDialog(
                                context,
                                { _, h, m ->
                                    editTime = String.format(Locale.US, "%02d:%02d", h, m)
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    BrishSecondaryButton(
                        text = editEndTime.takeIf(String::isNotBlank)?.let { "Fin: $it" } ?: "Hora fin",
                        leadingIcon = Icons.Rounded.Timer,
                        onClick = {
                            val c = Calendar.getInstance()
                            AndroidTimePickerDialog(
                                context,
                                { _, h, m ->
                                    editEndTime = String.format(Locale.US, "%02d:%02d", h, m)
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (editTime.isNotBlank() || editEndTime.isNotBlank()) {
                        BrishRoundButton(
                            icon = Icons.Rounded.Close,
                            description = "Quitar horas",
                            onClick = {
                                editTime = ""
                                editEndTime = ""
                            },
                            size = 40.dp
                        )
                    }
                }

                if (editTimeError != null) {
                    Text(
                        editTimeError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 5.dp)
                    )
                } else if (editTime.isNotBlank()) {
                    Text(
                        if (editEndTime.isBlank()) "La tarea quedará fijada a las $editTime." else "Bloque de $editTime a $editEndTime.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Area & Priority Selector Row
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
                        Triple("LOW", "Baja", BrishMuted),
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
                                .height(38.dp)
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
                        BrishChoiceChip(
                            label = rLabel,
                            selected = editRepeatMode == rKey,
                            onClick = { editRepeatMode = rKey },
                            modifier = Modifier.weight(1f)
                        )
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
                DraftRecoveryNotice(newSubtaskDraft, Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrishTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = "Añadir subtarea...",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BrishRoundButton(
                        icon = Icons.Rounded.Add,
                        description = "Agregar subtarea",
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                viewModel.addSubtask(liveTask, newSubtaskText.trim())
                                newSubtaskDraft.clearAfterSave()
                            }
                        },
                        size = 44.dp,
                        container = BrishYellow
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                BrishPrimaryButton(
                    text = "Guardar cambios",
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
                                time = editTime.takeIf(String::isNotBlank),
                                endTime = editEndTime.takeIf(String::isNotBlank),
                                priority = editPriority,
                                category = editCategory,
                                repeatMode = mode,
                                repeatDays = repeatDays
                            ),
                            onSuccess = {
                                editTitleDraft.clearAfterSave()
                                editPriorityDraft.clearStored(); editRepeatDraft.clearStored(); editTimeDraft.clearStored()
                                editCategoryDraft.clearStored(); editEndTimeDraft.clearStored(); newSubtaskDraft.clearAfterSave()
                                taskToEdit = null
                                InAppNotificationBus.post("Tarea actualizada")
                            }
                        )
                    },
                    enabled = !isSaving && editTitle.isNotBlank() && editTimeError == null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
