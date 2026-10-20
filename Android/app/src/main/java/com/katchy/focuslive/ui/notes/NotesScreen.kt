package com.katchy.focuslive.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.NoteCategory
import com.katchy.focuslive.ui.components.BrishIconTile
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.BrishScreenHeader
import com.katchy.focuslive.ui.components.brishSharedBounds
import com.katchy.focuslive.ui.theme.AreaHome
import com.katchy.focuslive.ui.theme.AreaHomeBg
import com.katchy.focuslive.ui.theme.AreaPersonal
import com.katchy.focuslive.ui.theme.AreaPersonalBg
import com.katchy.focuslive.ui.theme.AreaWork
import com.katchy.focuslive.ui.theme.AreaWorkBg
import com.katchy.focuslive.ui.theme.BrishHeroBrush
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishOnYellow
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishWarning
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.BrishYellowSoft
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.util.bounceClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class NotesFilter(val label: String) { All("Todas"), Pinned("Fijadas"), Lists("Listas"), Locked("Bloqueadas"), Trash("Papelera") }

@Composable
fun NotesScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToStudy: (String) -> Unit = {},
    onOpenNote: (String?, String?, String?) -> Unit = { _, _, _ -> },
    viewModel: NotesViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val trash by viewModel.trashNotes.collectAsState()
    val error by viewModel.error.collectAsState()
    val busy by viewModel.busy.collectAsState()

    var folderId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(NotesFilter.All) }
    var showFolderForm by remember { mutableStateOf(false) }
    var folderName by rememberSaveable { mutableStateOf("") }
    var renameFolder by remember { mutableStateOf(false) }
    var confirmFolderDelete by remember { mutableStateOf(false) }
    var permanentDelete by remember { mutableStateOf<Note?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it); viewModel.dismissError() } }

    val folder = categories.find { it.id == folderId }
    BackHandler(enabled = folderId != null || filter == NotesFilter.Trash) {
        if (folderId != null) folderId = null else filter = NotesFilter.All
    }

    val source = if (filter == NotesFilter.Trash) trash else notes
    val searching = query.isNotBlank()
    val visible = source.filter { note ->
        (folderId == null || (folderId == UNFILED && note.categoryId == null) || note.categoryId == folderId) &&
            (!searching || note.title.contains(query, true) || (!note.isLocked && note.content.contains(query, true))) &&
            when (filter) {
                NotesFilter.Pinned -> note.isPinned
                NotesFilter.Lists -> parseNoteBody(note.content).isChecklist
                NotesFilter.Locked -> note.isLocked
                else -> true
            }
    }
    val library = folderId == null && filter == NotesFilter.All && !searching

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 24.dp + com.katchy.focuslive.ui.main.LocalBottomBarInset.current),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                AnimatedContent(folder?.name ?: if (folderId == UNFILED) "Sin carpeta" else "Notas", transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                }, label = "notesTitle") { title ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (folderId != null) {
                            BrishRoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Volver a Notas", { folderId = null })
                            Spacer(Modifier.width(12.dp))
                        }
                        BrishScreenHeader(title, Modifier.weight(1f)) {
                            if (folder != null) {
                                BrishRoundButton(Icons.Rounded.Edit, "Renombrar carpeta", { folderName = folder.name; renameFolder = true; showFolderForm = true })
                                BrishRoundButton(Icons.Rounded.DeleteOutline, "Eliminar carpeta", { confirmFolderDelete = true })
                            }
                        }
                    }
                }
            }
            item(key = "search") { SearchPill(query, { query = it }) }
            if (folderId == null) {
                item(key = "filters") {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NotesFilter.entries.forEach { f -> FilterPill(f.label, filter == f) { filter = f } }
                    }
                }
            }

            if (library) {
                val pinned = notes.filter { it.isPinned }
                if (pinned.isNotEmpty()) {
                    item(key = "pinnedTitle") { SectionLabel("FIJADAS") }
                    item(key = "pinned") {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pinned.forEachIndexed { i, note ->
                                PinnedCard(note, highlighted = i == 0, onOpen = { onOpenNote(note.id, note.categoryId, note.courseId) }, onUnpin = { viewModel.togglePin(note) })
                            }
                        }
                    }
                }
                item(key = "foldersTitle") {
                    SectionLabel("CARPETAS", action = "Nueva") { renameFolder = false; folderName = ""; showFolderForm = true }
                }
                item(key = "folders") {
                    Column(Modifier.fillMaxWidth().brishCard().padding(vertical = 4.dp)) {
                        val rows = listOf(Triple(UNFILED, "Sin carpeta", notes.count { it.categoryId == null })) +
                            categories.map { Triple(it.id, it.name, notes.count { n -> n.categoryId == it.id }) }
                        rows.forEachIndexed { i, (id, name, count) ->
                            if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            FolderRow(name, count, folderStyle(name, id == UNFILED)) { folderId = id }
                        }
                    }
                }
                item(key = "recentTitle") { SectionLabel("RECIENTES") }
            } else if (filter == NotesFilter.Trash) {
                item(key = "trashInfo") {
                    Text("Restaura una nota o elimínala para siempre.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (visible.isEmpty()) {
                item(key = "empty") {
                    EmptyNotes(
                        when {
                            filter == NotesFilter.Trash -> "La papelera está vacía"
                            searching -> "Nada coincide con «$query»"
                            filter == NotesFilter.Pinned -> "Mantén presionada una nota para fijarla"
                            filter == NotesFilter.Lists -> "Aún no tienes listas"
                            filter == NotesFilter.Locked -> "Bloquea una nota desde su editor con el candado"
                            folderId != null -> "Esta carpeta está vacía"
                            else -> "Tu primera nota te espera"
                        },
                        if (filter == NotesFilter.Trash || searching) null else "Crear nota"
                    ) { onOpenNote(null, folder?.id, null) }
                }
            }
            items(visible, key = { it.id }) { note ->
                NoteRow(
                    note = note,
                    folder = categories.find { it.id == note.categoryId },
                    trash = filter == NotesFilter.Trash,
                    modifier = Modifier.animateItem(),
                    onOpen = { onOpenNote(note.id, note.categoryId, note.courseId) },
                    onPin = { viewModel.togglePin(note) },
                    onStudy = if (note.flashcards.isNotEmpty()) ({ onNavigateToStudy(note.id) }) else null,
                    onRestore = { viewModel.restoreFromTrash(note) },
                    onDelete = { permanentDelete = note }
                )
            }
            if (folderId != null && visible.isNotEmpty()) {
                item(key = "newInFolder") {
                    Text(
                        "＋ Nueva nota aquí",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                            .clickable(role = Role.Button) { onOpenNote(null, folder?.id, null) }.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (showFolderForm) {
        AlertDialog(
            onDismissRequest = { showFolderForm = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(if (renameFolder) "Renombrar carpeta" else "Nueva carpeta") },
            text = {
                OutlinedTextField(folderName, { folderName = it }, label = { Text("Nombre") }, singleLine = true, shape = RoundedCornerShape(16.dp))
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
            text = { Text("Las notas se conservan y pasan a «Sin carpeta».") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { viewModel.deleteCategory(folder) { confirmFolderDelete = false; folderId = null } }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmFolderDelete = false }) { Text("Cancelar") } }
        )
    }
    permanentDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { permanentDelete = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Eliminar para siempre") },
            text = { Text("Se eliminará «${note.title.ifBlank { "Sin título" }}». No se puede deshacer.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { viewModel.deletePermanentlyFromTrash(note.id) { permanentDelete = null } }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { permanentDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SearchPill(query: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(54.dp).brishCard(CircleShape).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Buscar en todas tus notas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Buscar notas" }
            )
        }
        AnimatedVisibility(query.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda", modifier = Modifier.clip(CircleShape).clickable { onChange("") }.padding(4.dp))
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) BrishYellow else MaterialTheme.colorScheme.surface, BrishMotion.soft(), label = "pill")
    Text(
        label,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = if (selected) BrishInk else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.brishShadow(CircleShape, elevation = 4.dp, strength = 0.5f).clip(CircleShape).background(bg)
            .clickable(role = Role.Tab, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun SectionLabel(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (action != null) {
            Text("$action ›", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

private data class FolderLook(val icon: ImageVector, val bg: Color, val fg: Color)

private fun folderStyle(name: String, unfiled: Boolean): FolderLook {
    val n = name.lowercase()
    return when {
        unfiled -> FolderLook(Icons.Rounded.Description, BrishYellowSoft, BrishInk)
        listOf("comida", "receta", "compras", "mercado").any { it in n } -> FolderLook(Icons.Rounded.Restaurant, AreaHomeBg, AreaHome)
        listOf("idea", "proyecto", "video").any { it in n } -> FolderLook(Icons.Rounded.Lightbulb, AreaPersonalBg, AreaPersonal)
        else -> FolderLook(Icons.Rounded.Folder, AreaWorkBg, AreaWork)
    }
}

@Composable
private fun FolderRow(name: String, count: Int, look: FolderLook, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrishIconTile(look.icon, look.bg, look.fg, size = 40.dp)
        Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count ›", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedCard(note: Note, highlighted: Boolean, onOpen: () -> Unit, onUnpin: () -> Unit) {
    val body = remember(note.content) { parseNoteBody(note.content) }
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .width(168.dp)
            .heightIn(min = 196.dp)
            .brishSharedBounds("note-${note.id}", shape)
            .then(if (highlighted) Modifier.brishShadow(shape, elevation = 14.dp, tone = Color(0xFFD6A014), strength = 1.4f).clip(shape).background(BrishHeroBrush).brishEdgeHighlight() else Modifier.brishCard(shape).clip(shape))
            .combinedClickable(onClick = onOpen, onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onUnpin() })
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.PushPin, contentDescription = "Fijada", tint = if (highlighted) BrishWarning else AreaPersonal, modifier = Modifier.size(16.dp))
        Text(note.title.ifBlank { "Sin título" }, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (highlighted) BrishInk else MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (note.isLocked) {
            Text("🔒 Bloqueada", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (highlighted) BrishOnYellow else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(relativeTime(note.updatedAt), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (highlighted) BrishOnYellow else MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (body.isChecklist) {
            body.items.take(3).forEach { item ->
                Text((if (item.checked) "☑ " else "☐ ") + item.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = if (highlighted) BrishOnYellow else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            Text("Lista · ${body.doneCount} de ${body.items.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (highlighted) BrishWarning else MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(body.text, fontSize = 13.sp, color = if (highlighted) BrishOnYellow else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Text(relativeTime(note.updatedAt), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (highlighted) BrishOnYellow else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(
    note: Note,
    folder: NoteCategory?,
    trash: Boolean,
    modifier: Modifier,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onStudy: (() -> Unit)?,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val body = remember(note.content) { parseNoteBody(note.content) }
    val haptics = LocalHapticFeedback.current
    val look = folderStyle(folder?.name.orEmpty(), folder == null)
    Column(
        modifier.fillMaxWidth()
            // Una nota fijada ya tiene su tarjeta arriba: solo esa se expande.
            .then(if (!note.isPinned) Modifier.brishSharedBounds("note-${note.id}", BrishShapes.card) else Modifier)
            .brishCard().clip(BrishShapes.card)
            .then(if (trash) Modifier else Modifier.combinedClickable(onClick = onOpen, onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onPin() }))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrishIconTile(if (body.isChecklist) Icons.Rounded.Style else look.icon, look.bg, look.fg, size = 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(note.title.ifBlank { "Sin título" }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        if (note.isLocked) "🔒 Bloqueada" else null,
                        folder?.name,
                        if (body.isChecklist && !note.isLocked) "lista ${body.doneCount}/${body.items.size}" else null,
                        note.attachments.size.takeIf { it > 0 }?.let { if (it == 1) "1 foto" else "$it fotos" },
                        note.linkedDate?.let { SimpleDateFormat("d MMM", Locale("es", "PE")).format(Date(it)).replace(".", "") }
                    ).joinToString(" · ").ifBlank {
                        if (note.isLocked) "" else body.text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (note.isPinned) Icon(Icons.Rounded.PushPin, contentDescription = "Fijada", tint = BrishWarning, modifier = Modifier.size(16.dp))
            Text(relativeTime(note.updatedAt), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trash) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionPill("Restaurar", BrishYellow, BrishInk, Modifier.weight(1f), onRestore)
                ActionPill("Eliminar", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.error, Modifier.weight(1f), onDelete)
            }
        } else if (onStudy != null) {
            Text("Repasar tarjetas ›", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onStudy).padding(horizontal = 6.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun ActionPill(label: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(44.dp).clip(CircleShape).background(bg).bounceClick(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, fontWeight = FontWeight.ExtraBold, color = fg)
    }
}

@Composable
private fun EmptyNotes(title: String, action: String?, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().brishCard().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text("Toca ＋ y escribe «idea: …» o «nota: …» para crear una al instante.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) {
            Text(action, fontWeight = FontWeight.ExtraBold, color = Color.White,
                modifier = Modifier.clip(CircleShape).background(BrishInk).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 18.dp, vertical = 10.dp))
        }
    }
}

private fun relativeTime(timestamp: Long): String {
    val minutes = (System.currentTimeMillis() - timestamp) / 60_000
    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "hace $minutes min"
        minutes < 24 * 60 -> "hace ${minutes / 60} h"
        minutes < 48 * 60 -> "ayer"
        else -> SimpleDateFormat("d MMM", Locale("es", "PE")).format(Date(timestamp)).replace(".", "")
    }
}

private const val UNFILED = "__unfiled"
