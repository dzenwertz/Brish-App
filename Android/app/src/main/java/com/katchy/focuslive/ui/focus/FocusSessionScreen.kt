package com.katchy.focuslive.ui.focus

import com.katchy.focuslive.ui.components.SmoothDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.provider.Settings
import android.content.Intent
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.DoNotDisturbOff
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.ui.home.HomeViewModel
import com.katchy.focuslive.ui.home.AnimatedFocusBackground

import com.katchy.focuslive.ui.util.bounceClick
import com.katchy.focuslive.ui.util.glassmorphism
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.TextStyle

@Composable
fun FocusSessionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val timeLeft by viewModel.timeLeft.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val isZenMode by viewModel.isZenModeEnabled.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Sync DND state on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Handle DND Permission Request
    var showDndPermissionDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.zenModeEffect.collect { effect ->
            when(effect) {
                HomeViewModel.ZenModeEffect.RequestDndPermission -> {
                    showDndPermissionDialog = true
                }
            }
        }
    }
    
    if (showDndPermissionDialog) {
        val isDarkDialog = com.katchy.focuslive.ui.theme.isDarkTheme()
        SmoothDialog(onDismissRequest = { showDndPermissionDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDarkDialog) Color(0xFF15171E) else Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDarkDialog) Color.White.copy(alpha = 0.07f) else Color(0xFFE5E7EB)
                ),
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Modo Concentración",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Para silenciar notificaciones y evitar distracciones durante tu sesión, Brish necesita permiso para activar 'No Molestar'.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDndPermissionDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(100.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDarkDialog) Color.White.copy(alpha = 0.20f) else Color(0xFFD1D5DB)
                            )
                        ) {
                            Text(
                                "Cancelar",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = {
                                showDndPermissionDialog = false
                                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkDialog) Color(0xFFF3F4F6) else Color(0xFF111827),
                                contentColor = if (isDarkDialog) Color(0xFF111827) else Color.White
                            )
                        ) {
                            Text("Conceder", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    
    val timerState by viewModel.timerState.collectAsState()
    val workDuration by viewModel.workDuration.collectAsState(initial = 25L)
    val breakDuration by viewModel.breakDuration.collectAsState(initial = 5L)
    
    val activeTaskId by viewModel.activeTaskId.collectAsState()
    val homeState by viewModel.homeState.collectAsState()
    val activeTask = homeState.tasks.find { it.id == activeTaskId }
    
    val activeSound by viewModel.ambientSound.collectAsState()
    val customSoundUri by viewModel.customSoundUri.collectAsState()
    var showSoundPicker by remember { mutableStateOf(false) }
    
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.setCustomSoundUri(uri.toString())
                viewModel.setAmbientSound(com.katchy.focuslive.data.manager.TimerManager.AmbientSound.CUSTOM)
                showSoundPicker = false
            } catch (e: Exception) {
                android.util.Log.e("FocusSessionScreen", "Exception caught", e)
            }
        }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    
    val minutes = String.format(java.util.Locale.US, "%02d", timeLeft / 60)
    val seconds = String.format(java.util.Locale.US, "%02d", timeLeft % 60)
    
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val isWarning = timeLeft in 1..59 && timerState == com.katchy.focuslive.data.manager.TimerManager.TimerState.WORK
    val isBreak = timerState == com.katchy.focuslive.data.manager.TimerManager.TimerState.BREAK
    
    val themePrimary = MaterialTheme.colorScheme.primary
    
    val animatedBg by animateColorAsState(
        targetValue = when { 
            isBreak -> Color(0xFF064E3B)
            isWarning -> Color(0xFF991B1B)
            else -> Color(0xFF0D0D14) // Near-black with neutral base 
        }, 
        animationSpec = tween(2000),
        label = "bgColorTransition"
    )
    
    // Blend theme tint into the base background
    val themeAccentBg = androidx.compose.ui.graphics.lerp(animatedBg, themePrimary, 0.18f)

    val currentAccentColor = when {
        isBreak -> Color(0xFF10B981) // Emerald / green
        isWarning -> Color(0xFFEF4444) // Rose / red
        else -> themePrimary // Cyan
    }

    // Zen Mode Autohide State
    var showZenControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableStateOf(0L) }

    LaunchedEffect(isRunning, isZenMode, lastInteractionTime) {
        if (isRunning && isZenMode) {
            delay(3000)
            showZenControls = false
        } else {
            showZenControls = true
        }
    }

    val zenAlpha by animateFloatAsState(
        targetValue = if (showZenControls) 1f else 0f,
        animationSpec = tween(600),
        label = "zenAlpha"
    )

    if (showSettingsDialog) {
        AnimatedGlassDialog(onDismissRequest = { showSettingsDialog = false }) { dismiss ->
            var animateItems by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                animateItems = true
            }

            Column(
                modifier = Modifier
                    .width(320.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = animateItems,
                    enter = fadeIn(tween(300, delayMillis = 50)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = spring(dampingRatio = 0.8f)),
                    exit = fadeOut(tween(200))
                ) {
                    Text(
                        text = "Configuración",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                AnimatedVisibility(
                    visible = animateItems,
                    enter = fadeIn(tween(300, delayMillis = 100)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = spring(dampingRatio = 0.8f)),
                    exit = fadeOut(tween(200))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enfoque",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "$workDuration min",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SpringSlider(
                            value = workDuration.toFloat(),
                            onValueChange = { 
                                val snapped = ((it / 5f).roundToInt() * 5).coerceIn(5, 60).toLong()
                                viewModel.setWorkDuration(snapped) 
                            },
                            valueRange = 5f..60f,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AnimatedVisibility(
                    visible = animateItems,
                    enter = fadeIn(tween(300, delayMillis = 150)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = spring(dampingRatio = 0.8f)),
                    exit = fadeOut(tween(200))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Descanso",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "$breakDuration min",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SpringSlider(
                            value = breakDuration.toFloat(),
                            onValueChange = { 
                                val snapped = it.roundToInt().coerceIn(1, 30).toLong()
                                viewModel.setBreakDuration(snapped) 
                            },
                            valueRange = 1f..30f,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                AnimatedVisibility(
                    visible = animateItems,
                    enter = fadeIn(tween(300, delayMillis = 200)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = spring(dampingRatio = 0.8f)),
                    exit = fadeOut(tween(200))
                ) {
                    Button(
                        onClick = {
                            animateItems = false
                            dismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Listo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    var isVisibleScreen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisibleScreen = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(themeAccentBg.copy(alpha=0.85f), animatedBg.copy(alpha=0.98f))))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        lastInteractionTime = System.currentTimeMillis()
                        showZenControls = true
                    }
                )
            }
    ) {
        AnimatedVisibility(
            visible = isVisibleScreen,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.95f),
            exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedFocusBackground(modifier = Modifier.fillMaxSize(), customTint = currentAccentColor)

                IconButton(
                    onClick = { if (showZenControls) onNavigateBack() },
                    enabled = showZenControls,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(24.dp)
                        .graphicsLayer { alpha = zenAlpha }
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .bounceClick()
                ) {
                    Icon(Icons.Rounded.CloseFullscreen, "Cerrar pantalla de enfoque", tint = Color.White)
                }
                
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(24.dp)
                        .graphicsLayer { alpha = zenAlpha },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { if (showZenControls) { viewModel.toggleZenMode(); lastInteractionTime = System.currentTimeMillis() } },
                        enabled = showZenControls,
                        modifier = Modifier
                            .background(if(isZenMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f), CircleShape)
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = if (isZenMode) Icons.Rounded.DoNotDisturbOn else Icons.Rounded.DoNotDisturbOff,
                            contentDescription = "Modo Zen",
                            tint = Color.White.copy(alpha = if(isZenMode) 1f else 0.6f)
                        )
                    }

                    IconButton(
                        onClick = { if (showZenControls) { showSoundPicker = true; lastInteractionTime = System.currentTimeMillis() } },
                        enabled = showZenControls,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = if (activeSound == com.katchy.focuslive.data.manager.TimerManager.AmbientSound.NONE) 
                                Icons.AutoMirrored.Rounded.VolumeOff else Icons.Rounded.MusicNote, 
                            contentDescription = "Cambiar sonido ambiental", 
                            tint = if (activeSound == com.katchy.focuslive.data.manager.TimerManager.AmbientSound.NONE) 
                                Color.White.copy(alpha = 0.6f) else Color.White
                        )
                    }

                    IconButton(
                        onClick = { if (showZenControls) { showSettingsDialog = true; lastInteractionTime = System.currentTimeMillis() } },
                        enabled = showZenControls,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .bounceClick()
                    ) {
                        Icon(Icons.Rounded.Settings, "Ajustes de pomodoro", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isBreak) "MODO DESCANSO" else "MODO ENFOQUE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 4.sp
                    )
                    
                    if (!isBreak && activeTask != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .graphicsLayer { alpha = zenAlpha }
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.RadioButtonChecked, 
                                "Tarea activa", 
                                tint = currentAccentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activeTask.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Box(contentAlignment = Alignment.Center) {
                        // Background Track Rings
                        Canvas(modifier = Modifier.size(260.dp)) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.03f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        }
                        Canvas(modifier = Modifier.size(240.dp)) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.02f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Breathing Glow Aura
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .scale(breathingScale * 1.1f)
                                .alpha(glowAlpha * 0.15f)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(currentAccentColor.copy(alpha = 0.35f), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                )
                        )

                        if (isRunning) {
                             val orbitalRotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(10000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "orbitalRotation"
                            )

                            Canvas(modifier = Modifier.size(260.dp).rotate(orbitalRotation)) {
                                drawArc(
                                    color = currentAccentColor.copy(alpha = 0.25f),
                                    startAngle = 0f,
                                    sweepAngle = 120f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }

                            Canvas(modifier = Modifier.size(240.dp).rotate(-orbitalRotation * 1.5f)) {
                                 drawArc(
                                    color = currentAccentColor.copy(alpha = 0.15f),
                                    startAngle = 180f,
                                    sweepAngle = 90f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }
                        }

                        Text(
                            text = "$minutes:$seconds",
                            fontSize = 100.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-4).sp,
                            style = TextStyle(
                                fontFeatureSettings = "tnum"
                            ),
                            modifier = Modifier.scale(if (isRunning) breathingScale else 1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isBreak) {
                        if (isRunning) {
                             Text("Relájate y recarga energías", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                             Text("¿Listo para el descanso?", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else if (isWarning) {
                        Text("¡Último esfuerzo!", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                         if (isRunning) {
                            Text("Sumérgete en el trabajo", color = Color.White.copy(alpha=0.5f))
                         } else {
                            Text("¡Hora de enfocar!", color = Color.White, fontWeight = FontWeight.Bold)
                         }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.graphicsLayer { alpha = zenAlpha }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                .bounceClick { 
                                    if (showZenControls) {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        viewModel.resetTimer() 
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                }
                        ) {
                            Icon(Icons.Rounded.Refresh, "Reiniciar cronómetro", tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(96.dp)
                                .scale(if (isRunning) 1f + (breathingScale-1f)*0.5f else 1f)
                                .shadow(if (isRunning) (glowAlpha * 20).dp else 0.dp, CircleShape, spotColor = Color.White)
                                .background(Color.White, CircleShape)
                                .bounceClick(scaleDown = 0.92f) { 
                                    if (showZenControls) {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        viewModel.toggleTimer() 
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                }
                        ) {
                           Column(horizontalAlignment = Alignment.CenterHorizontally) {
                               Icon(
                                   imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                   contentDescription = if (isRunning) "Pausar cronómetro" else "Iniciar cronómetro",
                                   tint = Color.Black,
                                   modifier = Modifier.size(32.dp)
                               )
                               if (!isRunning) {
                                   Text(
                                       text = if (isBreak) "Descanso" else "Iniciar",
                                       style = MaterialTheme.typography.labelSmall,
                                       fontWeight = FontWeight.Bold,
                                       color = Color.Black,
                                       fontSize = 10.sp
                                   )
                               }
                           }
                        }
                        
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                .bounceClick { 
                                    if (showZenControls) {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        viewModel.skipSession()
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                }
                        ) {
                            Icon(Icons.Rounded.SkipNext, "Saltar sesión", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
        }

        if (showSoundPicker) {
            SoundPickerDialog(
                activeSound = activeSound,
                customSoundUri = customSoundUri,
                onDismiss = { showSoundPicker = false },
                onSoundSelected = { sound ->
                    viewModel.setAmbientSound(sound)
                    showSoundPicker = false
                },
                onPickCustomMusic = {
                    audioPickerLauncher.launch(arrayOf("audio/*"))
                }
            )
        }
    }

@Composable
fun SoundPickerDialog(
    activeSound: com.katchy.focuslive.data.manager.TimerManager.AmbientSound,
    customSoundUri: String?,
    onDismiss: () -> Unit,
    onSoundSelected: (com.katchy.focuslive.data.manager.TimerManager.AmbientSound) -> Unit,
    onPickCustomMusic: () -> Unit
) {
    AnimatedGlassDialog(onDismissRequest = onDismiss) { dismiss ->
        var animateItems by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            animateItems = true
        }

        Column(
            modifier = Modifier
                .width(300.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = animateItems,
                enter = fadeIn(tween(300, delayMillis = 50)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = spring(dampingRatio = 0.8f)),
                exit = fadeOut(tween(200))
            ) {
                Text(
                    text = "Sonidos Ambientales",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val sounds = com.katchy.focuslive.data.manager.TimerManager.AmbientSound.entries
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sounds.forEachIndexed { index, sound ->
                    val isSelected = activeSound == sound
                    
                    if (sound == com.katchy.focuslive.data.manager.TimerManager.AmbientSound.CUSTOM && customSoundUri == null && !isSelected) {
                        return@forEachIndexed // Skip custom option if none selected, we'll show an add button instead
                    }
                    
                    val itemBgColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        Color.White.copy(alpha = 0.04f)
                    }
                    val itemBorderColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    } else {
                        Color.White.copy(alpha = 0.05f)
                    }

                    AnimatedVisibility(
                        visible = animateItems,
                        enter = fadeIn(tween(300, delayMillis = 80 + index * 40)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = spring(dampingRatio = 0.8f)),
                        exit = fadeOut(tween(200))
                    ) {
                        Surface(
                            onClick = { 
                                if (sound == com.katchy.focuslive.data.manager.TimerManager.AmbientSound.CUSTOM) {
                                    onPickCustomMusic() // Allow picking a new one even if already selected
                                } else {
                                    onSoundSelected(sound) 
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = itemBgColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, itemBorderColor, RoundedCornerShape(14.dp))
                                .bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when(sound) {
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.NONE -> Icons.AutoMirrored.Rounded.VolumeOff
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.RAIN -> Icons.Rounded.Cloud
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.FIRE -> Icons.Rounded.LocalFireDepartment
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.CAFE -> Icons.Rounded.Coffee
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.AMBIENT -> Icons.Rounded.Nature
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.OCEAN -> Icons.Rounded.Water
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.CUSTOM -> Icons.Rounded.LibraryMusic
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = when(sound) {
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.NONE -> "Silencio"
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.RAIN -> "Lluvia Calma"
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.FIRE -> "Fogata Lejana"
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.CAFE -> "Cafetería Jazz"
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.AMBIENT -> "Naturaleza"
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.OCEAN -> "Olas del Mar"
                                        com.katchy.focuslive.data.manager.TimerManager.AmbientSound.CUSTOM -> "Música Personalizada"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                        }
                    }
                }
                
                // Add Custom Music Button
                if (customSoundUri == null || activeSound != com.katchy.focuslive.data.manager.TimerManager.AmbientSound.CUSTOM) {
                    val customIndex = sounds.size
                    AnimatedVisibility(
                        visible = animateItems,
                        enter = fadeIn(tween(300, delayMillis = 80 + customIndex * 40)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = spring(dampingRatio = 0.8f)),
                        exit = fadeOut(tween(200))
                    ) {
                        Surface(
                            onClick = onPickCustomMusic,
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                .bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Música desde dispositivo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            AnimatedVisibility(
                visible = animateItems,
                enter = fadeIn(tween(300, delayMillis = 80 + (sounds.size + 1) * 40)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = spring(dampingRatio = 0.8f)),
                exit = fadeOut(tween(200))
            ) {
                TextButton(
                    onClick = {
                        animateItems = false
                        dismiss()
                    },
                    modifier = Modifier.bounceClick()
                ) {
                    Text("CERRAR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun AnimatedGlassDialog(
    onDismissRequest: () -> Unit,
    content: @Composable (dismiss: () -> Unit) -> Unit
) {
    var isAnimateOpen by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        isAnimateOpen = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isAnimateOpen) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dialogScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isAnimateOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = EaseInOutSine),
        label = "dialogAlpha"
    )
    
    val dismiss = {
        coroutineScope.launch {
            isAnimateOpen = false
            delay(150)
            onDismissRequest()
        }
        Unit
    }
    
    Dialog(onDismissRequest = { dismiss() }) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .glassmorphism(
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = Color(0xFF0F0F14).copy(alpha = 0.90f),
                    borderColor = Color(0xFF2C2C35),
                    borderWidth = 1.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            content(dismiss)
        }
    }
}

@Composable
fun SpringSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedRange<Float>,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    // Spring animation for thumb scaling
    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "thumbScale"
    )

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = Color.White.copy(alpha = 0.15f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        if (widthPx <= 0f) return@BoxWithConstraints
        val range = valueRange.endInclusive - valueRange.start
        
        // Calculate current position fraction
        val fraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)
        val currentX = fraction * widthPx

        // Track and gesture detection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(inactiveColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            isPressed = true
                            val newValue = valueRange.start + (offset.x / widthPx) * range
                            onValueChange(newValue.coerceIn(valueRange))
                            try {
                                tryAwaitRelease()
                            } catch (e: Exception) {
                                // Safe capture on layout detachment
                            }
                            isPressed = false
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isPressed = true },
                        onDragEnd = { isPressed = false },
                        onDragCancel = { isPressed = false },
                        onDrag = { change, _ ->
                            val currentFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                            val newValue = valueRange.start + currentFraction * range
                            onValueChange(newValue.coerceIn(valueRange))
                        }
                    )
                }
        ) {
            // Active Track
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(androidx.compose.ui.platform.LocalDensity.current) { currentX.toDp() })
                    .background(activeColor)
            )
        }

        // Thumb
        Box(
            modifier = Modifier
                .offset(x = with(androidx.compose.ui.platform.LocalDensity.current) { (currentX - 10.dp.toPx()).toDp() })
                .size(20.dp)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(elevation = 4.dp, shape = CircleShape)
                .background(Color.White, CircleShape)
                .border(2.dp, activeColor, CircleShape)
        )
    }
}

