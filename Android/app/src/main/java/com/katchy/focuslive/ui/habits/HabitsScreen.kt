package com.katchy.focuslive.ui.habits

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.ui.components.BrishTopHeader
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.theme.AntiPrimary
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.InAppNotificationBus
import com.katchy.focuslive.ui.util.bounceClick
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onBack: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HabitViewModel = hiltViewModel()
) {
    val habits by viewModel.habits.collectAsState()

    var showAddHabitSheet by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }

    var draftHabitTitle by rememberSaveable { mutableStateOf("") }
    var draftHabitReminder by rememberSaveable { mutableStateOf("") }
    var draftHabitDays by rememberSaveable { mutableStateOf(listOf(1, 2, 3, 4, 5, 6, 7)) }

    val isDark = isDarkTheme()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var todayTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                todayTimestamp = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val todayCalendar = remember(todayTimestamp) { Calendar.getInstance().apply { timeInMillis = todayTimestamp } }
    val todayDayOfWeek = todayCalendar.get(Calendar.DAY_OF_WEEK)
    val todayHabits = habits.filter { it.daysOfWeek.isEmpty() || todayDayOfWeek in it.daysOfWeek }

    val completedTodayCount = remember(todayHabits, todayTimestamp) {
        todayHabits.count { habit ->
            habit.completedDates.any { timestamp ->
                com.katchy.focuslive.util.HabitUtils.isSameDay(timestamp, todayTimestamp)
            }
        }
    }

    val totalHabits = todayHabits.size
    val progressPercent = if (totalHabits > 0) ((completedTodayCount.toFloat() / totalHabits) * 100).toInt() else 0

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BrishTopHeader(
                title = "Hábitos",
                onBack = onBack,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToChat = onNavigateToChat
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {

            // 2. Goal Progress Card (Image 4 Reference)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF15171E) else Color.White,
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Meta diaria",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$completedTodayCount de $totalHabits completados",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val targetProgress = if (totalHabits > 0) completedTodayCount.toFloat() / totalHabits else 0f
                        val animatedProgress by animateFloatAsState(
                            targetValue = targetProgress,
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                            label = "habitProgress"
                        )

                        // Linear Progress Bar with Apple-style Spring Animation
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(100.dp)),
                            color = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                            trackColor = if (isDark) Color(0xFF1E212B) else Color(0xFFE5E7EB)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = when {
                                totalHabits == 0 -> "Sin hábitos programados para hoy"
                                progressPercent >= 100 -> "100% - ¡Completaste todos tus hábitos hoy!"
                                progressPercent >= 50 -> "$progressPercent% - ¡Buen ritmo, sigue así!"
                                else -> "$progressPercent% - ¡Comienza tu jornada con fuerza!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 3. Habit Cards List (Image 4 Reference)
            if (habits.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF15171E) else Color.White,
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
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
                                Icon(Icons.Rounded.Spa, contentDescription = null, tint = if (isDark) Color.White else Color(0xFF111827), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Comienza un nuevo hábito",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Construye tu disciplina paso a paso y mantén tu racha activa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showAddHabitSheet = true },
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color.White else Color(0xFF111827),
                                    contentColor = if (isDark) Color.Black else Color.White
                                ),
                                modifier = Modifier.height(36.dp).bounceClick()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Crear hábito", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(habits, key = { it.id }) { habit ->
                    val isCompletedToday = habit.completedDates.any { timestamp ->
                        com.katchy.focuslive.util.HabitUtils.isSameDay(timestamp, todayCalendar.timeInMillis)
                    }

                    HabitItemCard(
                        habit = habit,
                        isCompleted = isCompletedToday,
                        isDark = isDark,
                        onToggle = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.toggleHabitDate(habit.id, System.currentTimeMillis())
                        },
                        onEdit = { habitToEdit = habit },
                        isScheduledToday = habit.daysOfWeek.isEmpty() || todayDayOfWeek in habit.daysOfWeek
                    )
                }
            }

            // 4. Add Habit Button at bottom
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showAddHabitSheet = true
                    },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color.White else Color(0xFF111827),
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .bounceClick()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Añadir hábito", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    // Add Habit Bottom Sheet
    if (showAddHabitSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddHabitSheet = false },
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
                Text(
                    text = "Nuevo hábito",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = draftHabitTitle,
                    onValueChange = { draftHabitTitle = it },
                    label = { Text("Nombre del hábito") },
                    placeholder = { Text("Ej. Leer 20 minutos") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                HabitReminderField(
                    value = draftHabitReminder,
                    onValueChange = { draftHabitReminder = it },
                    onPickTime = {
                        showHabitTimePicker(context, draftHabitReminder) { draftHabitReminder = it }
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))
                Text("Días", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(9.dp))
                HabitDayPicker(
                    selectedDays = draftHabitDays,
                    onToggleDay = { day ->
                        draftHabitDays = if (day in draftHabitDays && draftHabitDays.size > 1) {
                            draftHabitDays - day
                        } else if (day !in draftHabitDays) {
                            (draftHabitDays + day).sorted()
                        } else draftHabitDays
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (draftHabitTitle.isNotBlank()) {
                            viewModel.addHabit(
                                title = draftHabitTitle.trim(),
                                reminderTime = draftHabitReminder.trim().ifBlank { null },
                                daysOfWeek = draftHabitDays
                            )
                            draftHabitTitle = ""
                            draftHabitReminder = ""
                            draftHabitDays = listOf(1, 2, 3, 4, 5, 6, 7)
                            showAddHabitSheet = false
                            InAppNotificationBus.post("Hábito creado")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    )
                ) {
                    Text("Guardar hábito", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Edit Habit Bottom Sheet
    habitToEdit?.let { habit ->
        var editTitle by remember(habit.id) { mutableStateOf(habit.title) }
        var editReminder by remember(habit.id) { mutableStateOf(habit.reminderTime ?: "") }
        var editDays by remember(habit.id) { mutableStateOf(habit.daysOfWeek) }

        ModalBottomSheet(
            onDismissRequest = { habitToEdit = null },
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
                    Text("Editar hábito", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = {
                            val deletedHabit = habit
                            viewModel.deleteHabit(deletedHabit)
                            habitToEdit = null
                            InAppNotificationBus.post(
                                message = "Hábito eliminado",
                                actionLabel = "DESHACER",
                                onAction = { viewModel.restoreHabit(deletedHabit) }
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
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                HabitReminderField(
                    value = editReminder,
                    onValueChange = { editReminder = it },
                    onPickTime = {
                        showHabitTimePicker(context, editReminder) { editReminder = it }
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))
                Text("Días", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(9.dp))
                HabitDayPicker(
                    selectedDays = editDays,
                    onToggleDay = { day ->
                        editDays = if (day in editDays && editDays.size > 1) {
                            editDays - day
                        } else if (day !in editDays) {
                            (editDays + day).sorted()
                        } else editDays
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (editTitle.trim().isBlank()) {
                            InAppNotificationBus.post("El nombre no puede estar vacío")
                            return@Button
                        }
                        viewModel.updateHabitDetails(
                            habitId = habit.id,
                            newTitle = editTitle.trim(),
                            newReminder = editReminder.trim().ifBlank { null },
                            newDaysOfWeek = editDays
                        )
                        habitToEdit = null
                        InAppNotificationBus.post("Hábito actualizado")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    )
                ) {
                    Text("Guardar cambios", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HabitItemCard(
    habit: Habit,
    isCompleted: Boolean,
    isDark: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    isScheduledToday: Boolean
) {
    val iconTint: Triple<Color, Color, ImageVector> = remember(habit.title) {
        when {
            habit.title.contains("Medita", ignoreCase = true) -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), Icons.Rounded.SelfImprovement)
            habit.title.contains("Agua", ignoreCase = true) || habit.title.contains("Drink", ignoreCase = true) -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), Icons.Rounded.WaterDrop)
            habit.title.contains("Leer", ignoreCase = true) || habit.title.contains("Read", ignoreCase = true) -> Triple(Color(0xFFEDE9FE), Color(0xFF7C3AED), Icons.AutoMirrored.Rounded.MenuBook)
            habit.title.contains("Ejer", ignoreCase = true) || habit.title.contains("Gym", ignoreCase = true) -> Triple(Color(0xFFFEE2E2), Color(0xFFEF4444), Icons.Rounded.FitnessCenter)
            else -> Triple(Color(0xFFDCFCE7), Color(0xFF10B981), Icons.Rounded.Spa)
        }
    }

    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF15171E) else Color.White,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left circular icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isDark) iconTint.second.copy(alpha = 0.2f) else iconTint.first),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconTint.third,
                    contentDescription = null,
                    tint = if (isDark) Color.White else iconTint.second,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Habit Details & Streak Pill
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val supportingText = when {
                    !isScheduledToday -> "No programado hoy"
                    !habit.reminderTime.isNullOrBlank() -> "Recordatorio · ${habit.reminderTime}"
                    else -> "Sin recordatorio"
                }
                if (supportingText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Streak Tag Pill (Brish Theme)
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (habit.currentStreak > 0) {
                        if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE8F5E9)
                    } else {
                        if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF3F4F6)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (habit.currentStreak > 0) {
                                if (isDark) Color.White else Color(0xFF059669)
                            } else {
                                if (isDark) Color.White.copy(alpha = 0.4f) else Color.Gray
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${habit.currentStreak} días de racha",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (habit.currentStreak > 0) {
                                if (isDark) Color.White else Color(0xFF059669)
                            } else {
                                if (isDark) Color.White.copy(alpha = 0.4f) else Color.Gray
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Checkbox on right (Solid rounded square when checked)
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isCompleted) {
                                if (isDark) Color.White else Color(0xFF111827)
                            } else Color.Transparent
                        )
                        .border(
                            1.5.dp,
                            if (isCompleted) {
                                if (isDark) Color.White else Color(0xFF111827)
                            } else if (isDark) Color.White.copy(alpha = 0.3f) else Color(0xFFD1D5DB),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Completado",
                            tint = if (isDark) Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitReminderField(
    value: String,
    onValueChange: (String) -> Unit,
    onPickTime: () -> Unit
) {
    Surface(
        onClick = onPickTime,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.NotificationsNone, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Recordatorio", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value.ifBlank { "Sin hora" }, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Quitar recordatorio", modifier = Modifier.size(17.dp))
                }
            } else {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HabitDayPicker(selectedDays: List<Int>, onToggleDay: (Int) -> Unit) {
    val days = listOf(
        Calendar.MONDAY to "L",
        Calendar.TUESDAY to "M",
        Calendar.WEDNESDAY to "M",
        Calendar.THURSDAY to "J",
        Calendar.FRIDAY to "V",
        Calendar.SATURDAY to "S",
        Calendar.SUNDAY to "D"
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { (day, label) ->
            val selected = day in selectedDays
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onToggleDay(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun showHabitTimePicker(context: android.content.Context, currentValue: String, onTimeSelected: (String) -> Unit) {
    val parts = currentValue.split(":")
    val now = Calendar.getInstance()
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: now.get(Calendar.HOUR_OF_DAY)
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: now.get(Calendar.MINUTE)
    android.app.TimePickerDialog(
        context,
        { _, hour, minute -> onTimeSelected("%02d:%02d".format(hour, minute)) },
        initialHour,
        initialMinute,
        true
    ).show()
}
