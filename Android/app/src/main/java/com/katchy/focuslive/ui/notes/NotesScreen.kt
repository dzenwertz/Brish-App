package com.katchy.focuslive.ui.notes

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.NoteCategory
import com.katchy.focuslive.ui.components.BrishTopHeader
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.theme.AntiPrimary
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.InAppNotificationBus
import com.katchy.focuslive.ui.util.bounceClick
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onNavigateToChat: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToStudy: (String) -> Unit = {},
    viewModel: NotesViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val trashNotes by viewModel.trashNotes.collectAsState()
    val isAiProcessing by viewModel.isAiProcessing.collectAsState()

    var showAddNoteSheet by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showTrashSheet by remember { mutableStateOf(false) }
    var folderToManage by remember { mutableStateOf<NoteCategory?>(null) }
    var showEditFolderDialog by remember { mutableStateOf(false) }
    var showDeleteFolderConfirm by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var editedFolderName by remember { mutableStateOf("") }
    var selectedFolderCategory by remember { mutableStateOf<NoteCategory?>(null) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var noteToView by remember { mutableStateOf<Note?>(null) }

    // Draft states for new note
    var draftNoteTitle by rememberSaveable { mutableStateOf("") }
    var draftNoteContent by rememberSaveable { mutableStateOf("") }
    var draftNoteCategoryId by rememberSaveable { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val trashSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isDarkTheme()
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    // Default categories if empty, ensuring uncategorized notes have a home
    val displayCategories = remember(categories, notes) {
        val baseList = if (categories.isNotEmpty()) {
            categories.toMutableList()
        } else {
            mutableListOf(
                NoteCategory(id = "ideas", name = "Ideas", isDefault = true),
                NoteCategory(id = "proyectos", name = "Proyectos", isDefault = true),
                NoteCategory(id = "finanzas", name = "Finanzas", isDefault = true),
                NoteCategory(id = "comida", name = "Comida", isDefault = true)
            )
        }
        val knownCategoryIds = baseList.map { it.id }.toSet()
        val hasUncategorized = notes.any { it.categoryId == null || it.categoryId !in knownCategoryIds }
        if (hasUncategorized && baseList.none { it.id == "uncategorized" }) {
            baseList.add(0, NoteCategory(id = "uncategorized", name = "General", isDefault = true))
        }
        baseList
    }

    BackHandler(enabled = noteToView != null || noteToEdit != null || showAddNoteSheet || showAddCategoryDialog || showTrashSheet || folderToManage != null || showEditFolderDialog || showDeleteFolderConfirm || selectedFolderCategory != null) {
        when {
            noteToView != null -> noteToView = null
            noteToEdit != null -> noteToEdit = null
            showAddNoteSheet -> showAddNoteSheet = false
            showAddCategoryDialog -> showAddCategoryDialog = false
            showTrashSheet -> showTrashSheet = false
            showEditFolderDialog -> showEditFolderDialog = false
            showDeleteFolderConfirm -> showDeleteFolderConfirm = false
            folderToManage != null -> folderToManage = null
            selectedFolderCategory != null -> selectedFolderCategory = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (selectedFolderCategory != null) {
                BrishTopHeader(
                    title = selectedFolderCategory?.name ?: "Notas",
                    onBack = { selectedFolderCategory = null },
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToChat = onNavigateToChat,
                    actions = {
                        IconButton(
                            onClick = {
                                folderToManage = selectedFolderCategory
                                editedFolderName = selectedFolderCategory?.name ?: ""
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6))
                                .bounceClick()
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "Opciones de carpeta",
                                tint = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            } else {
                BrishTopHeader(
                    title = "Notas",
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToChat = onNavigateToChat
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    showAddNoteSheet = true
                },
                containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                contentColor = if (isDark) Color(0xFF111827) else Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .size(54.dp)
                    .bounceClick()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Nueva Nota", modifier = Modifier.size(24.dp))
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedFolderCategory,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(
                        initialOffsetX = { it / 3 },
                        animationSpec = tween(350, easing = EaseOutQuart)
                    ) + fadeIn(animationSpec = tween(300, easing = EaseOutQuart)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { -it / 4 },
                                animationSpec = tween(300, easing = EaseOutQuart)
                            ) + fadeOut(animationSpec = tween(200, easing = EaseOutQuart))
                        )
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { -it / 4 },
                        animationSpec = tween(350, easing = EaseOutQuart)
                    ) + fadeIn(animationSpec = tween(300, easing = EaseOutQuart)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { it / 3 },
                                animationSpec = tween(300, easing = EaseOutQuart)
                            ) + fadeOut(animationSpec = tween(200, easing = EaseOutQuart))
                        )
                }
            },
            label = "folder_transition"
        ) { folder ->
            if (folder != null) {
                // Folder Detail View (when user opens a folder)
                val knownCategoryIds = remember(displayCategories) { displayCategories.filter { it.id != "uncategorized" && it.id != "all_notes" }.map { it.id }.toSet() }
                val folderNotes = remember(folder, notes, knownCategoryIds) {
                    when (folder.id) {
                        "all_notes" -> notes
                        "uncategorized" -> notes.filter { it.categoryId == null || it.categoryId !in knownCategoryIds }
                        else -> notes.filter { it.categoryId == folder.id }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    if (folderNotes.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "No hay notas en esta carpeta aún.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            items(folderNotes, key = { it.id }) { note ->
                                FolderNoteListItem(
                                    note = note,
                                    categoryName = folder.name,
                                    isDark = isDark,
                                    onClick = { noteToView = note },
                                    onLongClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        noteToEdit = note
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Main Notes Screen (Image 2 Reference)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. "Recent Notes" Section (Horizontal Carousel)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notas recientes",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (notes.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        selectedFolderCategory = NoteCategory(id = "all_notes", name = "Todas las notas", isDefault = true)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Ver todas (${notes.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        if (notes.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDark) Color(0xFF15171E) else Color.White,
                                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                                modifier = Modifier.fillMaxWidth()
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
                                        Icon(
                                            Icons.Rounded.EditNote,
                                            contentDescription = null,
                                            tint = if (isDark) Color.White else Color(0xFF111827),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Tu libreta está lista",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Captura ideas, notas rápidas, resúmenes o proyectos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { showAddNoteSheet = true },
                                        shape = RoundedCornerShape(100.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                                            contentColor = if (isDark) Color(0xFF111827) else Color.White
                                        ),
                                        modifier = Modifier.height(36.dp).bounceClick()
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Escribir Nota", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(notes.take(6), key = { it.id }) { note ->
                                    val catName = displayCategories.find { it.id == note.categoryId }?.name ?: "General"
                                    RecentNoteCard(
                                        note = note,
                                        categoryName = catName,
                                        isDark = isDark,
                                        onClick = { noteToView = note },
                                        onLongClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            noteToEdit = note
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. "Folders" Section (2-Column Grid)
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Carpetas",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // AI Organize Button
                                if (notes.size >= 2) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            InAppNotificationBus.post("Organizando notas con IA...")
                                            viewModel.organizeNotesWithAI(
                                                onSuccess = {
                                                    InAppNotificationBus.post("¡Notas organizadas en carpetas!")
                                                },
                                                onError = { err ->
                                                    val msg = if (err == "Premium required") "Se requiere Brish Premium para organizar notas con IA." else err
                                                    InAppNotificationBus.post(msg)
                                                }
                                            )
                                        },
                                        enabled = !isAiProcessing,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        if (isAiProcessing) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(
                                                Icons.Rounded.AutoAwesome,
                                                contentDescription = "Organizar notas con IA",
                                                modifier = Modifier.size(20.dp),
                                                tint = Color(0xFF06B6D4)
                                            )
                                        }
                                    }
                                }

                                // Trash Button with badge
                                IconButton(
                                    onClick = { showTrashSheet = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (trashNotes.isNotEmpty()) {
                                                Badge(
                                                    containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                                                ) {
                                                    Text(
                                                        "${trashNotes.size}",
                                                        color = if (isDark) Color(0xFF111827) else Color.White,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Papelera",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                TextButton(
                                    onClick = { showAddCategoryDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Nueva", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        // 2-Column Grid
                        val chunkedFolders = displayCategories.chunked(2)
                        chunkedFolders.forEach { rowFolders ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                rowFolders.forEach { folder ->
                                    val count = if (folder.id == "uncategorized") {
                                        val knownCategoryIds = displayCategories.filter { it.id != "uncategorized" }.map { it.id }.toSet()
                                        notes.count { it.categoryId == null || it.categoryId !in knownCategoryIds }
                                    } else {
                                        notes.count { it.categoryId == folder.id }
                                    }
                                    FolderCard(
                                        category = folder,
                                        noteCount = count,
                                        isDark = isDark,
                                        onClick = { selectedFolderCategory = folder },
                                        onLongClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            folderToManage = folder
                                            editedFolderName = folder.name
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowFolders.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp)) // space for bottom nav
                }
            }
        }
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        SmoothDialog(onDismissRequest = { showAddCategoryDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Nueva Carpeta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nombre de la carpeta") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddCategoryDialog = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newCategoryName.isNotBlank()) {
                                    viewModel.addCategory(newCategoryName.trim())
                                    newCategoryName = ""
                                    showAddCategoryDialog = false
                                    InAppNotificationBus.post("Carpeta creada")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                                contentColor = if (isDark) Color(0xFF111827) else Color.White
                            ),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Crear", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Manage Folder (Options: Rename or Delete) Dialog
    folderToManage?.let { folder ->
        SmoothDialog(onDismissRequest = { folderToManage = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
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
                        Icon(
                            Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = if (isDark) Color.White else Color(0xFF111827),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Elige una acción para esta carpeta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Rename button
                    Button(
                        onClick = {
                            editedFolderName = folder.name
                            showEditFolderDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Renombrar Carpeta", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Delete button
                    Button(
                        onClick = {
                            showDeleteFolderConfirm = true
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eliminar Carpeta", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Rename Folder Dialog
    if (showEditFolderDialog && folderToManage != null) {
        val folder = folderToManage!!
        SmoothDialog(onDismissRequest = { showEditFolderDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Renombrar Carpeta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = editedFolderName,
                        onValueChange = { editedFolderName = it },
                        label = { Text("Nuevo nombre") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditFolderDialog = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (editedFolderName.isNotBlank()) {
                                    viewModel.updateCategory(folder, editedFolderName)
                                    showEditFolderDialog = false
                                    folderToManage = null
                                    InAppNotificationBus.post("Carpeta renombrada")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color.White else Color(0xFF111827),
                                contentColor = if (isDark) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Confirm Delete Folder Dialog
    if (showDeleteFolderConfirm && folderToManage != null) {
        val folder = folderToManage!!
        SmoothDialog(onDismissRequest = { showDeleteFolderConfirm = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "¿Eliminar carpeta?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "La carpeta \"${folder.name}\" se eliminará. Las notas dentro de ella no se borrarán, quedarán disponibles en la lista general.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDeleteFolderConfirm = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.deleteCategory(folder)
                                if (selectedFolderCategory?.id == folder.id) {
                                    selectedFolderCategory = null
                                }
                                showDeleteFolderConfirm = false
                                folderToManage = null
                                InAppNotificationBus.post("Carpeta eliminada")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Eliminar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Trash Bottom Sheet (Papelera)
    if (showTrashSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTrashSheet = false },
            sheetState = trashSheetState,
            containerColor = if (isDark) Color(0xFF15171E) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Papelera de Notas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${trashNotes.size} ${if (trashNotes.size == 1) "nota eliminada" else "notas eliminadas"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    if (trashNotes.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.emptyTrash()
                                InAppNotificationBus.post("Papelera vaciada")
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Vaciar todo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (trashNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "La papelera está vacía",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Las notas que elimines aparecerán aquí para que puedas recuperarlas cuando quieras.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(trashNotes, key = { it.id }) { note ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) Color(0xFF1E212B) else Color(0xFFF9FAFB),
                                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFE5E7EB)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title.ifBlank { "Sin título" },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (note.content.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = note.content,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Restore button
                                    IconButton(
                                        onClick = {
                                            viewModel.restoreFromTrash(note)
                                            InAppNotificationBus.post("Nota restaurada")
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Restore,
                                            contentDescription = "Restaurar",
                                            tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Delete forever button
                                    IconButton(
                                        onClick = {
                                            viewModel.deletePermanentlyFromTrash(note.id)
                                            InAppNotificationBus.post("Eliminada definitivamente")
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteForever,
                                            contentDescription = "Eliminar permanentemente",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Note Sheet
    if (showAddNoteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddNoteSheet = false },
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
                    text = "Nueva Nota",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = draftNoteTitle,
                    onValueChange = { draftNoteTitle = it },
                    label = { Text("Título de la nota") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = draftNoteContent,
                    onValueChange = { draftNoteContent = it },
                    label = { Text("Contenido / Apuntes") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Carpeta",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayCategories) { folder ->
                        FilterChip(
                            selected = draftNoteCategoryId == folder.id,
                            onClick = { draftNoteCategoryId = folder.id },
                            label = { Text(folder.name, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    enabled = draftNoteTitle.isNotBlank(),
                    onClick = {
                        if (draftNoteTitle.isNotBlank()) {
                            viewModel.addNote(
                                title = draftNoteTitle.trim(),
                                content = draftNoteContent.trim(),
                                colorHex = "#FFF9C4",
                                isPinned = false,
                                priority = "LOW",
                                categoryId = draftNoteCategoryId
                            )
                            draftNoteTitle = ""
                            draftNoteContent = ""
                            showAddNoteSheet = false
                            InAppNotificationBus.post("Nota guardada")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    )
                ) {
                    Text("Guardar nota", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Edit Note Sheet
    noteToEdit?.let { note ->
        var editTitle by remember { mutableStateOf(note.title) }
        var editContent by remember { mutableStateOf(note.content) }
        var editCategoryId by remember { mutableStateOf(note.categoryId) }

        ModalBottomSheet(
            onDismissRequest = { noteToEdit = null },
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
                    Text(
                        text = "Editar Nota",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val deletedNote = note
                                viewModel.deleteNote(deletedNote.id)
                                noteToEdit = null
                                InAppNotificationBus.post(
                                    message = "Nota eliminada",
                                    actionLabel = "DESHACER",
                                    onAction = { viewModel.restoreNote(deletedNote) }
                                )
                            }
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { noteToEdit = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    label = { Text("Contenido") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Carpeta",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayCategories) { folder ->
                        FilterChip(
                            selected = editCategoryId == folder.id,
                            onClick = { editCategoryId = folder.id },
                            label = { Text(folder.name, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.updateNote(
                            note.copy(
                                title = editTitle.trim(),
                                content = editContent.trim(),
                                categoryId = editCategoryId
                            )
                        )
                        noteToEdit = null
                        InAppNotificationBus.post("Nota actualizada")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    )
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Full Reading Mode Sheet
    noteToView?.let { note ->
        val catName = displayCategories.find { it.id == note.categoryId }?.name ?: "General"
        NoteDetailViewerSheet(
            note = note,
            categoryName = catName,
            isDark = isDark,
            isAiProcessing = isAiProcessing,
            onEnhanceWithAI = {
                viewModel.enhanceNoteWithAI(
                    note = note,
                    onSuccess = { updated ->
                        noteToView = updated
                        InAppNotificationBus.post("Nota mejorada con IA")
                    },
                    onError = { err ->
                        InAppNotificationBus.post(err)
                    }
                )
            },
            onEdit = {
                val current = noteToView
                noteToView = null
                noteToEdit = current
            },
            onDelete = {
                val deletedNote = note
                viewModel.deleteNote(deletedNote.id)
                noteToView = null
                InAppNotificationBus.post(
                    message = "Nota eliminada",
                    actionLabel = "DESHACER",
                    onAction = { viewModel.restoreNote(deletedNote) }
                )
            },
            onDismiss = { noteToView = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailViewerSheet(
    note: Note,
    categoryName: String,
    isDark: Boolean,
    isAiProcessing: Boolean,
    onEnhanceWithAI: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeFormatted = remember(note.timestamp) {
        val sdf = SimpleDateFormat("dd 'de' MMMM 'de' yyyy • hh:mm a", Locale("es", "ES"))
        sdf.format(Date(note.timestamp))
    }

    val wordCount = remember(note.content) {
        if (note.content.isBlank()) 0 else note.content.trim().split(Regex("\\s+")).size
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF15171E) else Color.White,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                color = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f),
                shape = RoundedCornerShape(100.dp)
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            // Action Header Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Pill
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE8EEF8)
                ) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color.White else Color(0xFF111827),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Enhance with AI Button
                    Button(
                        onClick = onEnhanceWithAI,
                        enabled = !isAiProcessing,
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF1E212B) else Color(0xFFF0F4FC),
                            contentColor = if (isDark) Color.White else Color(0xFF111827)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        if (isAiProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = if (isDark) Color.White else Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mejorando...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isDark) Color.White else Color(0xFF111827))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mejorar con IA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Edit Button
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }

                    // Delete Button
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date and Word count info
            Text(
                text = "$timeFormatted • $wordCount palabras",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Note Title
            Text(
                text = note.title.ifBlank { "Sin título" },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF0F2F6))
            Spacer(modifier = Modifier.height(14.dp))

            // Full Note Content (Scrollable & Selectable)
            SelectionContainer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = note.content.ifBlank { "Esta nota está vacía." },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            lineHeight = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentNoteCard(
    note: Note,
    categoryName: String,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val tagColor = remember(categoryName) {
        when {
            categoryName.contains("Finanza", ignoreCase = true) -> Color(0xFFE0E7FF) to Color(0xFF4F46E5)
            categoryName.contains("Proyecto", ignoreCase = true) -> Color(0xFFD1FAE5) to Color(0xFF059669)
            categoryName.contains("Idea", ignoreCase = true) -> Color(0xFFFEF3C7) to Color(0xFFD97706)
            else -> Color(0xFFE8EEF8) to Color(0xFF2563EB)
        }
    }

    val timeFormatted = remember(note.timestamp) {
        val diff = System.currentTimeMillis() - note.timestamp
        val hours = diff / (1000 * 60 * 60)
        if (hours > 0) "Hace ${hours}h" else "Hace un momento"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF15171E) else Color.White,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
        modifier = Modifier
            .width(220.dp)
            .height(132.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .bounceClick()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Tag & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (isDark) tagColor.second.copy(alpha = 0.2f) else tagColor.first
                ) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) tagColor.second else tagColor.second,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }

            // Title & Content
            Column {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = note.content.ifBlank { "Sin contenido adicional..." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderNoteListItem(
    note: Note,
    categoryName: String,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val timeFormatted = remember(note.timestamp) {
        val diff = System.currentTimeMillis() - note.timestamp
        val hours = diff / (1000 * 60 * 60)
        if (hours > 0) "Hace ${hours}h" else "Hace un momento"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF15171E) else Color.White,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .bounceClick()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifBlank { "Sin título" },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    category: NoteCategory,
    noteCount: Int,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val folderBgTint = remember(category.name) {
        when {
            category.name.contains("Comida", ignoreCase = true) -> Color(0xFFDBEAFE) to Color(0xFF2563EB)
            category.name.contains("Proyecto", ignoreCase = true) -> Color(0xFFD1FAE5) to Color(0xFF10B981)
            category.name.contains("Idea", ignoreCase = true) -> Color(0xFFE5E7EB) to Color(0xFF4B5563)
            category.name.contains("Finanza", ignoreCase = true) -> Color(0xFFEDE9FE) to Color(0xFF7C3AED)
            else -> Color(0xFFE0F2FE) to Color(0xFF0284C7)
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF15171E) else Color.White,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
        modifier = modifier
            .height(138.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .bounceClick()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large circular icon box
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isDark) folderBgTint.second.copy(alpha = 0.2f) else folderBgTint.first),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    category.name.contains("Comida", ignoreCase = true) -> Icons.Rounded.Restaurant
                    category.name.contains("Proyecto", ignoreCase = true) -> Icons.Rounded.RocketLaunch
                    category.name.contains("Idea", ignoreCase = true) -> Icons.Rounded.Lightbulb
                    category.name.contains("Finanza", ignoreCase = true) -> Icons.Rounded.AccountBalanceWallet
                    else -> Icons.Rounded.Folder
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDark) Color.White else folderBgTint.second,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$noteCount notas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
