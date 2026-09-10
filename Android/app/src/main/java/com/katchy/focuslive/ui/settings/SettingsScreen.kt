package com.katchy.focuslive.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.R
import com.katchy.focuslive.data.model.AppTheme
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.theme.AntiPrimary
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.InAppNotificationBus
import com.katchy.focuslive.ui.util.bounceClick

@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    
    val isPlannerEnabled by viewModel.isPlannerEnabled.collectAsState()
    val isNotesEnabled by viewModel.isNotesEnabled.collectAsState()
    val isFinanceEnabled by viewModel.isFinanceEnabled.collectAsState()
    val isHabitsEnabled by viewModel.isHabitsEnabled.collectAsState()

    val isPomodoroSoundEnabled by viewModel.isPomodoroSoundEnabled.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val hasOpenCodeKey by viewModel.hasOpenCodeKey.collectAsState()

    var showLanguagePicker by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showPomodoroDurationDialog by remember { mutableStateOf(false) }
    var showOpenCodeDialog by remember { mutableStateOf(false) }

    val workDuration by viewModel.workDuration.collectAsState(initial = 25L)
    val breakDuration by viewModel.breakDuration.collectAsState(initial = 5L)

    val isDark = isDarkTheme()
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .bounceClick()
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Ajustes",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.size(38.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 1. User Profile Card
            Surface(
                onClick = { showEditNameDialog = true },
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF15171E) else Color.White,
                shadowElevation = if (isDark) 0.dp else 2.dp,
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                modifier = Modifier.fillMaxWidth().bounceClick()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Box
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E212B) else Color(0xFFE8EEF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.email?.take(1)?.uppercase() ?: (user?.displayName?.take(1)?.uppercase() ?: "U"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1E3A8A)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.displayName ?: "Usuario",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user?.email ?: "Cuenta de invitado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // 2. Personalization (Theme Options)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tema de la aplicación",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF15171E) else Color.White,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemePillOption(
                            title = "Claro",
                            icon = Icons.Rounded.LightMode,
                            selected = currentTheme == AppTheme.LIGHT,
                            isDark = isDark,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.setTheme(AppTheme.LIGHT)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ThemePillOption(
                            title = "Oscuro",
                            icon = Icons.Rounded.DarkMode,
                            selected = currentTheme == AppTheme.DARK || currentTheme == AppTheme.AMOLED,
                            isDark = isDark,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.setTheme(AppTheme.DARK)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ThemePillOption(
                            title = "Sistema",
                            icon = Icons.Rounded.SettingsBrightness,
                            selected = currentTheme == AppTheme.SYSTEM,
                            isDark = isDark,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.setTheme(AppTheme.SYSTEM)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. General Preferences
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Preferencias",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF15171E) else Color.White,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ModernSettingsItem(
                            icon = Icons.Rounded.Language,
                            title = "Idioma",
                            subtitle = when (currentLanguage) {
                                "en" -> "English"
                                "pt" -> "Português"
                                else -> "Español"
                            },
                            onClick = { showLanguagePicker = true },
                            isDark = isDark,
                            showDivider = true
                        )

                        ModernSettingsItem(
                            icon = Icons.Rounded.HourglassTop,
                            title = "Duración de Pomodoro",
                            subtitle = "Enfoque: $workDuration min | Descanso: $breakDuration min",
                            onClick = { showPomodoroDurationDialog = true },
                            isDark = isDark,
                            showDivider = true
                        )

                        ModernSettingsToggle(
                            icon = Icons.Rounded.Timer,
                            title = "Sonido al finalizar Pomodoro",
                            checked = isPomodoroSoundEnabled,
                            onCheckedChange = { viewModel.setPomodoroSoundEnabled(it) },
                            isDark = isDark,
                            showDivider = false
                        )
                    }
                }
            }

            // 4. Brish IA
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Brish IA",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF15171E) else Color.White,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModernSettingsItem(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Clave de OpenCode",
                        subtitle = if (hasOpenCodeKey) "Configurada y protegida en este dispositivo" else "Necesaria para usar las funciones de IA",
                        onClick = { showOpenCodeDialog = true },
                        isDark = isDark,
                        iconTint = if (hasOpenCodeKey) Color(0xFF2E7D5B) else MaterialTheme.colorScheme.primary,
                        showDivider = false
                    )
                }
            }

            // 5. Modulos Activos
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Módulos",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF15171E) else Color.White,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ModernSettingsToggle(
                            icon = Icons.Rounded.CalendarToday,
                            title = "Agenda (Plan)",
                            checked = isPlannerEnabled,
                            onCheckedChange = { viewModel.setPlannerEnabled(it) },
                            isDark = isDark,
                            showDivider = true
                        )
                        ModernSettingsToggle(
                            icon = Icons.Rounded.FolderOpen,
                            title = "Notas",
                            checked = isNotesEnabled,
                            onCheckedChange = { viewModel.setNotesEnabled(it) },
                            isDark = isDark,
                            showDivider = true
                        )
                        ModernSettingsToggle(
                            icon = Icons.Rounded.CheckCircleOutline,
                            title = "Hábitos",
                            checked = isHabitsEnabled,
                            onCheckedChange = { viewModel.setHabitsEnabled(it) },
                            isDark = isDark,
                            showDivider = true
                        )
                        ModernSettingsToggle(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            title = "Finanzas",
                            checked = isFinanceEnabled,
                            onCheckedChange = { viewModel.setFinanceEnabled(it) },
                            isDark = isDark,
                            showDivider = false
                        )
                    }
                }
            }

            // 6. App Info & Logout
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Acerca de Brish",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF15171E) else Color.White,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ModernSettingsItem(
                            icon = Icons.Rounded.Info,
                            title = "Versión",
                            subtitle = "Brish v1.2.20 (Release)",
                            onClick = {},
                            isDark = isDark,
                            showDivider = true
                        )

                        ModernSettingsItem(
                            icon = Icons.AutoMirrored.Rounded.Logout,
                            title = "Cerrar sesión",
                            subtitle = "Desconectar tu cuenta actual",
                            onClick = { showLogoutDialog = true },
                            isDark = isDark,
                            iconTint = MaterialTheme.colorScheme.error,
                            showDivider = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        SmoothDialog(onDismissRequest = { showLogoutDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("¿Cerrar sesión?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Podrás volver a iniciar sesión cuando quieras.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.logout {
                                    showLogoutDialog = false
                                    onNavigateToLogin()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Salir", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        var newName by remember { mutableStateOf(user?.displayName ?: "") }
        SmoothDialog(onDismissRequest = { showEditNameDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Editar Nombre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Tu nombre") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showEditNameDialog = false }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    viewModel.updateDisplayName(newName.trim()) { success ->
                                        if (success) {
                                            InAppNotificationBus.post("Nombre actualizado")
                                        }
                                    }
                                    showEditNameDialog = false
                                }
                            },
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                                contentColor = if (isDark) Color(0xFF111827) else Color.White
                            )
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showOpenCodeDialog) {
        var apiKey by remember { mutableStateOf("") }
        SmoothDialog(onDismissRequest = { showOpenCodeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(340.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Conectar OpenCode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tu clave se cifra con Android Keystore y permanece en este dispositivo. OpenCode Zen puede ofrecer modelos gratuitos, sujetos a disponibilidad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API key") },
                        placeholder = { Text("Pega tu clave de OpenCode") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://opencode.ai/auth")))
                        },
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        Text("Obtener una clave")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (hasOpenCodeKey) {
                            TextButton(
                                onClick = {
                                    viewModel.clearOpenCodeKey()
                                    showOpenCodeDialog = false
                                    InAppNotificationBus.post("Clave eliminada del dispositivo")
                                }
                            ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                        Row {
                            TextButton(onClick = { showOpenCodeDialog = false }) { Text("Cancelar") }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                enabled = apiKey.isNotBlank(),
                                onClick = {
                                    if (viewModel.saveOpenCodeKey(apiKey)) {
                                        showOpenCodeDialog = false
                                        InAppNotificationBus.post("OpenCode conectado")
                                    } else {
                                        InAppNotificationBus.post("No se pudo guardar la clave")
                                    }
                                },
                                shape = RoundedCornerShape(100.dp)
                            ) { Text("Guardar", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemePillOption(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
        } else Color.Transparent,
        modifier = modifier.height(50.dp).bounceClick()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    if (isDark) Color(0xFF111827) else Color.White
                } else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) {
                    if (isDark) Color(0xFF111827) else Color.White
                } else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ModernSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDark: Boolean,
    iconTint: Color? = null,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(scaleDown = 0.98f) { onClick() }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(12.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F4F6),
                modifier = Modifier.padding(start = 52.dp, end = 18.dp)
            )
        }
    }
}

@Composable
fun ModernSettingsToggle(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDark: Boolean,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (isDark) Color.Black else Color.White,
                    checkedTrackColor = if (isDark) Color.White else Color(0xFF111827)
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F4F6),
                modifier = Modifier.padding(start = 52.dp, end = 18.dp)
            )
        }
    }
}
