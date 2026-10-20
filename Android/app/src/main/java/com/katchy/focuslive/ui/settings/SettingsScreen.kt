package com.katchy.focuslive.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.BuildConfig
import com.katchy.focuslive.data.model.AppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onNavigateToLogin: () -> Unit = {}, onBack: () -> Unit = {}, onNavigateToSubscription: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val theme by viewModel.currentTheme.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val premium by viewModel.isPremium.collectAsState()
    val work by viewModel.workDuration.collectAsState()
    val rest by viewModel.breakDuration.collectAsState()
    val sound by viewModel.isPomodoroSoundEnabled.collectAsState()
    val message by viewModel.message.collectAsState()
    val busy by viewModel.busy.collectAsState()
    var logout by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportLegacy) }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); viewModel.dismissMessage() } }
    Scaffold(topBar = { TopAppBar(title = { Text("Ajustes") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
    }) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(user?.displayName ?: "Tu cuenta", style = MaterialTheme.typography.titleLarge)
            Text(user?.email ?: "Cuenta de invitado", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { name = user?.displayName.orEmpty(); editName = true }) { Text("Editar nombre") }
            if (premium) Text("Suscripción Premium activa", style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = onNavigateToSubscription) { Text("Consultar o restaurar compras anteriores") }
            HorizontalDivider()
            Text("Apariencia", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AppTheme.LIGHT to "Claro", AppTheme.DARK to "Oscuro", AppTheme.SYSTEM to "Sistema").forEach { (option, title) ->
                    FilterChip(selected = theme == option || option == AppTheme.DARK && theme == AppTheme.AMOLED,
                        onClick = { viewModel.setTheme(option) }, label = { Text(title) })
                }
            }
            Text("Pomodoro", style = MaterialTheme.typography.titleMedium)
            Text("Enfoque: $work minutos")
            Slider(value = work.toFloat(), onValueChange = { viewModel.setWorkDuration(it.toLong()) }, valueRange = 5f..120f, steps = 114)
            Text("Descanso: $rest minutos")
            Slider(value = rest.toFloat(), onValueChange = { viewModel.setBreakDuration(it.toLong()) }, valueRange = 1f..30f, steps = 28)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sonido al finalizar", modifier = Modifier.weight(1f))
                Switch(checked = sound, onCheckedChange = viewModel::setPomodoroSoundEnabled)
            }
            HorizontalDivider()
            Text("Datos de versiones anteriores", style = MaterialTheme.typography.titleMedium)
            Text("Tus registros de Finanzas y Hábitos se conservan. Puedes exportarlos a un archivo JSON.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(enabled = !busy, onClick = { export.launch("Brish-datos-anteriores.json") }) { Text("Exportar registros") }
            Text("Brish ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium)
            TextButton(enabled = !busy, onClick = { logout = true }) { Text("Cerrar sesión") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
    if (editName) AlertDialog(onDismissRequest = { editName = false }, title = { Text("Editar nombre") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { viewModel.updateDisplayName(name) { if (it) editName = false } }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancelar") } })
    if (logout) AlertDialog(onDismissRequest = { logout = false }, title = { Text("Cerrar sesión") },
        text = { Text("Los cambios pendientes permanecerán en este dispositivo, vinculados a tu cuenta.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { viewModel.logout { logout = false; onNavigateToLogin() } }) { Text("Salir") } },
        dismissButton = { TextButton(onClick = { logout = false }) { Text("Cancelar") } })
}
