package com.katchy.focuslive.ui.notes

import android.content.Intent
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.brishSharedBounds
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.AreaHealthBg
import com.katchy.focuslive.ui.theme.AreaHome
import com.katchy.focuslive.ui.theme.AreaHomeBg
import com.katchy.focuslive.ui.theme.BrishCard
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishNightCard
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.bounceClick

@Composable
fun NoteEditorScreen(
    onBack: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val folders by viewModel.categories.collectAsState()
    val courses by viewModel.courses.collectAsState()
    var folderMenu by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var newItem by remember { mutableStateOf("") }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val note = state.note
    val body = remember(note.content) { parseNoteBody(note.content) }
    val editable = !state.loading && !state.closing && !state.unavailable

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) viewModel.flush() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // Atrás predictivo: el editor se encoge con el gesto y al soltar guarda y cierra.
    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler { progress ->
        try {
            progress.collect { backProgress = it.progress }
            viewModel.close(onBack)
        } catch (e: kotlinx.coroutines.CancellationException) {
            backProgress = 0f
            throw e
        }
    }
    LaunchedEffect(state.closing) { if (!state.closing) backProgress = 0f }

    fun updateBody(transform: (NoteBody) -> NoteBody) {
        viewModel.change { it.copy(content = serializeNoteBody(transform(parseNoteBody(it.content)))) }
    }
    fun addItem() {
        val text = newItem.trim()
        if (text.isEmpty()) return
        updateBody { it.copy(items = it.items + ChecklistItem(text, false)) }
        newItem = ""
    }

    Box(
        Modifier
            .fillMaxSize()
            .brishSharedBounds("note-${viewModel.id}", RoundedCornerShape(0.dp))
            .graphicsLayer {
                val s = 1f - 0.08f * backProgress
                scaleX = s
                scaleY = s
                translationX = 24.dp.toPx() * backProgress
                shape = RoundedCornerShape(28.dp * backProgress)
                clip = backProgress > 0f
            }
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onBackground)
            return@Box
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrishRoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", { viewModel.close(onBack) })
                Spacer(Modifier.weight(1f))
                SaveStatus(state.status, state.error, onRetry = viewModel::retry)
                Spacer(Modifier.weight(1f))
                BrishRoundButton(
                    Icons.Rounded.PushPin,
                    if (note.isPinned) "Desfijar" else "Fijar",
                    { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.change { it.copy(isPinned = !it.isPinned) } },
                    container = if (note.isPinned) BrishYellow else null
                )
            }

            if (state.unavailable) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("No se puede abrir esta nota", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(state.error ?: "La nota no está disponible.", color = MaterialTheme.colorScheme.onErrorContainer)
                }
                return@Column
            }

            Box {
                if (note.title.isEmpty()) Text("Título", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                BasicTextField(
                    value = note.title,
                    onValueChange = { t -> viewModel.change { it.copy(title = t) } },
                    enabled = editable,
                    textStyle = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground, letterSpacing = (-0.6).sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Título de la nota" }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    MetaPill(Icons.Rounded.Folder, (folders.find { it.id == note.categoryId }?.name ?: "Sin carpeta") + " ▾", AreaHomeBg, AreaHome) { folderMenu = true }
                    DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                        DropdownMenuItem(text = { Text("Sin carpeta") }, onClick = { viewModel.change { it.copy(categoryId = null) }; folderMenu = false })
                        folders.forEach { f ->
                            DropdownMenuItem(text = { Text(f.name) }, onClick = { viewModel.change { it.copy(categoryId = f.id) }; folderMenu = false })
                        }
                    }
                }
                courses.find { it.id == note.courseId }?.let { course ->
                    MetaPill(Icons.Rounded.Close, course.name, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant) {
                        viewModel.change { it.copy(courseId = null) }
                    }
                }
            }

            if (body.isChecklist) {
                Column(Modifier.fillMaxWidth().animateContentSize(BrishMotion.soft())) {
                    body.items.forEachIndexed { index, item ->
                        ChecklistRow(
                            item = item,
                            enabled = editable,
                            onToggle = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                updateBody { b -> b.copy(items = b.items.mapIndexed { i, it -> if (i == index) it.copy(checked = !it.checked) else it }) }
                            },
                            onText = { text -> updateBody { b -> b.copy(items = b.items.mapIndexed { i, it -> if (i == index) it.copy(text = text) else it }) } },
                            onRemove = { updateBody { b -> b.copy(items = b.items.filterIndexed { i, _ -> i != index }) } }
                        )
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).clickable { addItem() },
                            contentAlignment = Alignment.Center) { Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Box(Modifier.weight(1f)) {
                            if (newItem.isEmpty()) Text("Añadir elemento", fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            BasicTextField(
                                value = newItem,
                                onValueChange = { newItem = it.replace("\n", "") },
                                enabled = editable,
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { addItem() }),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Nuevo elemento de la lista" }
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }

            Box(Modifier.heightIn(min = 240.dp)) {
                if (body.text.isEmpty()) {
                    Text(
                        if (body.isChecklist) "Notas adicionales…" else "Escribe aquí. Brish guarda solo cada cambio.",
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = body.text,
                    onValueChange = { text -> updateBody { it.copy(text = text) } },
                    enabled = editable,
                    textStyle = TextStyle(fontSize = 17.sp, lineHeight = 27.sp, color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Texto de la nota" }
                )
            }
        }

        if (!state.unavailable) {
            EditorToolbar(
                isList = body.isChecklist,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 14.dp),
                onList = {
                    if (!body.isChecklist) {
                        // Convierte cada línea del texto en un elemento; si no hay texto, abre una lista vacía.
                        val lines = body.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        updateBody { NoteBody(lines.map { ChecklistItem(it, false) }.ifEmpty { listOf(ChecklistItem("", false)) }, "") }
                    } else {
                        updateBody { NoteBody(emptyList(), (it.items.map { i -> i.text } + listOf(it.text)).filter { t -> t.isNotBlank() }.joinToString("\n")) }
                    }
                },
                onShare = {
                    val text = listOf(note.title, note.content).filter { it.isNotBlank() }.joinToString("\n\n")
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Compartir nota"))
                },
                onDelete = { deleteConfirm = true },
                canDelete = editable && (note.title.isNotBlank() || note.content.isNotBlank())
            )
        }
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Mover a la papelera") },
            text = { Text("Podrás recuperarla desde Notas → Papelera.") },
            confirmButton = { TextButton(onClick = { deleteConfirm = false; viewModel.delete(onBack) }) { Text("Mover") } },
            dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SaveStatus(status: String, error: String?, onRetry: () -> Unit) {
    val failed = error != null || status == "No se pudo guardar"
    val saving = status == "Guardando"
    AnimatedContent(
        targetState = when { failed -> 2; saving -> 1; else -> 0 },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "saveStatus"
    ) { s ->
        val (bg, fg, label) = when (s) {
            2 -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "Reintentar")
            1 -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Guardando…")
            else -> Triple(AreaHealthBg, AreaHealth, "Guardado")
        }
        Row(
            Modifier.height(36.dp).clip(CircleShape).background(bg)
                .then(if (s == 2) Modifier.clickable(role = Role.Button, onClick = onRetry) else Modifier)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (s) {
                2 -> Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                1 -> CircularProgressIndicator(Modifier.size(14.dp), color = fg, strokeWidth = 2.dp)
                else -> Icon(Icons.Rounded.Check, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            }
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
        }
    }
}

@Composable
private fun MetaPill(icon: ImageVector, label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(CircleShape).background(bg).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
    }
}

@Composable
private fun ChecklistRow(item: ChecklistItem, enabled: Boolean, onToggle: () -> Unit, onText: (String) -> Unit, onRemove: () -> Unit) {
    val box by animateColorAsState(if (item.checked) BrishYellow else Color.Transparent, BrishMotion.lively(), label = "box")
    val textColor by animateColorAsState(
        if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, label = "itemText"
    )
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(box)
                .border(2.dp, if (item.checked) BrishYellow else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(role = Role.Checkbox, enabled = enabled, onClick = onToggle)
                .semantics { stateDescription = if (item.checked) "Hecho" else "Pendiente" },
            contentAlignment = Alignment.Center
        ) {
            CheckMark(item.checked)
        }
        BasicTextField(
            value = item.text,
            onValueChange = { onText(it.replace("\n", "")) },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Rounded.Close,
            contentDescription = "Quitar ${item.text}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(32.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onRemove).padding(7.dp)
        )
    }
}

@Composable
private fun CheckMark(checked: Boolean) {
    AnimatedVisibility(checked, enter = scaleIn(BrishMotion.lively()) + fadeIn(), exit = fadeOut()) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = BrishInk, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EditorToolbar(isList: Boolean, modifier: Modifier, onList: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit, canDelete: Boolean) {
    val dark = isDarkTheme()
    Row(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .brishShadow(BrishShapes.bar, elevation = 18.dp, strength = if (dark) 0f else 1f)
            .clip(BrishShapes.bar)
            .background((if (dark) BrishNightCard else BrishCard).copy(alpha = 0.97f))
            .brishEdgeHighlight(alpha = if (dark) 0.06f else 0.9f, inset = 28.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ToolButton(Icons.AutoMirrored.Rounded.FormatListBulleted, if (isList) "Convertir en texto" else "Convertir en lista", active = isList, onClick = onList)
        ToolButton(Icons.Rounded.Share, "Compartir", active = false, onClick = onShare)
        ToolButton(Icons.Rounded.DeleteOutline, "Mover a la papelera", active = false, enabled = canDelete, onClick = onDelete)
    }
}

@Composable
private fun ToolButton(icon: ImageVector, description: String, active: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val bg by animateColorAsState(if (active) BrishYellow else Color.Transparent, BrishMotion.soft(), label = "tool")
    Box(
        Modifier.size(56.dp).clip(CircleShape).background(bg)
            .then(if (enabled) Modifier.bounceClick(onClick = onClick) else Modifier)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (active) BrishInk else MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f), modifier = Modifier.size(24.dp))
    }
}
