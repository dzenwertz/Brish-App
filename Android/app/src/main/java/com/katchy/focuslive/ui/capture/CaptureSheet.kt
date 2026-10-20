package com.katchy.focuslive.ui.capture

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.AreaHealthBg
import com.katchy.focuslive.ui.theme.AreaHome
import com.katchy.focuslive.ui.theme.AreaHomeBg
import com.katchy.focuslive.ui.theme.AreaPersonal
import com.katchy.focuslive.ui.theme.AreaPersonalBg
import com.katchy.focuslive.ui.theme.AreaWork
import com.katchy.focuslive.ui.theme.AreaWorkBg
import com.katchy.focuslive.ui.theme.BrishButtonBrush
import com.katchy.focuslive.ui.theme.BrishHeroBrush
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishOnYellow
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SheetShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
private val FieldShape = RoundedCornerShape(22.dp)
private val Spanish = Locale("es", "PE")

/**
 * Hoja de captura rápida. Sube con resorte desde el ＋, se cierra arrastrando hacia abajo,
 * con Atrás o tocando fuera. El borde del campo gira mientras Brish interpreta el texto.
 */
@Composable
fun CaptureSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val reducedMotion = rememberReducedMotion()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val dismissThreshold = with(density) { 140.dp.toPx() }

    BackHandler(enabled = visible) { onDismiss() }
    LaunchedEffect(visible) { if (visible) dragOffset.snapTo(0f) }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x522A2012))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                    .semantics { contentDescription = "Cerrar captura" }
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = if (reducedMotion) fadeIn(tween(150)) else slideInVertically(BrishMotion.screen()) { it },
            exit = if (reducedMotion) fadeOut(tween(150)) else slideOutVertically(tween(260)) { it }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                    .brishShadow(SheetShape, elevation = 24.dp)
                    .clip(SheetShape)
                    .background(MaterialTheme.colorScheme.background)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            scope.launch { dragOffset.snapTo((dragOffset.value + delta).coerceAtLeast(0f)) }
                        },
                        onDragStopped = { velocity ->
                            if (dragOffset.value > dismissThreshold || velocity > 1800f) onDismiss()
                            else dragOffset.animateTo(0f, BrishMotion.lively())
                        }
                    )
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 42.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                )
                CaptureHeader()
                TypeSelector(selected = state.forcedType, onSelect = viewModel::onTypeChange)
                CaptureField(
                    text = state.text,
                    onTextChange = viewModel::onTextChange,
                    visible = visible,
                    reducedMotion = reducedMotion,
                    onDone = { viewModel.save(onDismiss) }
                )
                state.draft?.let { PreviewCard(it) }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                ActionsRow(
                    save = state.save,
                    onSpeech = { spoken -> viewModel.onTextChange(listOf(state.text, spoken).filter { it.isNotBlank() }.joinToString(" ")) },
                    onSave = { viewModel.save(onDismiss) }
                )
                Text(
                    "«gym lun mie vie 5:30pm» → rutina en Agenda\n«idea: video de reparación» → nota\n«tomar agua 8 veces al día» → hábito",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun CaptureHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BrishHeroBrush)
            .brishEdgeHighlight()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Captura ideas, tareas y más", style = MaterialTheme.typography.titleLarge, color = BrishInk)
            Text("Escribe como hablas. Brish lo ordena.", style = MaterialTheme.typography.bodyMedium, color = BrishOnYellow)
        }
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = Color(0xFFE3A21C), modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun TypeSelector(selected: CaptureType?, onSelect: (CaptureType?) -> Unit) {
    val options = listOf<Pair<CaptureType?, String>>(null to "Auto") + CaptureType.entries.map { it to it.label }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (type, label) ->
            val isSelected = type == selected
            val bg by animateColorAsState(if (isSelected) BrishYellow else MaterialTheme.colorScheme.surface, label = "typeBg")
            val fg by animateColorAsState(if (isSelected) BrishInk else MaterialTheme.colorScheme.onSurfaceVariant, label = "typeFg")
            Box(
                Modifier
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable(role = Role.RadioButton) { onSelect(type) }
                    .padding(horizontal = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun CaptureField(
    text: String,
    onTextChange: (String) -> Unit,
    visible: Boolean,
    reducedMotion: Boolean,
    onDone: () -> Unit
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var thinking by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(280)
            runCatching { focus.requestFocus() }
            keyboard?.show()
        }
    }
    LaunchedEffect(text) {
        if (text.isBlank()) { thinking = false; return@LaunchedEffect }
        thinking = true
        delay(900)
        thinking = false
    }
    val spin = rememberInfiniteTransition(label = "border")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    val glow by animateFloatAsState(if (thinking && !reducedMotion) 1f else 0f, tween(260), label = "glow")
    val sweep = remember {
        Brush.sweepGradient(listOf(BrishYellow, Color(0xFFF3B6A0), Color(0xFFB9CDF5), BrishYellow))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Escribe como lo dirías", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(FieldShape)
                .drawBehind {
                    drawRect(BrishYellow)
                    if (glow > 0f) {
                        val side = size.maxDimension * 1.6f
                        rotate(angle) {
                            drawRect(
                                brush = sweep,
                                topLeft = Offset(center.x - side / 2, center.y - side / 2),
                                size = Size(side, side),
                                alpha = glow
                            )
                        }
                    }
                }
                .padding(2.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (text.isEmpty()) {
                Text("Pagar la luz el viernes 6pm #hogar", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 27.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "Qué quieres capturar" }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewCard(draft: CaptureDraft) {
    val chips = buildList {
        add(Chip(draft.type.label, BrishInk, Color.White))
        dateLabel(draft)?.let { add(Chip(it)) }
        draft.time?.let { add(Chip(it.format(DateTimeFormatter.ofPattern("HH:mm")))) }
        draft.timesPerDay?.let { add(Chip("$it veces al día")) }
        draft.area?.let { area -> add(areaChip(area)) }
        if (draft.time != null && draft.type != CaptureType.NOTE) {
            add(Chip("Aviso a las ${draft.time.format(DateTimeFormatter.ofPattern("HH:mm"))}"))
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Brish lo guardará como", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (draft.title.isNotBlank()) {
            Text(draft.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            chips.forEach { chip ->
                key(chip.label) {
                    val shown = remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { shown.value = true }
                    AnimatedVisibility(shown.value, enter = scaleIn(BrishMotion.lively(), initialScale = 0.7f) + fadeIn(tween(160))) {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(chip.bg ?: MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(chip.label, color = chip.fg ?: MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Text("Toca un tipo arriba si Brish se equivocó.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class Chip(val label: String, val bg: Color? = null, val fg: Color? = null)

private fun areaChip(area: String) = when (area) {
    "Trabajo" -> Chip(area, AreaWorkBg, AreaWork)
    "Salud" -> Chip(area, AreaHealthBg, AreaHealth)
    "Hogar" -> Chip(area, AreaHomeBg, AreaHome)
    else -> Chip(area, AreaPersonalBg, AreaPersonal)
}

private val ShortDayNames = mapOf(
    Calendar.MONDAY to "Lun", Calendar.TUESDAY to "Mar", Calendar.WEDNESDAY to "Mié", Calendar.THURSDAY to "Jue",
    Calendar.FRIDAY to "Vie", Calendar.SATURDAY to "Sáb", Calendar.SUNDAY to "Dom"
)

private fun dateLabel(draft: CaptureDraft): String? {
    if (draft.daily) return "Todos los días"
    if (draft.repeatDays.isNotEmpty()) return draft.repeatDays.mapNotNull { ShortDayNames[it] }.joinToString(" · ")
    val date = draft.date ?: return if (draft.type == CaptureType.TASK || draft.type == CaptureType.EVENT) "Hoy" else null
    val today = LocalDate.now()
    return when (date) {
        today -> "Hoy"
        today.plusDays(1) -> "Mañana"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", Spanish)).replace(".", "").replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun ActionsRow(save: SaveState, onSpeech: (String) -> Unit, onSave: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    var speechError by remember { mutableStateOf(false) }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onSpeech)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(54.dp)
                .brishShadow(CircleShape, elevation = 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(role = Role.Button) {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, "Dime qué quieres guardar")
                    try { speech.launch(intent) } catch (e: ActivityNotFoundException) { speechError = true }
                }
                .semantics { contentDescription = "Dictar" },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        SaveButton(save = save, onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onSave()
        }, modifier = Modifier.weight(1f))
    }
    if (speechError) {
        Text("Este teléfono no tiene dictado por voz disponible.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun SaveButton(save: SaveState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) BrishMotion.PressScale else 1f, BrishMotion.lively(), label = "saveScale")
    val arrowShift by animateFloatAsState(if (pressed) 6f else 0f, BrishMotion.lively(), label = "arrow")
    Box(
        modifier
            .height(56.dp)
            .scale(scale)
            .brishShadow(BrishShapes.pill, elevation = 14.dp, tone = Color(0xFFD6A014), strength = 1.4f)
            .clip(BrishShapes.pill)
            .background(BrishButtonBrush)
            .brishEdgeHighlight()
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, enabled = save == SaveState.Idle, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(targetState = save, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "save") { s ->
            when (s) {
                SaveState.Idle -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Guardar", color = BrishInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = BrishInk, modifier = Modifier.offset(x = arrowShift.dp))
                }
                SaveState.Saving -> CircularProgressIndicator(color = BrishInk, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
                SaveState.Saved -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = BrishInk)
                    Spacer(Modifier.width(8.dp))
                    Text("Guardado", color = BrishInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
