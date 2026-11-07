package com.katchy.focuslive.ui.courses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.Course
import com.katchy.focuslive.data.model.LearningSpace
import com.katchy.focuslive.ui.components.BrishCardSurface
import com.katchy.focuslive.ui.components.BrishEmptyState
import com.katchy.focuslive.ui.components.BrishIconTile
import com.katchy.focuslive.ui.components.BrishPrimaryButton
import com.katchy.focuslive.ui.components.BrishPullToRefreshBox
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.BrishScreenHeader
import com.katchy.focuslive.ui.components.BrishSecondaryButton
import com.katchy.focuslive.ui.components.BrishTextField
import com.katchy.focuslive.ui.components.DraftRecoveryNotice
import com.katchy.focuslive.ui.components.LocalTabReselectSignal
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.components.brishSharedBounds
import com.katchy.focuslive.ui.components.rememberPersistentDraftText
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.AreaHealthBg
import com.katchy.focuslive.ui.theme.AreaWork
import com.katchy.focuslive.ui.theme.AreaWorkBg
import com.katchy.focuslive.ui.theme.BrishHeroBrush
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishOnYellow
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishWarning
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.BrishYellowDeep
import com.katchy.focuslive.ui.theme.BrishYellowSoft
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.brishCascade
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.isDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoursesScreen(
    onOpenCourse: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: CoursesViewModel = hiltViewModel()
) {
    val courses by viewModel.courses.collectAsState()
    val activeSpaces by viewModel.activeSpaces.collectAsState()
    val archivedSpaces by viewModel.archivedSpaces.collectAsState()
    val selectedSpace by viewModel.selectedSpace.collectAsState()
    val isDark = isDarkTheme()
    val error by viewModel.error.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showCourseDialog by remember { mutableStateOf(false) }
    var courseToDelete by remember { mutableStateOf<Course?>(null) }
    var showSpaceSwitcherDialog by remember { mutableStateOf(false) }
    var showCreateSpaceDialog by remember { mutableStateOf(false) }
    var showRenameSpaceDialog by remember { mutableStateOf(false) }
    var showArchivedSpacesDialog by remember { mutableStateOf(false) }
    var showArchiveConfirmDialog by remember { mutableStateOf(false) }

    val spaceName = selectedSpace?.name ?: "Espacio de aprendizaje"
    val listState = rememberLazyListState()
    val reselectSignal = LocalTabReselectSignal.current
    val initialReselect = remember { reselectSignal }
    LaunchedEffect(reselectSignal) {
        if (reselectSignal != initialReselect) {
            listState.animateScrollToItem(0)
        }
    }

    BrishPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 24.dp + com.katchy.focuslive.ui.main.LocalBottomBarInset.current),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "header") {
                BrishScreenHeader("Cursos") {
                    Box(
                        Modifier.height(40.dp).clip(CircleShape).background(BrishYellow)
                            .clickable(role = Role.Button) { showCourseDialog = true }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("＋ Nuevo", fontWeight = FontWeight.SemiBold, color = BrishInk)
                    }
                }
            }

            item(key = "spaceSummary") {
                Box(Modifier.brishCascade(0)) {
                    SpaceSummaryCard(
                        courses = courses,
                        spaceName = spaceName,
                        onClick = { showSpaceSwitcherDialog = true },
                        onLongClick = { showRenameSpaceDialog = true }
                    )
                }
            }

            item(key = "sectionHeader") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).brishCascade(1),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TUS MATERIAS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${courses.size} ${if (courses.size == 1) "registrada" else "registradas"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (courses.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.brishCascade(2)) {
                        BrishEmptyState(
                            title = "Sin cursos en este espacio",
                            subtitle = "Añade las materias de «$spaceName» para organizar entregas y apuntes.",
                            illustration = {
                                BrishIconTile(Icons.Rounded.School, BrishYellowSoft, BrishInk, size = 56.dp)
                            },
                            actionLabel = "Añadir curso",
                            onAction = { showCourseDialog = true }
                        )
                    }
                }
            } else {
                itemsIndexed(courses, key = { _, it -> it.course.id }) { idx, summary ->
                    CourseCard(
                        summary = summary,
                        isDark = isDark,
                        onClick = { onOpenCourse(summary.course.id) },
                        onLongClick = { courseToDelete = summary.course },
                        modifier = Modifier
                            .brishCascade(idx + 2, summary.course.id)
                            .brishSharedBounds("course-${summary.course.id}", BrishShapes.card)
                    )
                }
            }
        }
    }

    if (showCourseDialog) {
        AddCourseDialog(
            isSaving = isSaving,
            spaceName = spaceName,
            onDismiss = { showCourseDialog = false },
            onSave = { name, saved -> viewModel.addCourse(name) { saved(); showCourseDialog = false } }
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
            isSaving = isSaving,
            onDismiss = { showCreateSpaceDialog = false },
            onSave = { name, saved -> viewModel.createSpace(name) { saved(); showCreateSpaceDialog = false } }
        )
    }

    if (showRenameSpaceDialog) {
        RenameSpaceDialog(
            isSaving = isSaving,
            currentName = spaceName,
            onDismiss = { showRenameSpaceDialog = false },
            onSave = { name, saved -> viewModel.renameCurrentSpace(name) { saved(); showRenameSpaceDialog = false } }
        )
    }

    if (showArchivedSpacesDialog) {
        ArchivedSpacesDialog(
            archivedSpaces = archivedSpaces,
            onDismiss = { showArchivedSpacesDialog = false },
            onRestore = { id -> viewModel.restoreSpace(id) },
            onDelete = { id, onSuccess -> viewModel.deleteSpace(id, onSuccess) }
        )
    }

    if (showArchiveConfirmDialog) {
        SmoothDialog(onDismissRequest = { showArchiveConfirmDialog = false }) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("Archivar espacio", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "¿Deseas archivar «$spaceName»? Sus cursos, entregas y apuntes se conservarán y podrás restaurarlo cuando desees.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton("Cancelar", onClick = { showArchiveConfirmDialog = false }, modifier = Modifier.weight(1f))
                    BrishPrimaryButton(
                        text = "Archivar",
                        enabled = !isSaving,
                        onClick = { viewModel.archiveCurrentSpace { showArchiveConfirmDialog = false } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    courseToDelete?.let { course ->
        SmoothDialog(onDismissRequest = { courseToDelete = null }) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("Quitar curso", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Solo puedes eliminar ${course.name} si no contiene entregas ni apuntes. Para conservar el contenido, archiva su espacio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton("Cancelar", onClick = { courseToDelete = null }, modifier = Modifier.weight(1f))
                    BrishPrimaryButton(
                        text = "Quitar",
                        enabled = !isSaving,
                        onClick = { viewModel.removeCourse(course.id) { courseToDelete = null } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    CourseWriteError(error, viewModel::clearError)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpaceSummaryCard(
    courses: List<CourseSummary>,
    spaceName: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val pending = courses.sumOf { it.pendingDeliveries.size }
    val completed = courses.sumOf { it.completedCount }
    val nextWithDate = courses.flatMap { it.pendingDeliveries }.filter { it.hasDueDate }.minByOrNull { it.dueDate }
    val undatedCount = courses.flatMap { it.pendingDeliveries }.count { !it.hasDueDate }
    val total = pending + completed
    val progress = if (total == 0) 0f else completed.toFloat() / total

    Column(
        Modifier
            .fillMaxWidth()
            .brishShadow(BrishShapes.hero, strength = 1.4f)
            .clip(BrishShapes.hero)
            .background(BrishHeroBrush)
            .brishEdgeHighlight()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(22.dp), tint = BrishInk)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = spaceName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = BrishInk,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Cambiar espacio", modifier = Modifier.size(18.dp), tint = BrishOnYellow)
                    }
                    Text("Espacio de aprendizaje", color = BrishOnYellow, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(
                Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("${courses.size} ${if (courses.size == 1) "curso" else "cursos"}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BrishInk)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(CircleShape).background(BrishInk))
            }
            Spacer(Modifier.width(10.dp))
            Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrishInk)
        }
        Text(
            text = if (pending == 0) "Al día · $completed completadas" else "$pending pendientes · $completed completadas",
            style = MaterialTheme.typography.bodySmall,
            color = BrishOnYellow,
            fontWeight = FontWeight.SemiBold
        )

        if (nextWithDate != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = BrishInk, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    "Próxima: ${nextWithDate.title} · ${shortDate(nextWithDate.dueDate)}",
                    fontSize = 12.sp,
                    color = BrishInk,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else if (undatedCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = BrishInk, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    "$undatedCount ${if (undatedCount == 1) "entrega sin fecha límite" else "entregas sin fecha límite"}",
                    fontSize = 12.sp,
                    color = BrishInk,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .brishCard()
            .clip(BrishShapes.card)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = if (isDark) .22f else .14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = courseInitials(summary.course.name),
                    color = if (isDark) Color.White else accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.width(13.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = summary.course.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUpToDate) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AreaHealthBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = AreaHealth,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Al día",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AreaHealth
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accent.copy(alpha = if (isDark) .25f else .14f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${summary.pendingDeliveries.size} pendiente${if (summary.pendingDeliveries.size == 1) "" else "s"}",
                                    fontSize = 11.sp,
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

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
                        .height(6.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "$completed/$total",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddCourseDialog(spaceName: String, isSaving: Boolean, onDismiss: () -> Unit, onSave: (String, () -> Unit) -> Unit) {
    val nameDraft = rememberPersistentDraftText("course:new:$spaceName:name")
    var name by nameDraft::value
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestDismiss = {
        if (name.isNotBlank() || nameDraft.recovered) confirmDiscard = true else onDismiss()
    }
    if (confirmDiscard) {
        SmoothDialog(onDismissRequest = { confirmDiscard = false }) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("¿Descartar curso?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("El nombre escrito se descartará.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton("Seguir editando", onClick = { confirmDiscard = false }, modifier = Modifier.weight(1f))
                    BrishPrimaryButton("Descartar", onClick = { nameDraft.discard(); confirmDiscard = false; onDismiss() }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
    BrishCourseDialog(
        title = "Nuevo curso",
        subtitle = "Se añadirá al espacio «$spaceName».",
        accent = BrishWarning,
        icon = { Icon(Icons.Rounded.School, contentDescription = null) },
        confirmLabel = "Crear curso",
        confirmEnabled = !isSaving && name.isNotBlank(),
        onDismiss = requestDismiss,
        onConfirm = { onSave(name.trim()) { nameDraft.clearAfterSave() } }
    ) {
        DraftRecoveryNotice(nameDraft, Modifier.fillMaxWidth())
        BrishTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Ej. Redes de Computadoras",
            label = "Nombre del curso"
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(17.dp), tint = BrishWarning)
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
    SmoothDialog(onDismissRequest = onDismiss) {
        BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrishIconTile(Icons.Rounded.School, BrishYellowSoft, BrishInk, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Espacios de aprendizaje", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Separa universidad, metas personales o cursos libres.", fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text("Espacios activos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) AreaWorkBg else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) BorderStroke(1.5.dp, AreaWork) else null,
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
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.SemiBold,
                                color = if (isSelected) AreaWork else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Seleccionado", tint = AreaWork, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrishPrimaryButton(
                    text = "Nuevo espacio",
                    leadingIcon = Icons.Rounded.Add,
                    onClick = {
                        onDismiss()
                        onCreateSpace()
                    },
                    modifier = Modifier.weight(1f)
                )
                BrishSecondaryButton(
                    text = "Renombrar",
                    leadingIcon = Icons.Rounded.Edit,
                    onClick = {
                        onDismiss()
                        onRenameSpace()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (activeSpaces.size > 1) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onArchiveSpace()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Archivar este espacio", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }

            if (archivedCount > 0) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onViewArchived()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver archivados ($archivedCount)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cerrar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CreateSpaceDialog(isSaving: Boolean, onDismiss: () -> Unit, onSave: (String, () -> Unit) -> Unit) {
    val nameDraft = rememberPersistentDraftText("course:space:new:name")
    var name by nameDraft::value
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestDismiss = {
        if (name.isNotBlank() || nameDraft.recovered) confirmDiscard = true else onDismiss()
    }
    if (confirmDiscard) {
        SmoothDialog(onDismissRequest = { confirmDiscard = false }) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("¿Descartar espacio?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("El nombre escrito se descartará.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton("Seguir editando", onClick = { confirmDiscard = false }, modifier = Modifier.weight(1f))
                    BrishPrimaryButton("Descartar", onClick = { nameDraft.discard(); confirmDiscard = false; onDismiss() }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
    BrishCourseDialog(
        title = "Nuevo espacio",
        subtitle = "Ejemplos: Ciclo 2026-2, Ciberseguridad, Inglés, Certificaciones.",
        accent = BrishWarning,
        icon = { Icon(Icons.Rounded.School, contentDescription = null) },
        confirmLabel = "Crear espacio",
        confirmEnabled = !isSaving && name.isNotBlank(),
        onDismiss = requestDismiss,
        onConfirm = { onSave(name.trim()) { nameDraft.clearAfterSave() } }
    ) {
        DraftRecoveryNotice(nameDraft, Modifier.fillMaxWidth())
        BrishTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Ej. Ciberseguridad",
            label = "Nombre del espacio"
        )
    }
}

@Composable
private fun RenameSpaceDialog(currentName: String, isSaving: Boolean, onDismiss: () -> Unit, onSave: (String, () -> Unit) -> Unit) {
    val nameDraft = rememberPersistentDraftText("course:space:rename:$currentName", currentName)
    var name by nameDraft::value
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestDismiss = {
        if ((name.isNotBlank() && name != currentName) || nameDraft.recovered) confirmDiscard = true else onDismiss()
    }
    if (confirmDiscard) {
        SmoothDialog(onDismissRequest = { confirmDiscard = false }) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("¿Descartar cambios?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("Se mantendrá el nombre anterior del espacio.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton("Seguir editando", onClick = { confirmDiscard = false }, modifier = Modifier.weight(1f))
                    BrishPrimaryButton("Descartar", onClick = { nameDraft.discard(); confirmDiscard = false; onDismiss() }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
    BrishCourseDialog(
        title = "Renombrar espacio",
        subtitle = "El nuevo nombre se aplicará a este espacio de estudio.",
        accent = BrishInk,
        icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
        confirmLabel = "Guardar",
        confirmEnabled = !isSaving && name.isNotBlank(),
        onDismiss = requestDismiss,
        onConfirm = { onSave(name.trim()) { nameDraft.clearAfterSave() } }
    ) {
        DraftRecoveryNotice(nameDraft, Modifier.fillMaxWidth())
        BrishTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Nombre del espacio",
            label = "Nombre del espacio"
        )
    }
}

@Composable
private fun ArchivedSpacesDialog(
    archivedSpaces: List<LearningSpace>,
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit,
    onDelete: (String, () -> Unit) -> Unit
) {
    var spaceToDelete by remember { mutableStateOf<LearningSpace?>(null) }
    spaceToDelete?.let { space ->
        SmoothDialog(onDismissRequest = { spaceToDelete = null }) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("¿Eliminar espacio?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("Solo puedes eliminar «${space.name}» si no contiene cursos. Puedes restaurarlo para revisar su contenido.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton("Cancelar", onClick = { spaceToDelete = null }, modifier = Modifier.weight(1f))
                    BrishPrimaryButton("Eliminar", onClick = { onDelete(space.id) { spaceToDelete = null } }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
    SmoothDialog(onDismissRequest = onDismiss) {
        BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Espacios archivados", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Los cursos y apuntes de estos espacios se conservan intactos.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(space.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { onRestore(space.id) }) {
                                        Text("Restaurar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(onClick = { spaceToDelete = space }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            BrishPrimaryButton("Cerrar", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeliverableDialog(
    courseName: String,
    existing: com.katchy.focuslive.data.model.Task? = null,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (DeliverableDraft, () -> Unit) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val draftPrefix = "course:${existing?.id ?: "new"}:deliverable"
    val titleDraft = rememberPersistentDraftText("$draftPrefix:title", existing?.title.orEmpty())
    val descriptionDraft = rememberPersistentDraftText("$draftPrefix:description", existing?.description.orEmpty())
    val dueDateDraft = rememberPersistentDraftText("$draftPrefix:date", (existing?.dueDate?.takeIf { existing.hasDueDate } ?: System.currentTimeMillis()).toString())
    val hasDueDateDraft = rememberPersistentDraftText("$draftPrefix:has-date", (existing?.hasDueDate ?: false).toString())
    val timeDraft = rememberPersistentDraftText("$draftPrefix:time", existing?.time.orEmpty())
    var title by titleDraft::value
    var description by descriptionDraft::value
    val dueDate = dueDateDraft.value.toLongOrNull() ?: System.currentTimeMillis()
    val hasDueDate = hasDueDateDraft.value.toBooleanStrictOrNull() ?: false
    var time by timeDraft::value
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val draft = DeliverableDraft(title, description, dueDate.takeIf { hasDueDate }, time.takeIf { hasDueDate && it.isNotBlank() })
    val validationError = draft.validationError()
    val requestDismiss = {
        if (listOf(title, description, time).any(String::isNotBlank) || titleDraft.recovered || descriptionDraft.recovered || timeDraft.recovered) confirmDiscard = true
        else onDismiss()
    }

    BrishCourseDialog(
        title = if (existing == null) "Nueva entrega" else "Editar entrega",
        subtitle = courseName,
        accent = AreaWork,
        icon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
        confirmLabel = if (isSaving) "Guardando…" else "Guardar entrega",
        confirmEnabled = !isSaving && validationError == null,
        onDismiss = { if (!isSaving) requestDismiss() },
        onConfirm = {
            onSave(draft) {
                titleDraft.clearStored(); descriptionDraft.clearStored(); dueDateDraft.clearStored()
                hasDueDateDraft.clearStored(); timeDraft.clearStored()
            }
        }
    ) {
        if (titleDraft.recovered || descriptionDraft.recovered || dueDateDraft.recovered || hasDueDateDraft.recovered || timeDraft.recovered) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Borrador recuperado", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { titleDraft.discard(); descriptionDraft.discard(); dueDateDraft.discard(); hasDueDateDraft.discard(); timeDraft.discard(); onDismiss() }) { Text("Descartar") }
            }
        } else {
            DraftRecoveryNotice(titleDraft, Modifier.fillMaxWidth())
        }
        BrishTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = "Ej. Examen parcial o informe",
            label = "Nombre de la entrega",
            enabled = !isSaving
        )
        BrishTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = "Detalles opcionales...",
            label = "Descripción · opcional",
            enabled = !isSaving,
            singleLine = false,
            minLines = 2,
            maxLines = 4
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Fecha límite · opcional", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Switch(
                checked = hasDueDate,
                enabled = !isSaving,
                onCheckedChange = { hasDueDateDraft.value = it.toString() },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = androidx.compose.ui.graphics.Color.White, checkedBorderColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
        if (hasDueDate) {
            BrishSecondaryButton(
                text = fullDate(dueDate),
                leadingIcon = Icons.Rounded.CalendarToday,
                enabled = !isSaving,
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            )
            BrishTextField(
                value = time,
                onValueChange = { time = it },
                placeholder = "Por ejemplo, 14:30",
                label = "Hora · opcional (HH:mm)",
                enabled = !isSaving
            )
        }
        if (title.isNotBlank() && validationError != null) Text(validationError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        if (onDelete != null) {
            TextButton(onClick = onDelete, enabled = !isSaving) {
                Text("Eliminar entrega", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
    if (confirmDiscard) {
        SmoothDialog(onDismissRequest = { confirmDiscard = false }) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("¿Descartar cambios?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("El texto de esta entrega se conservará como borrador si sigues editando.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton("Seguir editando", onClick = { confirmDiscard = false }, modifier = Modifier.weight(1f))
                    BrishPrimaryButton(
                        text = "Descartar",
                        onClick = {
                            titleDraft.discard(); descriptionDraft.discard(); dueDateDraft.discard(); hasDueDateDraft.discard(); timeDraft.discard()
                            confirmDiscard = false
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
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
                    pickerState.selectedDateMillis?.let { dueDateDraft.value = pickerDateToLocalMillis(it).toString() }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
internal fun CourseWriteError(error: String?, onDismiss: () -> Unit) {
    if (error != null) {
        SmoothDialog(onDismissRequest = onDismiss) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("No se pudo guardar", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BrishPrimaryButton("Aceptar", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
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
        BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = if (isDark) .22f else .14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides if (isDark) Color.White else accent,
                            content = icon
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(11.dp), content = content)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton("Cancelar", onClick = onDismiss, modifier = Modifier.weight(1f))
                    BrishPrimaryButton(confirmLabel, enabled = confirmEnabled, onClick = onConfirm, modifier = Modifier.weight(1f))
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
