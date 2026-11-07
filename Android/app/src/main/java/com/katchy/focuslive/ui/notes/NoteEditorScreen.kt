package com.katchy.focuslive.ui.notes

import android.content.Intent
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.katchy.focuslive.ui.components.BrishCardSurface
import com.katchy.focuslive.ui.components.BrishCelebrationBurst
import com.katchy.focuslive.ui.components.BrishPrimaryButton
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.BrishSecondaryButton
import com.katchy.focuslive.ui.components.BrishTextField
import com.katchy.focuslive.ui.components.DraftRecoveryNotice
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.components.brishSharedBounds
import com.katchy.focuslive.ui.components.rememberPersistentDraftText
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
import com.katchy.focuslive.ui.theme.BrishYellowSoft
import com.katchy.focuslive.ui.theme.MotionTokens
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import com.katchy.focuslive.ui.util.UnlockResult
import com.katchy.focuslive.ui.util.bounceClick
import com.katchy.focuslive.ui.util.canLockNotes
import com.katchy.focuslive.ui.util.rememberUnlocker
import kotlinx.coroutines.launch


/** Alto de la barra de herramientas flotante del editor, con su margen. */
private val EditorToolbarSpace = 100.dp

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
    var showFlashcardsDialog by remember { mutableStateOf(false) }
    val newItemDraft = rememberPersistentDraftText("note-editor:${viewModel.id}:checklist-item")
    var newItem by newItemDraft::value
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val reduced = rememberReducedMotion()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val note = state.note
    val body = remember(note.content) { parseNoteBody(note.content) }
    val editable = !state.loading && !state.closing && !state.unavailable
    val scope = rememberCoroutineScope()
    val unlocker = rememberUnlocker()
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var lockMessage by remember { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf<String?>(null) }
    val locked = !state.loading && note.isLocked && !unlocked

    val pinBounce = remember { Animatable(1f) }
    var initialPinned by remember { mutableStateOf(note.isPinned) }
    LaunchedEffect(note.isPinned) {
        if (note.isPinned != initialPinned) {
            initialPinned = note.isPinned
            if (!reduced) {
                pinBounce.animateTo(1.28f, MotionTokens.overshoot())
                pinBounce.animateTo(1f, MotionTokens.pressSpring())
            }
        }
    }

    fun askUnlock(then: () -> Unit = {}) = unlocker("Abrir nota bloqueada") { result ->
        when (result) {
            UnlockResult.Success -> { unlocked = true; lockMessage = null; then() }
            is UnlockResult.Unavailable -> lockMessage = result.message
            UnlockResult.Cancelled -> Unit
        }
    }
    // Una nota bloqueada pide la huella al abrirse.
    LaunchedEffect(state.loading) { if (!state.loading && note.isLocked && !unlocked) askUnlock() }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = NoteImages.import(context, uri)
            if (path != null) viewModel.change { it.copy(attachments = it.attachments + path) }
            else lockMessage = "No se pudo añadir la foto."
        }
    }
    fun pickDay() {
        val cal = java.util.Calendar.getInstance().apply { note.linkedDate?.let { timeInMillis = it } }
        android.app.DatePickerDialog(context, { _, y, m, d ->
            val day = java.util.Calendar.getInstance().apply { clear(); set(y, m, d) }.timeInMillis
            viewModel.change { it.copy(linkedDate = day) }
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) viewModel.flush()
        }
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
        newItemDraft.clearAfterSave()
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
                shape = RoundedCornerShape(28.dp * backProgress.coerceIn(0f, 1f))
                clip = backProgress > 0f
            }
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onBackground)
            return@Box
        }
        if (locked) {
            LockedPane(
                title = note.title,
                message = lockMessage,
                onBack = { onBack() },
                onUnlock = { askUnlock() },
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                // El área desplazable termina encima de la barra de herramientas: así el cursor nunca
                // queda escondido detrás de ella mientras se escribe.
                .padding(bottom = if (state.unavailable) 0.dp else EditorToolbarSpace)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrishRoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", { viewModel.close(onBack) })
                Spacer(Modifier.weight(1f))
                SaveStatus(state.status, state.error, onRetry = viewModel::retry)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.graphicsLayer {
                        scaleX = pinBounce.value
                        scaleY = pinBounce.value
                        rotationZ = (pinBounce.value - 1f) * -28f
                    }
                ) {
                    BrishRoundButton(
                        Icons.Rounded.PushPin,
                        if (note.isPinned) "Desfijar" else "Fijar",
                        { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.change { it.copy(isPinned = !it.isPinned) } },
                        container = if (note.isPinned) BrishYellow else null
                    )
                }
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

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    MetaPill(Icons.Rounded.Folder, (folders.find { it.id == note.categoryId }?.name ?: "Sin carpeta") + " ▾", AreaHomeBg, AreaHome) { folderMenu = true }
                    DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                        DropdownMenuItem(text = { Text("Sin carpeta") }, onClick = { viewModel.change { it.copy(categoryId = null) }; folderMenu = false })
                        folders.forEach { f ->
                            DropdownMenuItem(text = { Text(f.name) }, onClick = { viewModel.change { it.copy(categoryId = f.id) }; folderMenu = false })
                        }
                    }
                }
                note.linkedDate?.let { day ->
                    val label = java.text.SimpleDateFormat("EEE d MMM", java.util.Locale("es", "PE")).format(java.util.Date(day))
                        .replace(".", "").replaceFirstChar { it.uppercase() }
                    MetaPill(Icons.Rounded.CalendarMonth, "$label  ✕", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface) {
                        viewModel.change { it.copy(linkedDate = null) }
                    }
                }
                if (note.isLocked) {
                    MetaPill(Icons.Rounded.Lock, "Bloqueada", BrishInk, Color.White) {}
                }
                courses.find { it.id == note.courseId }?.let { course ->
                    MetaPill(Icons.Rounded.Close, course.name, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant) {
                        viewModel.change { it.copy(courseId = null) }
                    }
                }
                if (note.flashcards.isNotEmpty()) {
                    MetaPill(
                        Icons.AutoMirrored.Rounded.FormatListBulleted,
                        "${note.flashcards.size} ${if (note.flashcards.size == 1) "tarjeta" else "tarjetas"}",
                        BrishYellowSoft,
                        BrishInk
                    ) { showFlashcardsDialog = true }
                }
            }
            lockMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }

            if (note.attachments.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    note.attachments.forEach { path ->
                        Box {
                            AsyncImage(
                                model = java.io.File(path),
                                contentDescription = "Foto de la nota",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(width = 120.dp, height = 150.dp).clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant).clickable { viewing = path }
                            )
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
                                    .clickable(enabled = editable) {
                                        viewModel.change { it.copy(attachments = it.attachments - path) }
                                        NoteImages.delete(context, path)
                                    }
                                    .semantics { contentDescription = "Quitar foto" },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
            }

            if (body.isChecklist) {
                Column(Modifier.fillMaxWidth().animateContentSize(BrishMotion.soft())) {
                    val rowPx = with(LocalDensity.current) { ChecklistRowHeight.toPx() }
                    val target = dragFrom?.let { reorderTarget(it, dragDy, rowPx, body.items.size) }
                    body.items.forEachIndexed { index, item ->
                        // Los demás elementos se apartan con resorte para dejar hueco al que se arrastra.
                        val shift = when {
                            dragFrom == null || target == null || index == dragFrom -> 0f
                            dragFrom!! < target && index in (dragFrom!! + 1)..target -> -rowPx
                            dragFrom!! > target && index in target until dragFrom!! -> rowPx
                            else -> 0f
                        }
                        val animatedShift by animateFloatAsState(shift, BrishMotion.soft(), label = "reorder")
                        val dragging = index == dragFrom
                        ChecklistRow(
                            item = item,
                            modifier = Modifier
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (dragging) dragDy else animatedShift
                                    val s = if (dragging) 1.02f else 1f
                                    scaleX = s
                                    scaleY = s
                                }
                                .then(if (dragging) Modifier.brishShadow(RoundedCornerShape(14.dp), elevation = 14.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)) else Modifier),
                            handle = Modifier.pointerInput(index, body.items.size) {
                                detectDragGestures(
                                    onDragStart = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); dragFrom = index; dragDy = 0f },
                                    onDrag = { change, amount -> change.consume(); dragDy += amount.y },
                                    onDragEnd = {
                                        val from = dragFrom
                                        val to = from?.let { reorderTarget(it, dragDy, rowPx, body.items.size) }
                                        dragFrom = null
                                        dragDy = 0f
                                        if (from != null && to != null && from != to) updateBody { b -> b.copy(items = b.items.moved(from, to)) }
                                    },
                                    onDragCancel = { dragFrom = null; dragDy = 0f }
                                )
                            },
                            enabled = editable,
                            onToggle = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                updateBody { b -> b.copy(items = b.items.mapIndexed { i, it -> if (i == index) it.copy(checked = !it.checked) else it }) }
                            },
                            onText = { text -> updateBody { b -> b.copy(items = b.items.mapIndexed { i, it -> if (i == index) it.copy(text = text) else it }) } },
                            onRemove = { updateBody { b -> b.copy(items = b.items.filterIndexed { i, _ -> i != index }) } }
                        )
                    }
                    DraftRecoveryNotice(newItemDraft, Modifier.fillMaxWidth())
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
                canDelete = editable && (note.title.isNotBlank() || note.content.isNotBlank()),
                isLocked = note.isLocked,
                hasDay = note.linkedDate != null,
                hasFlashcards = note.flashcards.isNotEmpty(),
                onPhoto = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onDay = { pickDay() },
                onFlashcards = { showFlashcardsDialog = true },
                onLock = {
                    if (note.isLocked) askUnlock { viewModel.change { it.copy(isLocked = false) } }
                    else if (canLockNotes(context)) { unlocked = true; viewModel.change { it.copy(isLocked = true) } }
                    else lockMessage = "Configura una huella o un bloqueo de pantalla en tu teléfono para bloquear notas."
                }
            )
        }
    }

    if (showFlashcardsDialog) {
        FlashcardsEditorDialog(
            noteId = viewModel.id,
            flashcards = note.flashcards,
            onAdd = { front, back ->
                viewModel.change {
                    it.copy(flashcards = it.flashcards + com.katchy.focuslive.data.model.FlashcardItem(question = front, answer = back))
                }
            },
            onRemove = { cardId ->
                viewModel.change {
                    it.copy(flashcards = it.flashcards.filterNot { c -> c.id == cardId })
                }
            },
            onDismiss = { showFlashcardsDialog = false }
        )
    }

    viewing?.let { path ->
        Dialog(onDismissRequest = { viewing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable { viewing = null }, contentAlignment = Alignment.Center) {
                AsyncImage(model = java.io.File(path), contentDescription = "Foto ampliada", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().padding(12.dp))
            }
        }
    }

    if (deleteConfirm) {
        SmoothDialog(onDismissRequest = { deleteConfirm = false }) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("Mover a la papelera", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("Podrás recuperarla desde Notas → Papelera.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton("Cancelar", onClick = { deleteConfirm = false }, modifier = Modifier.weight(1f))
                    BrishPrimaryButton(
                        text = "Mover",
                        onClick = { deleteConfirm = false; viewModel.delete(onBack) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedPane(title: String, message: String?, onBack: () -> Unit, onUnlock: () -> Unit, modifier: Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(BrishInk), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = BrishYellow, modifier = Modifier.size(38.dp))
        }
        Text(title.ifBlank { "Nota bloqueada" }, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Text("Esta nota está protegida con tu huella o el bloqueo de tu teléfono.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
        Box(Modifier.fillMaxWidth().height(54.dp).clip(BrishShapes.pill).background(BrishYellow).bounceClick(onClick = onUnlock), contentAlignment = Alignment.Center) {
            Text("Desbloquear", fontWeight = FontWeight.ExtraBold, color = BrishInk)
        }
        Text("Volver", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onBack).padding(horizontal = 16.dp, vertical = 10.dp))
    }
}

@Composable
private fun SaveStatus(status: String, error: String?, onRetry: () -> Unit) {
    val failed = error != null || status == "No se pudo guardar"
    val saving = status == "Guardando"
    val recovered = status == "Borrador recuperado"
    val slidePx = with(LocalDensity.current) { 8.dp.roundToPx() }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedContent(
            targetState = when { failed -> 3; recovered -> 2; saving -> 1; else -> 0 },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "saveStatus"
        ) { s ->
            val (bg, fg, label) = when (s) {
                3 -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "Reintentar")
                2 -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "Borrador recuperado")
                1 -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Guardando…")
                else -> Triple(AreaHealthBg, AreaHealth, "Guardado") // el icono ✓ ya acompaña al texto
            }
            Row(
                Modifier.height(36.dp).clip(CircleShape).background(bg)
                    .then(if (s == 3) Modifier.clickable(role = Role.Button, onClick = onRetry) else Modifier)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (s) {
                    3 -> Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                    1 -> CircularProgressIndicator(Modifier.size(14.dp), color = fg, strokeWidth = 2.dp)
                    else -> Icon(Icons.Rounded.Check, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                }
                AnimatedContent(
                    targetState = label,
                    transitionSpec = {
                        (fadeIn(tween(MotionTokens.microMs)) +
                            slideInVertically(tween(MotionTokens.microMs)) { -slidePx }) togetherWith
                            fadeOut(tween(MotionTokens.microMs))
                    },
                    label = "saveStatusLabel"
                ) { text ->
                    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
                }
            }
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

private val ChecklistRowHeight = 56.dp

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    modifier: Modifier,
    handle: Modifier,
    enabled: Boolean,
    onToggle: () -> Unit,
    onText: (String) -> Unit,
    onRemove: () -> Unit
) {
    val box by animateColorAsState(if (item.checked) BrishYellow else Color.Transparent, BrishMotion.lively(), label = "box")
    val textColor by animateColorAsState(
        if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, label = "itemText"
    )
    val reduced = rememberReducedMotion()
    val scope = rememberCoroutineScope()
    val popScale = remember { Animatable(1f) }
    var burstTrigger by remember { mutableIntStateOf(0) }

    Row(modifier.fillMaxWidth().height(ChecklistRowHeight).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        BrishCelebrationBurst(trigger = burstTrigger) {
            Box(
                Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        scaleX = popScale.value
                        scaleY = popScale.value
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(box)
                    .border(2.dp, if (item.checked) BrishYellow else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Checkbox, enabled = enabled) {
                        if (!item.checked) {
                            burstTrigger++
                            if (!reduced) {
                                scope.launch {
                                    popScale.animateTo(1.26f, MotionTokens.overshoot())
                                    popScale.animateTo(1f, MotionTokens.pressSpring())
                                }
                            }
                        }
                        onToggle()
                    }
                    .semantics { stateDescription = if (item.checked) "Hecho" else "Pendiente" },
                contentAlignment = Alignment.Center
            ) {
                CheckMark(item.checked)
            }
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
            Icons.Rounded.DragHandle,
            contentDescription = "Arrastrar para reordenar ${item.text}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(36.dp).then(if (enabled) handle else Modifier).padding(6.dp)
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
    AnimatedVisibility(checked, enter = scaleIn(MotionTokens.overshoot()) + fadeIn(), exit = fadeOut()) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = BrishInk, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EditorToolbar(
    isList: Boolean,
    modifier: Modifier,
    onList: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean,
    isLocked: Boolean,
    hasDay: Boolean,
    hasFlashcards: Boolean = false,
    onPhoto: () -> Unit,
    onDay: () -> Unit,
    onFlashcards: () -> Unit = {},
    onLock: () -> Unit
) {
    val dark = isDarkTheme()
    Row(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .brishShadow(BrishShapes.bar, elevation = 18.dp, strength = if (dark) 0f else 1f)
            .clip(BrishShapes.bar)
            .background((if (dark) BrishNightCard else BrishCard).copy(alpha = 0.97f))
            .brishEdgeHighlight(alpha = if (dark) 0.06f else 0.9f, inset = 28.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ToolButton(Icons.AutoMirrored.Rounded.FormatListBulleted, if (isList) "Convertir en texto" else "Convertir en lista", active = isList, onClick = onList)
        ToolButton(Icons.Rounded.Image, "Añadir foto", active = false, onClick = onPhoto)
        ToolButton(Icons.Rounded.Check, "Tarjetas de repaso", active = hasFlashcards, onClick = onFlashcards)
        ToolButton(Icons.Rounded.CalendarMonth, if (hasDay) "Cambiar día enlazado" else "Enlazar a un día", active = hasDay, onClick = onDay)
        ToolButton(Icons.Rounded.Lock, if (isLocked) "Desbloquear nota" else "Bloquear con huella", active = isLocked, onClick = onLock)
        ToolButton(Icons.Rounded.Share, "Compartir", active = false, onClick = onShare)
        ToolButton(Icons.Rounded.DeleteOutline, "Mover a la papelera", active = false, enabled = canDelete, onClick = onDelete)
    }
}

@Composable
private fun FlashcardsEditorDialog(
    noteId: String,
    flashcards: List<com.katchy.focuslive.data.model.FlashcardItem>,
    onAdd: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val frontDraft = rememberPersistentDraftText("flashcard:$noteId:front")
    val backDraft = rememberPersistentDraftText("flashcard:$noteId:back")
    var front by frontDraft::value
    var back by backDraft::value
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestClose = {
        if (front.isNotBlank() || back.isNotBlank() || frontDraft.recovered || backDraft.recovered) {
            confirmDiscard = true
        } else {
            onDismiss()
        }
    }
    if (confirmDiscard) {
        SmoothDialog(onDismissRequest = { confirmDiscard = false }) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("¿Descartar tarjeta?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("Se descartará la pregunta o respuesta en edición.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton("Seguir editando", onClick = { confirmDiscard = false }, modifier = Modifier.weight(1f))
                    BrishPrimaryButton(
                        text = "Descartar",
                        onClick = {
                            frontDraft.discard()
                            backDraft.discard()
                            confirmDiscard = false
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    SmoothDialog(onDismissRequest = requestClose) {
        BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text("Tarjetas de repaso", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            if (frontDraft.recovered || backDraft.recovered) {
                DraftRecoveryNotice(if (frontDraft.recovered) frontDraft else backDraft, Modifier.fillMaxWidth())
            } else {
                DraftRecoveryNotice(frontDraft, Modifier.fillMaxWidth())
            }
            BrishTextField(
                value = front,
                onValueChange = { front = it },
                placeholder = "Ej. ¿Qué es el principio de Pareto?",
                label = "Pregunta (frente)"
            )
            BrishTextField(
                value = back,
                onValueChange = { back = it },
                placeholder = "Escribe la respuesta breve...",
                label = "Respuesta (reverso)",
                singleLine = false,
                minLines = 2,
                maxLines = 4
            )
            if (flashcards.isNotEmpty()) {
                Text("Tarjetas guardadas (${flashcards.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                flashcards.takeLast(4).forEach { card ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(card.question, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            Text(card.answer, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Eliminar tarjeta",
                            modifier = Modifier.size(22.dp).clip(CircleShape).clickable { onRemove(card.id) }.padding(3.dp)
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrishSecondaryButton("Cerrar", onClick = requestClose, modifier = Modifier.weight(1f))
                BrishPrimaryButton(
                    text = "Añadir tarjeta",
                    enabled = front.isNotBlank() && back.isNotBlank(),
                    onClick = {
                        onAdd(front.trim(), back.trim())
                        frontDraft.clearAfterSave()
                        backDraft.clearAfterSave()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, description: String, active: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val bg by animateColorAsState(if (active) BrishYellow else Color.Transparent, BrishMotion.soft(), label = "tool")
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(bg)
            .then(if (enabled) Modifier.bounceClick(onClick = onClick) else Modifier)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (active) BrishInk else MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f), modifier = Modifier.size(24.dp))
    }
}
