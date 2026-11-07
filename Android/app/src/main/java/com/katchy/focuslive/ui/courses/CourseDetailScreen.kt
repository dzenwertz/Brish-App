package com.katchy.focuslive.ui.courses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
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
import com.katchy.focuslive.ui.components.BrishArtKind
import com.katchy.focuslive.ui.components.BrishCardSurface
import com.katchy.focuslive.ui.components.BrishCelebrationBurst
import com.katchy.focuslive.ui.components.BrishEmptyState
import com.katchy.focuslive.ui.components.BrishIconTile
import com.katchy.focuslive.ui.components.BrishPrimaryButton
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.BrishSecondaryButton
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.components.brishSharedBounds
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.BrishDanger
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishWarning
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.brishCascade
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
    val accent = state.course?.let { Color(it.color) } ?: BrishWarning

    BackHandler(onBack = onBack)
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(Modifier.brishCascade(0)) {
                CourseDetailHeader(
                    title = state.course?.name ?: "Curso",
                    semester = state.course?.semester.orEmpty(),
                    accent = accent,
                    onBack = onBack
                )
            }
        }

        item {
            Box(
                Modifier
                    .brishCascade(1)
                    .brishSharedBounds("course-${viewModel.courseId}", BrishShapes.card)
            ) {
                ProgressCard(state = state, accent = accent)
            }
        }

        item {
            Box(Modifier.brishCascade(2)) {
                SectionHeader(
                    title = "Pendientes",
                    count = state.deliveries.count { !it.isCompleted },
                    actionLabel = "Nueva",
                    onAction = { showNewDelivery = true }
                )
            }
        }

        if (pending.isEmpty()) {
            item {
                BrishEmptyState(
                    art = BrishArtKind.Tasks,
                    title = "Nada pendiente por ahora",
                    subtitle = if (completed.isEmpty()) "Añade tu primera tarea desde Nueva." else "Tus entregas están al día. Buen trabajo.",
                    actionLabel = "Añadir entrega",
                    onAction = { showNewDelivery = true }
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
                BrishEmptyState(
                    art = BrishArtKind.Note,
                    title = "Tu espacio de estudio",
                    subtitle = "Guarda resúmenes, fórmulas o ideas clave.",
                    actionLabel = "Crear apunte",
                    onAction = { onOpenNote(null, viewModel.courseId) }
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
            onSave = { draft, saved ->
                viewModel.saveDeliverable(draft, deliveryToEdit) {
                    saved()
                    showNewDelivery = false
                    deliveryToEdit = null
                }
            },
            onDelete = deliveryToEdit?.let { task -> { deliveryToDelete = task } }
        )
    }

    deliveryToDelete?.let { task ->
        SmoothDialog(onDismissRequest = { deliveryToDelete = null }) {
            BrishCardSurface(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                shape = BrishShapes.hero
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BrishIconTile(Icons.Rounded.DeleteOutline, BrishDanger.copy(alpha = 0.14f), BrishDanger, size = 40.dp)
                        Text("Eliminar entrega", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("¿Deseas eliminar \"${task.title}\" de este curso?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        BrishSecondaryButton("Cancelar", { deliveryToDelete = null }, Modifier.weight(1f), height = 46.dp)
                        BrishPrimaryButton(
                            text = "Eliminar",
                            onClick = {
                                viewModel.deleteDelivery(task.id) {
                                    deliveryToDelete = null
                                    deliveryToEdit = null
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving,
                            height = 46.dp
                        )
                    }
                }
            }
        }
    }

    CourseWriteError(error, viewModel::clearError)
}

@Composable
private fun CourseDetailHeader(title: String, semester: String, accent: Color, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BrishRoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", onBack, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
            Text(semester.ifBlank { "Ciclo actual" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Surface(shape = CircleShape, color = accent, modifier = Modifier.size(10.dp)) {}
    }
}

@Composable
private fun ProgressCard(state: CourseDetailState, accent: Color) {
    val completed = state.deliveries.count { it.isCompleted }
    val total = state.deliveries.size
    val progress = if (total == 0) 0f else completed.toFloat() / total
    val next = state.deliveries.firstOrNull { !it.isCompleted }

    Column(Modifier.fillMaxWidth().brishCard().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Progreso", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (total == 0) "Empieza con una entrega" else "$completed de $total completadas", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
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
            Text("Próxima · ${next.title}", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(if (!next.hasDueDate) "Sin fecha límite" else detailRelativeDate(next.dueDate), color = accent, fontSize = 11.sp)
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
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
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
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeliveryRow(
    task: Task,
    accent: Color,
    onToggle: () -> Unit,
    onFocus: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .brishCard(RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onLongClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var celebrationTrigger by remember(task.id) { mutableStateOf(0) }
        BrishCelebrationBurst(trigger = celebrationTrigger, particleColor = accent) {
            IconButton(
                onClick = {
                    if (!task.isCompleted) celebrationTrigger++
                    onToggle()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (task.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Circle,
                    contentDescription = if (task.isCompleted) "Marcar pendiente" else "Completar",
                    tint = if (task.isCompleted) AreaHealth else accent.copy(alpha = .62f),
                    modifier = Modifier.size(21.dp)
                )
            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .brishSharedBounds("note-${note.id}", RoundedCornerShape(18.dp))
            .brishCard(RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.width(3.dp).height(38.dp).clip(CircleShape)) { Surface(color = accent, modifier = Modifier.fillMaxSize()) {} }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(note.title.ifBlank { "Apunte" }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(note.content, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
