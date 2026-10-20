package com.katchy.focuslive.ui.focus

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katchy.focuslive.data.manager.TimerManager
import com.katchy.focuslive.ui.components.BrishTopHeader
import com.katchy.focuslive.ui.components.brishMutedPanelColor
import com.katchy.focuslive.ui.components.brishPanelBorder
import com.katchy.focuslive.ui.components.brishPanelColor
import com.katchy.focuslive.ui.components.brishPrimaryActionColor
import com.katchy.focuslive.ui.components.brishPrimaryActionContentColor
import com.katchy.focuslive.ui.home.HomeViewModel
import com.katchy.focuslive.ui.home.formatTimer

@Composable
fun FocusSessionScreen(
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val remaining by viewModel.timeLeft.collectAsState()
    val running by viewModel.isTimerRunning.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val work by viewModel.workDuration.collectAsState()
    val rest by viewModel.breakDuration.collectAsState()
    val zen by viewModel.isZenModeEnabled.collectAsState()
    val sound by viewModel.ambientSound.collectAsState()
    val customSoundUri by viewModel.customSoundUri.collectAsState()
    val activeTaskId by viewModel.activeTaskId.collectAsState()
    val homeState by viewModel.homeState.collectAsState()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var requestDnd by remember { mutableStateOf(false) }

    val customSoundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.setCustomSoundUri(uri.toString())
            viewModel.setAmbientSound(TimerManager.AmbientSound.CUSTOM)
        }
    }

    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        owner.lifecycle.addObserver(observer)
        viewModel.onResume()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.zenModeEffect.collect { requestDnd = true }
    }

    val sessionMinutes = if (timerState == TimerManager.TimerState.WORK) work else rest
    val totalSeconds = (sessionMinutes * 60L).coerceAtLeast(1L)
    val progress = (1f - remaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val activeTask = homeState.focusTasks.firstOrNull { it.id == activeTaskId }
    val accent = Color(0xFF06B6D4)

    Column(Modifier.fillMaxSize()) {
        BrishTopHeader(title = "Enfoque", onBack = onNavigateBack, showSettings = false)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = brishPanelColor(),
                border = brishPanelBorder(),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(if (timerState == TimerManager.TimerState.WORK) "ENFOQUE" else "DESCANSO") },
                        leadingIcon = {
                            Icon(if (timerState == TimerManager.TimerState.WORK) Icons.Rounded.CenterFocusStrong else Icons.Rounded.Coffee, null)
                        }
                    )

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp,
                            color = accent,
                            trackColor = brishMutedPanelColor()
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedContent(
                                targetState = remaining,
                                transitionSpec = {
                                    (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith
                                        (fadeOut() + scaleOut(targetScale = 1.04f))
                                },
                                label = "timer"
                            ) { seconds ->
                                Text(formatTimer(seconds), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                activeTask?.title ?: if (timerState == TimerManager.TimerState.WORK) "Una cosa a la vez" else "Recarga un momento",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = viewModel::toggleTimer,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brishPrimaryActionColor(),
                            contentColor = brishPrimaryActionContentColor()
                        )
                    ) {
                        Icon(if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (running) "Pausar" else "Iniciar", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = viewModel::resetTimer, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Rounded.Refresh, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Reiniciar")
                        }
                        FilledTonalButton(onClick = viewModel::skipSession, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Rounded.SkipNext, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Siguiente")
                        }
                    }
                }
            }

            FocusSettingsCard(title = "Duración", icon = Icons.Rounded.Schedule) {
                DurationControl("Enfoque", work, !running, viewModel::setWorkDuration)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DurationControl("Descanso", rest, !running, viewModel::setBreakDuration)
            }

            FocusSettingsCard(title = "Tarea de la sesión", icon = Icons.Rounded.CheckCircleOutline) {
                FilterChip(
                    selected = activeTaskId == null,
                    onClick = { viewModel.setActiveTask(null) },
                    label = { Text("Sin tarea") }
                )
                homeState.focusTasks.forEach { task ->
                    FilterChip(
                        selected = activeTaskId == task.id,
                        onClick = { viewModel.setActiveTask(task.id) },
                        label = { Text(task.title, maxLines = 1) }
                    )
                }
            }

            FocusSettingsCard(title = "Ambiente", icon = Icons.Rounded.Headphones) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("No molestar", fontWeight = FontWeight.Medium)
                        Text("Reduce interrupciones durante la sesión", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = zen, onCheckedChange = { viewModel.toggleZenMode() })
                }

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimerManager.AmbientSound.entries.filter { it != TimerManager.AmbientSound.CUSTOM }.forEach { choice ->
                        FilterChip(
                            selected = sound == choice,
                            onClick = { viewModel.setAmbientSound(choice) },
                            label = { Text(soundLabel(choice)) }
                        )
                    }
                    if (customSoundUri != null) {
                        FilterChip(
                            selected = sound == TimerManager.AmbientSound.CUSTOM,
                            onClick = { viewModel.setAmbientSound(TimerManager.AmbientSound.CUSTOM) },
                            label = { Text("Mi sonido") }
                        )
                    }
                }

                OutlinedButton(
                    onClick = { customSoundPicker.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.LibraryMusic, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Elegir sonido personal")
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    if (requestDnd) {
        AlertDialog(
            onDismissRequest = { requestDnd = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Permitir No molestar") },
            text = { Text("Activa el acceso de Brish a No molestar para silenciar interrupciones durante el enfoque.") },
            confirmButton = {
                TextButton(onClick = {
                    requestDnd = false
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }) { Text("Abrir ajustes") }
            },
            dismissButton = { TextButton(onClick = { requestDnd = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun FocusSettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = brishPanelColor(),
        border = brishPanelBorder(),
        shadowElevation = 0.dp
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = brishMutedPanelColor()) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.padding(8.dp).size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun DurationControl(label: String, minutes: Long, enabled: Boolean, onChange: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text("$minutes min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(enabled = enabled && minutes > 1, onClick = { onChange((minutes - 5).coerceAtLeast(1)) }) {
            Icon(Icons.Rounded.Remove, contentDescription = "Restar 5 minutos")
        }
        IconButton(enabled = enabled && minutes < 120, onClick = { onChange((minutes + 5).coerceAtMost(120)) }) {
            Icon(Icons.Rounded.Add, contentDescription = "Añadir 5 minutos")
        }
    }
}

private fun soundLabel(sound: TimerManager.AmbientSound): String = when (sound) {
    TimerManager.AmbientSound.NONE -> "Silencio"
    TimerManager.AmbientSound.RAIN -> "Lluvia"
    TimerManager.AmbientSound.FIRE -> "Fuego"
    TimerManager.AmbientSound.CAFE -> "Café"
    TimerManager.AmbientSound.AMBIENT -> "Ambiente"
    TimerManager.AmbientSound.OCEAN -> "Océano"
    TimerManager.AmbientSound.CUSTOM -> "Mi sonido"
}
