package com.katchy.focuslive.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.JsonParser
import com.katchy.focuslive.R
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
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val isHistoryDrawerOpen by viewModel.isHistoryDrawerOpen.collectAsState()
    val isMemoryDialogOpen by viewModel.isMemoryDialogOpen.collectAsState()
    val userMemory by viewModel.userAiMemory.collectAsState()
    val chatError by viewModel.chatError.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val isDark = isDarkTheme()
    var messageToExportTasks by remember { mutableStateOf<ChatMessage?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Sync drawer state with ViewModel
    LaunchedEffect(isHistoryDrawerOpen) {
        if (isHistoryDrawerOpen) drawerState.open() else drawerState.close()
    }
    LaunchedEffect(drawerState.isOpen) {
        if (!drawerState.isOpen && isHistoryDrawerOpen) {
            viewModel.closeHistoryDrawer()
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            kotlinx.coroutines.delay(50L)
            listState.animateScrollToItem(0)
        }
    }

    // Scroll to bottom FAB visibility
    val showScrollToBottom by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 1 }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = if (isDark) Color(0xFF14161C) else Color(0xFFF9FAFB),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(320.dp)
            ) {
                ChatHistoryDrawerContent(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    isDark = isDark,
                    onSelectSession = { session ->
                        viewModel.selectSession(session.id)
                        viewModel.closeHistoryDrawer()
                        coroutineScope.launch { drawerState.close() }
                    },
                    onNewChat = {
                        viewModel.startNewChat()
                        viewModel.closeHistoryDrawer()
                        coroutineScope.launch { drawerState.close() }
                    },
                    onDeleteSession = { viewModel.deleteSession(it) },
                    onOpenMemory = {
                        viewModel.closeHistoryDrawer()
                        coroutineScope.launch { drawerState.close() }
                        viewModel.openMemoryDialog()
                    },
                    onClose = {
                        viewModel.closeHistoryDrawer()
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                val activeSession = sessions.find { it.id == currentSessionId }
                val currentTitle = activeSession?.title ?: "Brish AI"

                BridgeChatTopBar(
                    title = currentTitle,
                    isGenerating = isGenerating,
                    onBack = onBack,
                    onOpenHistory = { viewModel.openHistoryDrawer() },
                    onNewChat = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.startNewChat()
                        InAppNotificationBus.post("Nuevo chat iniciado")
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 1. Chat Area (Empty State vs Message List)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty() && !isGenerating) {
                        AiWelcomeCanvas(
                            isDark = isDark,
                            onSuggestionClick = { prompt ->
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.sendMessage(prompt)
                            }
                        )
                    } else {
                        // Message List
                        LazyColumn(
                            state = listState,
                            reverseLayout = true,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (isGenerating) {
                                item {
                                    GeneratingBubble(isDark = isDark)
                                }
                            }

                            items(messages.reversed(), key = { it.id }) { message ->
                                BridgeMessageBubble(
                                    message = message,
                                    isDark = isDark,
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(message.text))
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        InAppNotificationBus.post("Texto copiado")
                                    },
                                    onExportNote = {
                                        viewModel.exportToNotes(message.text)
                                        InAppNotificationBus.post("Guardado en Notas")
                                    },
                                    onExportTasks = {
                                        messageToExportTasks = message
                                    },
                                    onExecuteActions = { msgId, payload ->
                                        viewModel.executeActions(msgId, payload)
                                        InAppNotificationBus.post("Aplicando cambios...")
                                    },
                                    onDismissActions = { msgId ->
                                        viewModel.dismissActions(msgId)
                                        InAppNotificationBus.post("Acciones descartadas")
                                    }
                                )
                            }
                        }
                    }

                    // Scroll-to-Bottom Floating Action Button
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showScrollToBottom,
                        enter = scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 12.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                coroutineScope.launch { listState.animateScrollToItem(0) }
                            },
                            shape = CircleShape,
                            containerColor = if (isDark) Color.White else Color(0xFF111827),
                            contentColor = if (isDark) Color.Black else Color.White,
                            modifier = Modifier
                                .size(42.dp)
                                .bounceClick()
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Bajar",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                if (chatError != null) {
                    ChatErrorCard(
                        message = chatError?.message.orEmpty(),
                        canRetry = !chatError?.failedPrompt.isNullOrBlank(),
                        isDark = isDark,
                        onRetry = viewModel::retryLastMessage,
                        onDismiss = viewModel::dismissError
                    )
                }

                // 2. Quick Suggestions Row (visible when chatting)
                if (messages.isNotEmpty()) {
                    val activeSuggestions = listOf(
                        "¿Qué tengo para hoy?",
                        "Planifica mi jornada",
                        "Resumen de mis finanzas",
                        "Mis hábitos activos"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeSuggestions) { suggestion ->
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    viewModel.sendMessage(suggestion)
                                },
                                shape = RoundedCornerShape(100.dp),
                                color = if (isDark) Color(0xFF1E2028) else Color(0xFFF3F4F6),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E7EB)
                                )
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Floating Input Bar
                ChatInputBar(
                    isGenerating = isGenerating,
                    isDark = isDark,
                    onSendMessage = { text ->
                        viewModel.sendMessage(text)
                    }
                )
            }
        }
    }

    // Long-Term Memory Dialog
    if (isMemoryDialogOpen) {
        AiMemoryDialog(
            memory = userMemory,
            isDark = isDark,
            onDismiss = { viewModel.closeMemoryDialog() },
            onAddFact = { viewModel.addKeyFact(it) },
            onDeleteFact = { viewModel.deleteKeyFact(it) },
            onClearMemory = { viewModel.clearMemory() }
        )
    }

    // Export Tasks Confirmation Dialog
    messageToExportTasks?.let { message ->
        val titles = remember(message.text) { extractTaskTitlesFromAiText(message.text) }
        ExportTasksConfirmationDialog(
            titles = titles,
            isDark = isDark,
            onDismiss = { messageToExportTasks = null },
            onConfirm = {
                viewModel.exportToTasks(message.text)
                messageToExportTasks = null
                InAppNotificationBus.post("${titles.size} tarea${if (titles.size == 1) "" else "s"} añadida${if (titles.size == 1) "" else "s"} a Agenda")
            }
        )
    }
}

@Composable
private fun ChatErrorCard(
    message: String,
    canRetry: Boolean,
    isDark: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF211E22) else Color(0xFFFFF7F6),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .16f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(Modifier.padding(start = 13.dp, end = 7.dp, top = 9.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Text(message, modifier = Modifier.weight(1f), fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            if (canRetry) {
                TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 7.dp)) {
                    Text("Reintentar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar error", modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun ExportTasksConfirmationDialog(
    titles: List<String>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SmoothDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF15171E) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
            modifier = Modifier.width(330.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(if (isDark) Color(0xFF1E212B) else Color(0xFFE8EEF8)), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.EventNote, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text("Añadir a Agenda", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("Revisa antes de guardar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (titles.isEmpty()) {
                    Text("Esta respuesta no contiene una lista clara de tareas. Pídele a Brish que la convierta en pasos concretos.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
                } else {
                    Text("Se crearán ${titles.size} tareas para hoy:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    titles.forEach { title ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(title, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onConfirm,
                    enabled = titles.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827), contentColor = if (isDark) Color(0xFF111827) else Color.White)
                ) { Text("Confirmar y añadir", fontWeight = FontWeight.Bold) }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancelar") }
            }
        }
    }
}

@Composable
fun BridgeChatTopBar(
    title: String,
    isGenerating: Boolean,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onNewChat: () -> Unit
) {
    val isDark = isDarkTheme()
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onOpenHistory, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Rounded.History,
                    contentDescription = "Historial",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Title & Status
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isGenerating) AntiPrimary.copy(alpha = glowAlpha) else Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isGenerating) "Preparando respuesta…" else "Tu asistente de estudio",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isGenerating) AntiPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // New Chat Button (Solid Circle)
            IconButton(
                onClick = onNewChat,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White else Color(0xFF111827))
                    .bounceClick()
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Nuevo Chat",
                    tint = if (isDark) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AiWelcomeCanvas(
    isDark: Boolean,
    onSuggestionClick: (String) -> Unit
) {
    val promptCards = listOf(
        Pair("Planifica mi día", "Organiza mis tareas y horarios de hoy"),
        Pair("Vaciado de pendientes", "Convierte notas sueltas en tareas"),
        Pair("Balance de finanzas", "Analiza mis ingresos y gastos"),
        Pair("Revisar hábitos", "Consulta mis rachas y progresos")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // AI Icon Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1C1E24) else Color(0xFFEEF2F6))
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = if (isDark) Color.White else Color(0xFF111827),
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Hola, soy Brish AI",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "¿En qué te puedo ayudar hoy a organizar o planificar?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 2x2 Suggestion Cards
        val chunkedCards = promptCards.chunked(2)
        chunkedCards.forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCards.forEach { (title, subtitle) ->
                    Surface(
                        onClick = { onSuggestionClick(title) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF15171E) else Color.White,
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                        modifier = Modifier
                            .weight(1f)
                            .height(115.dp)
                            .bounceClick()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ChatHistoryDrawerContent(
    sessions: List<ChatSession>,
    currentSessionId: String,
    isDark: Boolean,
    onSelectSession: (ChatSession) -> Unit,
    onNewChat: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenMemory: () -> Unit,
    onClose: () -> Unit
) {
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(18.dp)
    ) {
        // Drawer Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Conversaciones",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // New Chat Button
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .bounceClick(),
            shape = RoundedCornerShape(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color.White else Color(0xFF111827),
                contentColor = if (isDark) Color.Black else Color.White
            )
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Nuevo Chat", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Memory Button
        OutlinedButton(
            onClick = onOpenMemory,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .bounceClick(),
            shape = RoundedCornerShape(100.dp),
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.20f) else Color(0xFFD1D5DB))
        ) {
            Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Memoria de Brish AI", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(12.dp))

        // Grouped Sessions List
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aún no tienes conversaciones guardadas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val now = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000L
            val sevenDaysMs = 7 * oneDayMs

            val todaySessions = sessions.filter { (now - it.updatedAt) < oneDayMs }
            val weekSessions = sessions.filter { (now - it.updatedAt) in oneDayMs until sevenDaysMs }
            val olderSessions = sessions.filter { (now - it.updatedAt) >= sevenDaysMs }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (todaySessions.isNotEmpty()) {
                    item { SectionHeader("Hoy") }
                    items(todaySessions, key = { it.id }) { session ->
                        SessionItemRow(
                            session = session,
                            isSelected = session.id == currentSessionId,
                            isDark = isDark,
                            onClick = { onSelectSession(session) },
                            onDelete = { sessionToDelete = session }
                        )
                    }
                }

                if (weekSessions.isNotEmpty()) {
                    item { SectionHeader("Últimos 7 días") }
                    items(weekSessions, key = { it.id }) { session ->
                        SessionItemRow(
                            session = session,
                            isSelected = session.id == currentSessionId,
                            isDark = isDark,
                            onClick = { onSelectSession(session) },
                            onDelete = { sessionToDelete = session }
                        )
                    }
                }

                if (olderSessions.isNotEmpty()) {
                    item { SectionHeader("Anteriores") }
                    items(olderSessions, key = { it.id }) { session ->
                        SessionItemRow(
                            session = session,
                            isSelected = session.id == currentSessionId,
                            isDark = isDark,
                            onClick = { onSelectSession(session) },
                            onDelete = { sessionToDelete = session }
                        )
                    }
                }
            }
        }
    }

    sessionToDelete?.let { session ->
        SmoothDialog(onDismissRequest = { sessionToDelete = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "¿Eliminar conversación?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Se eliminará permanentemente \"${session.title}\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { sessionToDelete = null }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onDeleteSession(session.id)
                                sessionToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Eliminar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 4.dp)
    )
}

@Composable
fun SessionItemRow(
    session: ChatSession,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE5E7EB)
        } else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.ChatBubbleOutline,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (session.previewText.isNotBlank()) {
                    Text(
                        text = session.previewText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Eliminar chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun AiMemoryDialog(
    memory: UserAiMemory,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onAddFact: (String) -> Unit,
    onDeleteFact: (String) -> Unit,
    onClearMemory: () -> Unit
) {
    var newFactText by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    SmoothDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF1B1D22) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF262C38) else Color(0xFFE8EEF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Psychology, contentDescription = null, tint = if (isDark) Color.White else Color(0xFF111827), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Memoria de Brish",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "La IA recuerda hechos clave para personalizar sus respuestas y ayudarte mejor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Add Fact Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newFactText,
                        onValueChange = { newFactText = it },
                        placeholder = { Text("Ej: Examen de cálculo el viernes", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (newFactText.isNotBlank()) {
                                onAddFact(newFactText)
                                newFactText = ""
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDark) Color.White else Color(0xFF111827))
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Añadir", tint = if (isDark) Color.Black else Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // List of facts
                Text("Recuerdos aprendidos (${memory.keyFacts.size}):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                if (memory.keyFacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin recuerdos guardados aún. La IA irá aprendiendo conforme conversen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(memory.keyFacts) { fact ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF252830) else Color(0xFFF1F3F7),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• $fact",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 11.sp
                                    )
                                    IconButton(
                                        onClick = { onDeleteFact(fact) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (memory.keyFacts.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Limpiar toda la memoria", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        SmoothDialog(onDismissRequest = { showClearConfirm = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("¿Limpiar memoria?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("La IA olvidará los hechos aprendidos sobre ti.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showClearConfirm = false }) { Text("Cancelar") }
                        Button(
                            onClick = {
                                onClearMemory()
                                showClearConfirm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Olvidar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BridgeMessageBubble(
    message: ChatMessage,
    isDark: Boolean,
    onCopy: () -> Unit,
    onExportNote: () -> Unit,
    onExportTasks: () -> Unit,
    onExecuteActions: (String, String) -> Unit,
    onDismissActions: (String) -> Unit
) {
    val isUser = message.sender == "USER"
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) {
        if (message.timestamp > 0) timeFormat.format(Date(message.timestamp)) else ""
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E212B) else Color(0xFFE8EEF8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = if (isDark) Color.White else Color(0xFF111827),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) {
                    if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                } else {
                    if (isDark) Color(0xFF15171E) else Color.White
                },
                shadowElevation = if (isDark) 0.dp else 2.dp,
                border = if (!isUser) {
                    BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6))
                } else null,
                modifier = Modifier
                    .widthIn(max = 310.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp,
                            fontSize = 14.sp
                        ),
                        color = if (isUser) {
                            if (isDark) Color(0xFF111827) else Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (formattedTime.isNotBlank()) {
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (isUser) {
                                    if (isDark) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                }
                            )
                        }

                        if (!isUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Rounded.ContentCopy,
                                contentDescription = "Copiar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onCopy() }
                            )
                        }
                    }
                }
            }
        }

        if (!isUser) {
            val taskCount = remember(message.text) { extractTaskTitlesFromAiText(message.text).size }
            Row(
                modifier = Modifier.padding(start = 38.dp, top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TextButton(onClick = onExportNote, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(30.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Guardar nota", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                if (taskCount > 0) {
                    TextButton(onClick = onExportTasks, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(30.dp)) {
                        Icon(Icons.Rounded.EventAvailable, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Agenda ($taskCount)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Action Proposal Card (Agent Actions)
        if (!isUser && message.actionPayload != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AgentActionCard(
                message = message,
                payload = message.actionPayload,
                isDark = isDark,
                onExecute = { onExecuteActions(message.id, message.actionPayload) },
                onDismiss = { onDismissActions(message.id) }
            )
        }
    }
}

@Composable
fun AgentActionCard(
    message: ChatMessage,
    payload: String,
    isDark: Boolean,
    onExecute: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val parsedActions = remember(payload) {
        try {
            val json = JsonParser.parseString(payload).asJsonObject
            val array = json.getAsJsonArray("actions")
            array?.map { it.asJsonObject } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    if (parsedActions.isEmpty()) return

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF15171E) else Color.White,
        shadowElevation = if (isDark) 0.dp else 2.dp,
        border = BorderStroke(
            1.dp,
            if (message.actionsExecuted) Color(0xFF10B981).copy(alpha = 0.4f)
            else if (message.actionsDismissed) Color(0xFFEF4444).copy(alpha = 0.3f)
            else if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 38.dp, end = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (message.actionsExecuted) Icons.Rounded.CheckCircle
                    else if (message.actionsDismissed) Icons.Rounded.Cancel
                    else Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = if (message.actionsExecuted) Color(0xFF10B981)
                    else if (message.actionsDismissed) Color(0xFFEF4444)
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (message.actionsExecuted) "Acciones aplicadas con éxito"
                    else if (message.actionsDismissed) "Acciones canceladas"
                    else "Acción propuesta por Brish AI",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (message.actionsExecuted) Color(0xFF10B981)
                    else if (message.actionsDismissed) Color(0xFFEF4444)
                    else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            parsedActions.forEach { action ->
                val type = action.get("type")?.asString ?: ""
                val title = action.get("title")?.asString ?: ""
                val time = if (action.has("time") && !action.get("time").isJsonNull) action.get("time").asString else ""

                val label = when (type) {
                    "CREATE_TASK" -> "Tarea: $title ${if (time.isNotBlank()) "($time)" else ""}".trim()
                    "UPDATE_TASK" -> "Modificar: $title"
                    "DELETE_TASK" -> "Eliminar tarea"
                    "CREATE_HABIT" -> "Hábito: $title"
                    "DELETE_HABIT" -> "Eliminar hábito"
                    "CREATE_NOTE" -> "Nota: $title"
                    "SET_POMODORO" -> "Ajustar Pomodoro"
                    "CREATE_TRANSACTION" -> "Transacción: $title"
                    else -> type
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            if (!message.actionsExecuted && !message.actionsDismissed) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.20f) else Color(0xFFD1D5DB)),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Descartar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onExecute()
                        },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color.White else Color(0xFF111827),
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        modifier = Modifier.weight(1.3f).height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aplicar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratingBubble(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "generating")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1E212B) else Color(0xFFE8EEF8)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = if (isDark) Color.White else Color(0xFF111827),
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF15171E) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
            modifier = Modifier.widthIn(max = 220.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Brish está pensando...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    isGenerating: Boolean,
    isDark: Boolean,
    onSendMessage: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        "Pregunta o pide organizar algo...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 120.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDark) Color.White else Color(0xFF111827),
                    unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE5E7EB),
                    focusedContainerColor = if (isDark) Color(0xFF15171E) else Color.White,
                    unfocusedContainerColor = if (isDark) Color(0xFF15171E) else Color.White
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && !isGenerating) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onSendMessage(inputText)
                            inputText = ""
                            focusManager.clearFocus()
                        }
                    }
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isGenerating) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onSendMessage(inputText)
                        inputText = ""
                        focusManager.clearFocus()
                    }
                },
                enabled = inputText.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isGenerating) {
                            if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                        } else {
                            if (isDark) Color(0xFF1E212B) else Color(0xFFE5E7EB)
                        }
                    )
                    .bounceClick()
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Enviar",
                    tint = if (inputText.isNotBlank() && !isGenerating) {
                        if (isDark) Color(0xFF111827) else Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
