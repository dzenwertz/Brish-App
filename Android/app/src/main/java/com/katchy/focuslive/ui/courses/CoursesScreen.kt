package com.katchy.focuslive.ui.courses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.Course
import com.katchy.focuslive.data.model.LearningSpace
import com.katchy.focuslive.ui.components.BrishTopHeader
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.bounceClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoursesScreen(
    onOpenCourse: (String) -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: CoursesViewModel = hiltViewModel()
) {
    val courses by viewModel.courses.collectAsState()
    val activeSpaces by viewModel.activeSpaces.collectAsState()
    val archivedSpaces by viewModel.archivedSpaces.collectAsState()
    val selectedSpace by viewModel.selectedSpace.collectAsState()
    val isDark = isDarkTheme()

    var showCourseDialog by remember { mutableStateOf(false) }
    var courseToDelete by remember { mutableStateOf<Course?>(null) }
    var showSpaceSwitcherDialog by remember { mutableStateOf(false) }
    var showCreateSpaceDialog by remember { mutableStateOf(false) }
    var showRenameSpaceDialog by remember { mutableStateOf(false) }
    var showArchivedSpacesDialog by remember { mutableStateOf(false) }
    var showArchiveConfirmDialog by remember { mutableStateOf(false) }

    val spaceName = selectedSpace?.name ?: "Espacio de aprendizaje"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BrishTopHeader(
                title = "Cursos",
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToChat = onNavigateToChat
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCourseDialog = true },
                containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                contentColor = if (isDark) Color(0xFF111827) else Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 80.dp).size(54.dp).bounceClick()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Nuevo curso", modifier = Modifier.size(24.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                SpaceSummaryCard(
                    courses = courses,
                    spaceName = spaceName,
                    isDark = isDark,
                    onClick = { showSpaceSwitcherDialog = true },
                    onLongClick = { showRenameSpaceDialog = true }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tus materias",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${courses.size} registrados",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            if (courses.isEmpty()) {
                item {
                    EmptyCoursesCard(
                        isDark = isDark,
                        spaceName = spaceName,
                        onClick = { showCourseDialog = true }
                    )
                }
            } else {
                items(courses, key = { it.course.id }) { summary ->
                    CourseCard(
                        summary = summary,
                        isDark = isDark,
                        onClick = { onOpenCourse(summary.course.id) },
                        onLongClick = { courseToDelete = summary.course }
                    )
                }
            }
        }
    }

    if (showCourseDialog) {
        AddCourseDialog(
            spaceName = spaceName,
            onDismiss = { showCourseDialog = false },
            onSave = { name -> if (viewModel.addCourse(name)) showCourseDialog = false }
        )
    }

    if (showSpaceSwitcherDialog) {
        SpaceSwitcherDialog(
            activeSpaces = activeSpaces,
            selectedSpaceId = selectedSpace?.id,
            archivedCount = archivedSpaces.size,
            onDismiss = { showSpaceSwitcherDialog = false },
            onSelectSpace = { id -> viewModel.selectSpace(id) },
            onCreateSpace = { showCreateSpaceDialog = true },
            onRenameSpace = { showRenameSpaceDialog = true },
            onArchiveSpace = { showArchiveConfirmDialog = true },
            onViewArchived = { showArchivedSpacesDialog = true }
        )
    }

    if (showCreateSpaceDialog) {
        CreateSpaceDialog(
            onDismiss = { showCreateSpaceDialog = false },
            onSave = { name -> if (viewModel.createSpace(name)) showCreateSpaceDialog = false }
        )
    }

    if (showRenameSpaceDialog) {
        RenameSpaceDialog(
            currentName = spaceName,
            onDismiss = { showRenameSpaceDialog = false },
            onSave = { name -> if (viewModel.renameCurrentSpace(name)) showRenameSpaceDialog = false }
        )
    }

    if (showArchivedSpacesDialog) {
        ArchivedSpacesDialog(
            archivedSpaces = archivedSpaces,
            onDismiss = { showArchivedSpacesDialog = false },
            onRestore = { id -> viewModel.restoreSpace(id) },
            onDelete = { id -> viewModel.deleteSpace(id) }
        )
    }

    if (showArchiveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirmDialog = false },
            title = { Text("Archivar espacio") },
            text = { Text("¿Deseas archivar \"$spaceName\"? Sus cursos, entregas y apuntes se conservarán y podrás restaurarlo cuando desees.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.archiveCurrentSpace()
                    showArchiveConfirmDialog = false
                }) { Text("Archivar") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }

    courseToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text("Quitar curso") },
            text = { Text("¿Quieres quitar ${course.name} de este espacio? Sus tareas y apuntes se conservarán.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeCourse(course.id)
                    courseToDelete = null
                }) { Text("Quitar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { courseToDelete = null }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpaceSummaryCard(
    courses: List<CourseSummary>,
    spaceName: String,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val pending = courses.sumOf { it.pendingDeliveries.size }
    val completed = courses.sumOf { it.completedCount }
    val nextWithDate = courses.flatMap { it.pendingDeliveries }.filter { it.hasDueDate }.minByOrNull { it.dueDate }
    val undatedCount = courses.flatMap { it.pendingDeliveries }.count { !it.hasDueDate }
    val total = pending + completed
    val progress = if (total == 0) 0f else completed.toFloat() / total

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF15171E) else Color.White,
        shadowElevation = if (isDark) 0.dp else 2.dp,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(if (isDark) Color(0xFF1E212B) else Color(0xFFE8EEF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(21.dp), tint = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = spaceName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Cambiar espacio", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Espacio de aprendizaje", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Surface(shape = RoundedCornerShape(100.dp), color = if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)) {
                    Text("${courses.size} ${if (courses.size == 1) "curso" else "cursos"}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                    trackColor = if (isDark) Color(0xFF1E212B) else Color(0xFFEFF1F4),
                    modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text("${(progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (pending == 0) "Al día · $completed completadas" else "$pending pendientes · $completed completadas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (nextWithDate != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "Próxima: ${nextWithDate.title} · ${shortDate(nextWithDate.dueDate)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (undatedCount > 0) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "$undatedCount ${if (undatedCount == 1) "entrega sin fecha límite" else "entregas sin fecha límite"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String, modifier: Modifier, isDark: Boolean) {
    Surface(shape = RoundedCornerShape(12.dp), color = if (isDark) Color(0xFF1E212B) else Color(0xFFF7F8FA), modifier = modifier) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(7.dp))
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CourseCard(
    summary: CourseSummary,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(summary.course.color)
    val next = summary.pendingDeliveries.firstOrNull()
    val isUpToDate = summary.pendingDeliveries.isEmpty()
    val total = summary.deliveries.size
    val completed = summary.completedCount

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF15171E) else Color.White,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color badge with course initial
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accent.copy(alpha = if (isDark) .22f else .12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = courseInitials(summary.course.name),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.width(13.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = summary.course.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isUpToDate) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (isDark) Color(0xFF0F2E22) else Color(0xFFDCFCE7)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Al día",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = accent.copy(alpha = if (isDark) .25f else .14f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.CalendarToday,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${summary.pendingDeliveries.size} pendiente${if (summary.pendingDeliveries.size == 1) "" else "s"}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else accent
                                    )
                                }
                            }

                            if (next != null) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (!next.hasDueDate) "Sin fecha" else "Próx: ${shortDate(next.dueDate)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Abrir curso",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Progress bar if there are deliverables
            if (total > 0) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LinearProgressIndicator(
                        progress = { summary.progress },
                        color = accent,
                        trackColor = accent.copy(alpha = .18f),
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$completed/$total",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCoursesCard(isDark: Boolean, spaceName: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF15171E) else Color.White,
        shadowElevation = if (isDark) 0.dp else 2.dp,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1E212B) else Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.School, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Sin cursos en este espacio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Añade los cursos de \"$spaceName\" para registrar entregas y apuntes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                    contentColor = if (isDark) Color(0xFF111827) else Color.White
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Añadir curso", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AddCourseDialog(spaceName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    BrishCourseDialog(
        title = "Nuevo curso",
        subtitle = "Se añadirá al espacio \"$spaceName\".",
        accent = Color(0xFF06B6D4),
        icon = { Icon(Icons.Rounded.School, contentDescription = null) },
        confirmLabel = "Crear curso",
        confirmEnabled = name.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onSave(name.trim()) }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Nombre del curso") },
            placeholder = { Text("Ej. Redes de Computadoras") },
            shape = RoundedCornerShape(14.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(17.dp), tint = Color(0xFF06B6D4))
                Spacer(Modifier.width(8.dp))
                Text("Espacio: $spaceName", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SpaceSwitcherDialog(
    activeSpaces: List<LearningSpace>,
    selectedSpaceId: String?,
    archivedCount: Int,
    onDismiss: () -> Unit,
    onSelectSpace: (String) -> Unit,
    onCreateSpace: () -> Unit,
    onRenameSpace: () -> Unit,
    onArchiveSpace: () -> Unit,
    onViewArchived: () -> Unit
) {
    val isDark = isDarkTheme()
    SmoothDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) Color(0xFF15171E) else Color.White,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFEFF1F5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF06B6D4).copy(alpha = if (isDark) .22f else .12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.School, contentDescription = null, tint = if (isDark) Color.White else Color(0xFF06B6D4), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Espacios de aprendizaje", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Separa universidad, metas personales o cursos libres.", fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Espacios activos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeSpaces, key = { it.id }) { space ->
                        val isSelected = space.id == selectedSpaceId
                        Surface(
                            onClick = {
                                onSelectSpace(space.id)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                if (isDark) Color(0xFF1E2638) else Color(0xFFE0F2FE)
                            } else {
                                if (isDark) Color(0xFF1B1E28) else Color(0xFFF3F4F6)
                            },
                            border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF0284C7)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = space.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Seleccionado", tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onCreateSpace()
                        },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                            contentColor = if (isDark) Color(0xFF111827) else Color.White
                        ),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nuevo espacio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            onRenameSpace()
                        },
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Renombrar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (activeSpaces.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            onDismiss()
                            onArchiveSpace()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Archivar este espacio", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }

                if (archivedCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            onDismiss()
                            onViewArchived()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver archivados ($archivedCount)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Cerrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CreateSpaceDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    BrishCourseDialog(
        title = "Nuevo espacio",
        subtitle = "Ejemplos: Ciclo 2026-2, Ciberseguridad, Inglés, Certificaciones.",
        accent = Color(0xFF06B6D4),
        icon = { Icon(Icons.Rounded.School, contentDescription = null) },
        confirmLabel = "Crear espacio",
        confirmEnabled = name.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onSave(name.trim()) }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Nombre del espacio") },
            placeholder = { Text("Ej. Ciberseguridad") },
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun RenameSpaceDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    BrishCourseDialog(
        title = "Renombrar espacio",
        subtitle = "El nuevo nombre se aplicará a este espacio de estudio.",
        accent = Color(0xFF111827),
        icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
        confirmLabel = "Guardar",
        confirmEnabled = name.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onSave(name.trim()) }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Nombre del espacio") },
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun ArchivedSpacesDialog(
    archivedSpaces: List<LearningSpace>,
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val isDark = isDarkTheme()
    var spaceToDelete by remember { mutableStateOf<LearningSpace?>(null) }
    spaceToDelete?.let { space ->
        AlertDialog(
            onDismissRequest = { spaceToDelete = null },
            title = { Text("¿Eliminar espacio?") },
            text = { Text("Vas a eliminar «${space.name}». Si solo quieres recuperarlo, elige Cancelar y después Restaurar.") },
            confirmButton = {
                TextButton(onClick = { spaceToDelete = null; onDelete(space.id) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { spaceToDelete = null }) { Text("Cancelar") } }
        )
    }
    SmoothDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) Color(0xFF15171E) else Color.White,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFEFF1F5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("Espacios archivados", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text("Los cursos y apuntes de estos espacios se conservan intactos.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))

                if (archivedSpaces.isEmpty()) {
                    Text("No hay espacios archivados.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(archivedSpaces, key = { it.id }) { space ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF1B1E28) else Color(0xFFF3F4F6),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(space.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { onRestore(space.id) }) {
                                            Text("Restaurar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(onClick = { spaceToDelete = space }, modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddDeliverableDialog(
    courseName: String,
    onDismiss: () -> Unit,
    onSave: (String, Long, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var hasDueDate by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }

    BrishCourseDialog(
        title = "Nueva entrega",
        subtitle = courseName,
        accent = Color(0xFF2563EB),
        icon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
        confirmLabel = "Añadir entrega",
        confirmEnabled = title.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onSave(title.trim(), dueDate, hasDueDate) }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Nombre de la entrega") },
            placeholder = { Text("Tarea, cuestionario o proyecto") },
            shape = RoundedCornerShape(14.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Sin fecha límite", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Para tareas o entregas abiertas sin fecha fija", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = !hasDueDate,
                onCheckedChange = { hasDueDate = !it }
            )
        }

        if (hasDueDate) {
            Surface(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFDBEAFE)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Fecha límite", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(fullDate(dueDate), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Esta entrega no vencerá automáticamente", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showDatePicker && hasDueDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = localDateToPickerMillis(dueDate))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dueDate = pickerDateToLocalMillis(it) }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
internal fun BrishCourseDialog(
    title: String,
    subtitle: String,
    accent: Color,
    icon: @Composable () -> Unit,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isDarkTheme()
    SmoothDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) Color(0xFF15171E) else Color.White,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFEFF1F5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()).padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = if (isDark) .22f else .12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides if (isDark) Color.White else accent,
                            content = icon
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(subtitle, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(11.dp), content = content)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun courseInitials(name: String): String = name.trim()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.take(1) }
    .uppercase(Locale.getDefault())
    .ifBlank { "C" }

private fun shortDate(timestamp: Long): String = SimpleDateFormat("d MMM", Locale("es", "PE")).format(Date(timestamp)).replaceFirstChar { it.uppercase() }

private fun fullDate(timestamp: Long): String = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "PE")).format(Date(timestamp)).replaceFirstChar { it.uppercase() }
