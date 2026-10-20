package com.katchy.focuslive.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katchy.focuslive.ui.components.BrishTopHeader

@Composable
fun NoteEditorScreen(
    onBack: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val folders by viewModel.categories.collectAsState()
    val courses by viewModel.courses.collectAsState()
    var folderMenu by remember { mutableStateOf(false) }
    var courseMenu by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.flush()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    BackHandler { viewModel.close(onBack) }

    Scaffold(
        topBar = {
            BrishTopHeader(
                title = "Apunte",
                onBack = { viewModel.close(onBack) },
                showSettings = false,
                actions = {
                    IconButton(
                        onClick = { deleteConfirm = true },
                        enabled = !state.loading && !state.closing && (state.note.title.isNotBlank() || state.note.content.isNotBlank())
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, "Mover a la papelera")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SaveStatusCard(status = state.status, error = state.error)

                if (state.error != null && !state.unavailable) {
                    FilledTonalButton(
                        onClick = viewModel::retry,
                        enabled = !state.closing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reintentar guardado")
                    }
                }

                if (state.unavailable) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("No se puede editar este apunte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(state.error ?: "El apunte no está disponible.", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Organización", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "La carpeta y el curso son independientes: puedes usar ambos al mismo tiempo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.weight(1f)) {
                                    FilledTonalButton(
                                        onClick = { folderMenu = true },
                                        enabled = !state.closing,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(15.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                                    ) {
                                        Icon(Icons.Rounded.Folder, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(7.dp))
                                        Text(
                                            folders.find { it.id == state.note.categoryId }?.name ?: "Sin carpeta",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Sin carpeta") },
                                            onClick = { viewModel.change { it.copy(categoryId = null) }; folderMenu = false },
                                            leadingIcon = { Icon(Icons.Rounded.Description, null) }
                                        )
                                        folders.forEach { folder ->
                                            DropdownMenuItem(
                                                text = { Text(folder.name) },
                                                onClick = { viewModel.change { it.copy(categoryId = folder.id) }; folderMenu = false },
                                                leadingIcon = { Icon(Icons.Rounded.Folder, null) }
                                            )
                                        }
                                    }
                                }

                                Box(Modifier.weight(1f)) {
                                    FilledTonalButton(
                                        onClick = { courseMenu = true },
                                        enabled = !state.closing,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(15.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                                    ) {
                                        Icon(Icons.Rounded.School, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(7.dp))
                                        Text(
                                            courses.find { it.id == state.note.courseId }?.name ?: "Sin curso",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    DropdownMenu(expanded = courseMenu, onDismissRequest = { courseMenu = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Sin curso") },
                                            onClick = { viewModel.change { it.copy(courseId = null) }; courseMenu = false },
                                            leadingIcon = { Icon(Icons.Rounded.Description, null) }
                                        )
                                        courses.forEach { course ->
                                            DropdownMenuItem(
                                                text = { Text(course.name) },
                                                onClick = { viewModel.change { it.copy(courseId = course.id) }; courseMenu = false },
                                                leadingIcon = { Icon(Icons.Rounded.School, null) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.note.title,
                        onValueChange = { title -> viewModel.change { it.copy(title = title) } },
                        placeholder = { Text("Título") },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.closing,
                        shape = RoundedCornerShape(20.dp),
                        singleLine = false,
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = state.note.content,
                        onValueChange = { text -> viewModel.change { it.copy(content = text) } },
                        placeholder = { Text("Escribe aquí. Brish guarda tus cambios automáticamente…") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp),
                        enabled = !state.closing,
                        shape = RoundedCornerShape(22.dp),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Mover a la papelera") },
            text = { Text("Podrás recuperar este apunte desde Notas → Papelera.") },
            confirmButton = {
                TextButton(onClick = { deleteConfirm = false; viewModel.delete(onBack) }) {
                    Text("Mover")
                }
            },
            dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SaveStatusCard(status: String, error: String?) {
    val failed = error != null || status == "No se pudo guardar"
    val saving = status == "Guardando"
    val icon = when {
        failed -> Icons.Rounded.ErrorOutline
        saving -> Icons.Rounded.Sync
        else -> Icons.Rounded.CheckCircle
    }
    val container = if (failed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer
    val content = if (failed) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = container) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(icon, null, tint = content, modifier = Modifier.size(18.dp))
            AnimatedContent(
                targetState = if (failed) (error ?: status) else status,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = .98f)) togetherWith (fadeOut() + scaleOut(targetScale = 1.02f)) },
                label = "save-status"
            ) { label ->
                Text(label.ifBlank { "Autoguardado activado" }, style = MaterialTheme.typography.labelLarge, color = content)
            }
            Spacer(Modifier.weight(1f))
            if (saving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        }
    }
}
