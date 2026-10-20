package com.katchy.focuslive.ui.courses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.ui.theme.isDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CourseDetailScreen(
    onBack: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onOpenNote: (noteId: String?, courseId: String) -> Unit,
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val error by viewModel.error.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    var deliveryToEdit by remember { mutableStateOf<Task?>(null) }
    var showNewDelivery by remember { mutableStateOf(false) }
    var deliveryToDelete by remember { mutableStateOf<Task?>(null) }
    var showCompleted by remember { mutableStateOf(false) }
    val pending = state.deliveries.filterNot { it.isCompleted }
    val completed = state.deliveries.filter { it.isCompleted }
    val accent = state.course?.let { Color(it.color) } ?: com.katchy.focuslive.ui.theme.BrishWarning

    BackHandler(onBack = onBack)
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CourseDetailHeader(
                title = state.course?.name ?: "Curso",
                semester = state.course?.semester.orEmpty(),
                accent = accent,
                onBack = onBack
            )
        }

        item {
            ProgressCard(state = state, accent = accent)
        }

        item {
            SectionHeader(
                title = "Pendientes",
                count = state.deliveries.count { !it.isCompleted },
                actionLabel = "Nueva",
                onAction = { showNewDelivery = true }
            )
        }

        if (pending.isEmpty()) {
            item {
                CompactEmptyState(
                    icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = accent) },
                    title = "Nada pendiente por ahora",
                    subtitle = if (completed.isEmpty()) "Añade tu primera tarea desde Nueva." else "Tus entregas están al día. Buen trabajo.",
                    buttonLabel = "Añadir entrega",
                    accent = accent,
                    onClick = { showNewDelivery = true }
                )
            }
        } else {
            items(pending, key = { "delivery-${it.id}" }) { task ->
                DeliveryRow(
                    task = task,
                    accent = accent,
                    onToggle = { viewModel.toggleDelivery(task) },
                    onFocus = { viewModel.beginFocus(task); onNavigateToFocus() },
                    onLongClick = { deliveryToEdit = task }
                )
            }
        }

        if (completed.isNotEmpty()) {
            item {
                TextButton(onClick = { showCompleted = !showCompleted }, modifier = Modifier.fillMaxWidth()) {
                    Text("${if (showCompleted) "Ocultar" else "Ver"} completadas · ${completed.size}")
                }
            }
            if (showCompleted) {
                items(completed, key = { "delivery-${it.id}" }) { task ->
                    DeliveryRow(task, accent, { viewModel.toggleDelivery(task) }, {}, { deliveryToEdit = task })
                }
            }
        }

        item {
            SectionHeader(
                title = "Apuntes",
                count = state.notes.size,
                actionLabel = "Nuevo",
                onAction = { onOpenNote(null, viewModel.courseId) }
            )
        }

        if (state.notes.isEmpty()) {
            item {
                CompactEmptyState(
                    icon = { Icon(Icons.Rounded.EditNote, contentDescription = null, tint = accent) },
                    title = "Tu espacio de estudio",
                    subtitle = "Guarda resúmenes, fórmulas o ideas clave.",
                    buttonLabel = "Crear apunte",
                    accent = accent,
                    onClick = { onOpenNote(null, viewModel.courseId) }
                )
            }
        } else {
            items(state.notes, key = { "note-${it.id}" }) { note ->
                NoteCard(
                    note = note,
                    accent = accent,
                    onClick = { onOpenNote(note.id, viewModel.courseId) }
                )
            }
        }
    }

    if (showNewDelivery || deliveryToEdit != null) {
        DeliverableDialog(
            courseName = state.course?.name ?: "Curso",
            existing = deliveryToEdit,
            isSaving = isSaving,
            onDismiss = { showNewDelivery = false; deliveryToEdit = null },
            onSave = { draft ->
                viewModel.saveDeliverable(draft, deliveryToEdit) {
                    showNewDelivery = false
                    deliveryToEdit = null
                }
            },
            onDelete = deliveryToEdit?.let { task -> { deliveryToDelete = task } }
        )
    }

    deliveryToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { deliveryToDelete = null },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text("Eliminar entrega") },
            text = { Text("¿Deseas eliminar \"${task.title}\" de este curso?") },
            confirmButton = {
                TextButton(enabled = !isSaving, onClick = {
                    viewModel.deleteDelivery(task.id) {
                        deliveryToDelete = null
                        deliveryToEdit = null
                    }
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deliveryToDelete = null }) { Text("Cancelar") } }
        )
    }

    CourseWriteError(error, viewModel::clearError)

}

@Composable
private fun CourseDetailHeader(title: String, semester: String, accent: Color, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(semester.ifBlank { "Ciclo actual" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Surface(shape = CircleShape, color = accent, modifier = Modifier.size(10.dp)) {}
    }
}

@Composable
private fun ProgressCard(state: CourseDetailState, accent: Color) {
    val isDark = isDarkTheme()
    val completed = state.deliveries.count { it.isCompleted }
    val total = state.deliveries.size
    val progress = if (total == 0) 0f else completed.toFloat() / total
    val next = state.deliveries.firstOrNull { !it.isCompleted }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Progreso", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (total == 0) "Empieza con una entrega" else "$completed de $total completadas", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Text("${(progress * 100).toInt()}%", color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = accent,
                trackColor = accent.copy(alpha = .16f),
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CourseMetric(
                    value = state.deliveries.count { !it.isCompleted }.toString(),
                    label = "pendientes",
                    modifier = Modifier.weight(1f)
                )
                CourseMetric(
                    value = state.notes.size.toString(),
                    label = "apuntes",
                    modifier = Modifier.weight(1f)
                )
            }
            if (next != null) {
                Spacer(Modifier.height(12.dp))
                Text("Próxima · ${next.title}", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(if (!next.hasDueDate) "Sin fecha límite" else detailRelativeDate(next.dueDate), color = accent, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CourseMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f),
        modifier = modifier
    ) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(7.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(count.toString(), modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
        TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(3.dp))
            Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CompactEmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    buttonLabel: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkTheme()) Color(0xFF15171E) else Color.White,
        border = BorderStroke(1.dp, if (isDarkTheme()) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = accent.copy(alpha = .11f), modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
            Spacer(Modifier.height(9.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            TextButton(onClick = onClick) { Text(buttonLabel, color = accent) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeliveryRow(
    task: Task,
    accent: Color,
    onToggle: () -> Unit,
    onFocus: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onLongClick,
                onLongClick = onLongClick
            )
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (task.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Circle,
                    contentDescription = if (task.isCompleted) "Marcar pendiente" else "Completar",
                    tint = if (task.isCompleted) Color(0xFF10B981) else accent.copy(alpha = .62f),
                    modifier = Modifier.size(21.dp)
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    task.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (!task.hasDueDate) "Sin fecha límite" else detailRelativeDate(task.dueDate) + task.time?.let { " · $it" }.orEmpty(),
                    fontSize = 11.sp,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else accent
                )
            }
            if (!task.isCompleted) {
                FilledTonalButton(onClick = onFocus, shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Enfocarme", modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.width(3.dp).height(38.dp).clip(CircleShape)) { Surface(color = accent, modifier = Modifier.fillMaxSize()) {} }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(note.title.ifBlank { "Apunte" }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                if (note.content.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(note.content, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun detailRelativeDate(timestamp: Long): String {
    val days = courseDaysFromToday(timestamp)
    return when {
        days < 0 -> "Venció hace ${-days} d"
        days == 0L -> "Vence hoy"
        days == 1L -> "Vence mañana"
        days <= 7 -> "Vence en $days días"
        else -> SimpleDateFormat("EEE d MMM", Locale("es", "PE")).format(Date(timestamp)).replaceFirstChar { it.uppercase() }
    }
}
