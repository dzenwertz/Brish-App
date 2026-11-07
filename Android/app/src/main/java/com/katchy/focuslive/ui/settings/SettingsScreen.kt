package com.katchy.focuslive.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.BuildConfig
import com.katchy.focuslive.data.model.AppTheme
import com.katchy.focuslive.ui.components.BrishCardSurface
import com.katchy.focuslive.ui.components.BrishIconTile
import com.katchy.focuslive.ui.components.BrishPrimaryButton
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.BrishScreenHeader
import com.katchy.focuslive.ui.components.BrishSecondaryButton
import com.katchy.focuslive.ui.components.BrishSegmented
import com.katchy.focuslive.ui.components.BrishTextField
import com.katchy.focuslive.ui.components.DraftRecoveryNotice
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.components.rememberPersistentDraftText
import com.katchy.focuslive.ui.theme.AreaHome
import com.katchy.focuslive.ui.theme.AreaHomeBg
import com.katchy.focuslive.ui.theme.AreaPersonal
import com.katchy.focuslive.ui.theme.AreaPersonalBg
import com.katchy.focuslive.ui.theme.AreaWork
import com.katchy.focuslive.ui.theme.AreaWorkBg
import com.katchy.focuslive.ui.theme.BrishHeroBrush
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishOnYellow
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishYellowDeep
import com.katchy.focuslive.ui.theme.BrishYellowSoft
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.brishCascade
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow

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
    val context = LocalContext.current
    var logout by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }
    var confirmNameDiscard by remember { mutableStateOf(false) }
    val nameDraft = rememberPersistentDraftText("settings:display-name")
    var name by nameDraft::value
    var reduceMotion by remember(context) {
        mutableStateOf(context.getSharedPreferences("brish_accessibility", android.content.Context.MODE_PRIVATE).getBoolean("reduce_motion", false))
    }
    val snackbar = remember { SnackbarHostState() }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportLegacy) }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); viewModel.dismissMessage() } }
    val guest = user?.isAnonymous == true || user?.email.isNullOrBlank()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrishRoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", onBack)
                Spacer(Modifier.width(12.dp))
                BrishScreenHeader("Ajustes", Modifier.weight(1f))
            }

            // Cuenta
            Row(
                Modifier.fillMaxWidth().brishShadow(BrishShapes.hero, strength = 1.2f).clip(BrishShapes.hero).background(BrishHeroBrush).brishEdgeHighlight().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(BrishInk), contentAlignment = Alignment.Center) {
                    Text((user?.displayName?.trim()?.firstOrNull() ?: 'B').uppercase(), color = BrishYellowSoft, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(user?.displayName?.takeIf { it.isNotBlank() } ?: "Tu cuenta", style = MaterialTheme.typography.titleLarge, color = BrishInk)
                    Text(if (guest) "Sin cuenta: tus datos viven en este teléfono" else user?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = BrishOnYellow)
                    if (premium) Text("Premium activo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrishInk)
                }
                Text("Editar", fontWeight = FontWeight.SemiBold, color = BrishInk,
                    modifier = Modifier.clip(CircleShape).background(BrishYellowSoft).clickable(role = Role.Button) {
                        if (!nameDraft.recovered) name = user?.displayName.orEmpty()
                        editName = true
                    }
                        .padding(horizontal = 14.dp, vertical = 8.dp))
            }

            Section("Apariencia") {
                val options = listOf(AppTheme.LIGHT to "Claro", AppTheme.DARK to "Oscuro", AppTheme.SYSTEM to "Sistema")
                val index = options.indexOfFirst { it.first == theme || (it.first == AppTheme.DARK && theme == AppTheme.AMOLED) }.coerceAtLeast(0)
                BrishSegmented(options.map { it.second }, index, { viewModel.setTheme(options[it].first) }, Modifier.fillMaxWidth())
            }

            Section("Enfoque") {
                Stepper("Sesión de enfoque", "$work min") { delta -> viewModel.setWorkDuration(work + delta * 5) }
                Stepper("Descanso", "$rest min") { delta -> viewModel.setBreakDuration(rest + delta) }
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sonido al terminar", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Switch(sound, viewModel::setPomodoroSoundEnabled, colors = SwitchDefaults.colors(checkedTrackColor = BrishYellowDeep, checkedThumbColor = BrishInk))
                }
            }

            Section("Accesibilidad") {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reducir movimiento", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("Usa fundidos cortos y evita desplazamientos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = reduceMotion,
                        onCheckedChange = { enabled ->
                            reduceMotion = enabled
                            context.getSharedPreferences("brish_accessibility", android.content.Context.MODE_PRIVATE)
                                .edit().putBoolean("reduce_motion", enabled).apply()
                        }
                    )
                }
            }

            Section("Ayuda y permisos") {
                ActionRow(Icons.Rounded.Explore, AreaPersonalBg, AreaPersonal, "Ver la guía otra vez", "Deslizar, capturar y trabajar sin conexión") { viewModel.showGuideAgain() }
                ActionRow(Icons.Rounded.NotificationsActive, AreaWorkBg, AreaWork, "Avisos de Brish", "Recordatorios de tus bloques y hábitos") {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
                ActionRow(Icons.Rounded.WorkspacePremium, BrishYellowSoft, BrishInk, "Compras", "Consulta o restaura tus compras", onNavigateToSubscription)
            }

            Section("Tus datos") {
                ActionRow(Icons.Rounded.Download, AreaHomeBg, AreaHome, "Exportar registros anteriores", "Finanzas y hábitos en un archivo JSON") {
                    if (!busy) export.launch("Brish-datos-anteriores.json")
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            Row(
                Modifier.fillMaxWidth().height(54.dp).brishCard(CircleShape).clip(CircleShape).clickable(enabled = !busy, role = Role.Button) { logout = true },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            }
            Text("Brish ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (editName) {
        val requestDismiss = {
            if (name.isNotBlank() && name != user?.displayName.orEmpty()) confirmNameDiscard = true else editName = false
        }
        SmoothDialog(onDismissRequest = requestDismiss) {
            BrishCardSurface(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                shape = BrishShapes.hero
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tu nombre", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    DraftRecoveryNotice(nameDraft, Modifier.fillMaxWidth())
                    BrishTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Nombre",
                        label = "Nombre",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        BrishSecondaryButton("Cancelar", requestDismiss, Modifier.weight(1f), height = 46.dp)
                        BrishPrimaryButton(
                            text = "Guardar",
                            onClick = {
                                viewModel.updateDisplayName(name) { if (it) { nameDraft.clearAfterSave(); editName = false } }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = name.isNotBlank(),
                            height = 46.dp
                        )
                    }
                }
            }
        }
    }
    if (confirmNameDiscard) {
        SmoothDialog(onDismissRequest = { confirmNameDiscard = false }) {
            BrishCardSurface(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                shape = BrishShapes.hero
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("¿Descartar cambios?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text("El nombre escrito no se guardará.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        BrishSecondaryButton("Seguir editando", { confirmNameDiscard = false }, Modifier.weight(1f), height = 46.dp)
                        BrishPrimaryButton(
                            text = "Descartar",
                            onClick = { nameDraft.discard(); confirmNameDiscard = false; editName = false },
                            modifier = Modifier.weight(1f),
                            height = 46.dp
                        )
                    }
                }
            }
        }
    }
    if (logout) {
        SmoothDialog(onDismissRequest = { logout = false }) {
            BrishCardSurface(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                shape = BrishShapes.hero
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cerrar sesión", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        if (guest) "Estás sin cuenta: al salir, tus datos de este teléfono dejarán de estar accesibles. Antes, entra con Google desde la pantalla de inicio para conservarlos."
                        else "Los cambios pendientes se quedan en este teléfono, vinculados a tu cuenta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        BrishSecondaryButton("Cancelar", { logout = false }, Modifier.weight(1f), height = 46.dp)
                        BrishPrimaryButton(
                            text = "Salir",
                            onClick = { viewModel.logout { logout = false; onNavigateToLogin() } },
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                            height = 46.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.brishCascade(1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        Column(Modifier.fillMaxWidth().brishCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun Stepper(label: String, value: String, onStep: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        StepButton("−") { onStep(-1) }
        Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(76.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        StepButton("+") { onStep(1) }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(BrishShapes.card).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrishIconTile(icon, bg, fg, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
