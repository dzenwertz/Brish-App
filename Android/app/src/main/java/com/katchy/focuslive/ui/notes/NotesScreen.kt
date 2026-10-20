package com.katchy.focuslive.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.NoteCategory
import com.katchy.focuslive.ui.components.BrishTopHeader
import com.katchy.focuslive.ui.components.brishMutedPanelColor
import com.katchy.focuslive.ui.components.brishPanelBorder
import com.katchy.focuslive.ui.components.brishPanelColor
import com.katchy.focuslive.ui.components.brishPrimaryActionColor
import com.katchy.focuslive.ui.components.brishPrimaryActionContentColor
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.bounceClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToStudy: (String) -> Unit = {},
    onOpenNote: (String?, String?, String?) -> Unit = { _, _, _ -> },
    viewModel: NotesViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val trash by viewModel.trashNotes.collectAsState()
    val error by viewModel.error.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val isDark = isDarkTheme()

    var folderId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var courseId by rememberSaveable { mutableStateOf<String?>(null) }
    var showTrash by rememberSaveable { mutableStateOf(false) }
    var showFolderForm by remember { mutableStateOf(false) }
    var folderName by rememberSaveable { mutableStateOf("") }
    var renameFolder by remember { mutableStateOf(false) }
    var confirmFolderDelete by remember { mutableStateOf(false) }
    var permanentDelete by remember { mutableStateOf<Note?>(null) }

    val folder = categories.find { it.id == folderId }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.dismissError() }
    }
    BackHandler(enabled = folderId != null || showTrash) {
        if (showTrash) showTrash = false else folderId = null
    }

    val visible = (if (showTrash) trash else notes).filter { note ->
        (folderId == null || (folderId == UNFILED && note.categoryId == null) || note.categoryId == folderId) &&
            (courseId == null || note.courseId == courseId) &&
            (note.title.contains(query, true) || note.content.contains(query, true))
    }

    val title = when {
        showTrash -> "Papelera"
        folder != null -> folder.name
        folderId == UNFILED -> "Sin carpeta"
        else -> "Notas"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BrishTopHeader(
                title = title,
                onBack = if (folderId != null || showTrash) ({ if (showTrash) showTrash = false else folderId = null }) else null,
                onNavigateToSettings = onNavigateToSettings,
                actions = {
                    if (folder != null && !showTrash) {
                        IconButton(onClick = { folderName = folder.name; renameFolder = true; showFolderForm = true }) {
                            Icon(Icons.Rounded.Edit, "Renombrar carpeta")
                        }
                        IconButton(onClick = { confirmFolderDelete = true }) {
                            Icon(Icons.Rounded.DeleteOutline, "Eliminar carpeta")
                        }
                    } else if (folderId == null) {
                        IconButton(onClick = { showTrash = !showTrash; folderId = null }) {
                            Icon(Icons.Rounded.DeleteOutline, "Papelera")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (!showTrash) {
                FloatingActionButton(
                    onClick = { onOpenNote(null, folder?.id, courseId) },
                    containerColor = brishPrimaryActionColor(),
                    contentColor = brishPrimaryActionContentColor(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(54.dp).bounceClick()
                ) { Icon(Icons.Rounded.Add, "Nueva nota", modifier = Modifier.size(24.dp)) }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (folderId == null && !showTrash) {
                item { NotesLibraryHero(noteCount = notes.size, folderCount = categories.size) }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(if (showTrash) "Buscar en papelera" else "Buscar en tus apuntes") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        AnimatedVisibility(visible = query.isNotBlank(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Limpiar búsqueda") }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (courses.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = courseId == null,
                                onClick = { courseId = null },
                                label = { Text("Todos") },
                                leadingIcon = { Icon(Icons.Rounded.School, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        items(courses, key = { it.id }) { course ->
                            FilterChip(
                                selected = courseId == course.id,
                                onClick = { courseId = if (courseId == course.id) null else course.id },
                                label = { Text(course.name, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            if (folderId == null && !showTrash) {
                item {
                    NotesSectionHeader(
                        title = "Carpetas",
                        subtitle = "Organiza sin perder el vínculo con el curso",
                        action = "Nueva",
                        onAction = { renameFolder = false; folderName = ""; showFolderForm = true }
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            FolderCard(
                                title = "Sin carpeta",
                                count = notes.count { it.categoryId == null },
                                icon = Icons.Rounded.Description,
                                onClick = { folderId = UNFILED }
                            )
                        }
                        items(categories, key = { it.id }) { category ->
                            FolderCard(
                                title = category.name,
                                count = notes.count { it.categoryId == category.id },
                                icon = Icons.Rounded.Folder,
                                onClick = { folderId = category.id }
                            )
                        }
                    }
                }
                item {
                    NotesSectionHeader(
                        title = "Notas recientes",
                        subtitle = if (visible.isEmpty()) "Tu biblioteca está lista" else "${visible.size} apuntes disponibles"
                    )
                }
            } else if (!showTrash) {
                item {
                    NotesSectionHeader(
                        title = if (folderId == UNFILED) "Apuntes sin carpeta" else "Contenido",
                        subtitle = if (visible.isEmpty()) "Esta carpeta está vacía" else "${visible.size} apuntes"
                    )
                }
            }

            if (showTrash) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = brishPanelColor(),
                        border = brishPanelBorder(),
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column {
                                Text("Papelera", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Restaura un apunte o elimínalo definitivamente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (visible.isEmpty()) {
                item {
                    NotesEmptyState(
                        icon = if (showTrash) Icons.Rounded.DeleteOutline else if (query.isNotBlank()) Icons.Rounded.Search else Icons.Rounded.Description,
                        title = when {
                            showTrash -> "La papelera está vacía"
                            query.isNotBlank() -> "No encontramos coincidencias"
                            folderId != null -> "Todavía no hay apuntes aquí"
                            else -> "Empieza tu biblioteca"
                        },
                        description = when {
                            showTrash -> "Los apuntes que elimines aparecerán aquí."
                            query.isNotBlank() -> "Prueba con otra palabra o cambia el filtro de curso."
                            folderId != null -> "Crea una nota y quedará guardada directamente en esta carpeta."
                            else -> "Crea tu primer apunte y Brish lo guardará automáticamente."
                        }
                    )
                }
            }

            items(visible, key = { it.id }) { note ->
                NoteLibraryCard(
                    note = note,
                    folder = categories.find { it.id == note.categoryId },
                    courseName = courses.find { it.id == note.courseId }?.name,
                    showTrashActions = showTrash,
                    onOpen = { onOpenNote(note.id, note.categoryId, note.courseId) },
                    onStudy = if (note.flashcards.isNotEmpty()) ({ onNavigateToStudy(note.id) }) else null,
                    onRestore = { viewModel.restoreFromTrash(note) },
                    onDelete = { permanentDelete = note }
                )
            }
        }
    }

    if (showFolderForm) {
        AlertDialog(
            onDismissRequest = { showFolderForm = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(if (renameFolder) "Renombrar carpeta" else "Nueva carpeta") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                TextButton(enabled = !busy && folderName.isNotBlank(), onClick = {
                    if (renameFolder && folder != null) viewModel.updateCategory(folder, folderName) { showFolderForm = false }
                    else viewModel.addCategory(folderName) { showFolderForm = false }
                }) { Text(if (busy) "Guardando…" else "Guardar") }
            },
            dismissButton = { TextButton(onClick = { showFolderForm = false }) { Text("Cancelar") } }
        )
    }

    if (confirmFolderDelete && folder != null) {
        AlertDialog(
            onDismissRequest = { confirmFolderDelete = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Eliminar carpeta") },
            text = { Text("Las notas se conservarán sin carpeta. Su vínculo con los cursos se mantiene.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    viewModel.deleteCategory(folder) { confirmFolderDelete = false; folderId = null }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmFolderDelete = false }) { Text("Cancelar") } }
        )
    }

    permanentDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { permanentDelete = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Eliminar definitivamente") },
            text = { Text("Se eliminará «${note.title.ifBlank { "Sin título" }}». Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    viewModel.deletePermanentlyFromTrash(note.id) { permanentDelete = null }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { permanentDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun NotesLibraryHero(noteCount: Int, folderCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = brishPanelColor(),
        border = brishPanelBorder(),
        shadowElevation = 0.dp
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Surface(shape = RoundedCornerShape(13.dp), color = brishMutedPanelColor()) {
                Icon(Icons.Rounded.MenuBook, null, tint = Color(0xFF06B6D4), modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tu biblioteca de estudio", style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), fontWeight = FontWeight.ExtraBold)
                AnimatedContent(
                    targetState = noteCount to folderCount,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = .98f)) togetherWith (fadeOut() + scaleOut(targetScale = 1.02f)) },
                    label = "notes-count"
                ) { (noteTotal, folderTotal) ->
                    Text(
                        "$noteTotal ${if (noteTotal == 1) "apunte" else "apuntes"} · $folderTotal ${if (folderTotal == 1) "carpeta" else "carpetas"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesSectionHeader(title: String, subtitle: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun FolderCard(title: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(156.dp).bounceClick(scaleDown = .97f, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = brishPanelColor(),
        border = brishPanelBorder(),
        shadowElevation = 0.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(13.dp), color = brishMutedPanelColor()) {
                Icon(icon, null, tint = Color(0xFF06B6D4), modifier = Modifier.padding(9.dp).size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text("$count ${if (count == 1) "nota" else "notas"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NoteLibraryCard(
    note: Note,
    folder: NoteCategory?,
    courseName: String?,
    showTrashActions: Boolean,
    onOpen: () -> Unit,
    onStudy: (() -> Unit)?,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onOpen,
        enabled = !showTrashActions,
        modifier = Modifier.fillMaxWidth().animateContentSize().bounceClick(scaleDown = .985f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = brishPanelColor()),
        border = brishPanelBorder()
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(13.dp), color = brishMutedPanelColor()) {
                    Icon(Icons.Rounded.Description, null, modifier = Modifier.padding(9.dp).size(20.dp), tint = Color(0xFF06B6D4))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(note.title.ifBlank { "Sin título" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (note.content.isNotBlank()) {
                        Text(note.content, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(noteTimeLabel(note.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOfNotNull(folder?.name, courseName).forEach { label ->
                    SuggestionChip(onClick = {}, enabled = false, label = { Text(label, maxLines = 1) })
                }
            }
            if (showTrashActions) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onRestore, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Rounded.Restore, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Restaurar")
                    }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Rounded.DeleteForever, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Eliminar")
                    }
                }
            } else if (onStudy != null) {
                TextButton(onClick = onStudy) {
                    Icon(Icons.Rounded.Style, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Repasar tarjetas")
                }
            }
        }
    }
}

@Composable
private fun NotesEmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = brishPanelColor(),
        border = brishPanelBorder(),
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = RoundedCornerShape(13.dp), color = brishMutedPanelColor()) {
                Icon(icon, null, modifier = Modifier.padding(10.dp).size(22.dp), tint = Color(0xFF06B6D4))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun noteTimeLabel(timestamp: Long): String =
    SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()).format(Date(timestamp))

private const val UNFILED = "__unfiled"
