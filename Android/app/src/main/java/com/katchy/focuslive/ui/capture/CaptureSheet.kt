package com.katchy.focuslive.ui.capture

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.PredictiveBackHandler
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
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katchy.focuslive.ui.theme.MotionTokens
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.graphicsLayer
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.ui.components.BrishCardSurface
import com.katchy.focuslive.ui.components.BrishPrimaryButton
import com.katchy.focuslive.ui.components.BrishSecondaryButton
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.AreaHealthBg
import com.katchy.focuslive.ui.theme.AreaHome
import com.katchy.focuslive.ui.theme.AreaHomeBg
import com.katchy.focuslive.ui.theme.AreaPersonal
import com.katchy.focuslive.ui.theme.AreaPersonalBg
import com.katchy.focuslive.ui.theme.AreaWork
import com.katchy.focuslive.ui.theme.AreaWorkBg
import com.katchy.focuslive.ui.theme.BrishButtonBrush
import com.katchy.focuslive.ui.theme.BrishGoldShadow
import com.katchy.focuslive.ui.theme.BrishHeroBrush
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishOnYellow
import com.katchy.focuslive.ui.theme.BrishOrbBlue
import com.katchy.focuslive.ui.theme.BrishOrbCoral
import com.katchy.focuslive.ui.theme.BrishScrim
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishWarning
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.BrishYellowSoft
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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.flushDraft()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.flushDraft()
        }
    }
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestDismiss = {
        if (state.text.isNotBlank()) confirmDiscard = true else onDismiss()
    }
    if (confirmDiscard) {
        SmoothDialog(onDismissRequest = { confirmDiscard = false }) {
            BrishCardSurface(
                shape = BrishShapes.hero,
                contentPadding = PaddingValues(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("¿Descartar captura?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Lo que escribiste se conservará como borrador si mantienes la captura.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton(
                        text = "Seguir editando",
                        onClick = { confirmDiscard = false },
                        modifier = Modifier.weight(1f)
                    )
                    BrishPrimaryButton(
                        text = "Descartar",
                        onClick = { viewModel.discard(); confirmDiscard = false; onDismiss() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val dismissThreshold = with(density) { 140.dp.toPx() }

    // Atrás predictivo: la hoja baja siguiendo el gesto; si se cancela, vuelve con resorte.
    PredictiveBackHandler(enabled = visible) { progress ->
        try {
            progress.collect { event -> dragOffset.snapTo(event.progress * dismissThreshold * 1.6f) }
            requestDismiss()
        } catch (e: kotlinx.coroutines.CancellationException) {
            scope.launch { dragOffset.animateTo(0f, BrishMotion.lively()) }
            throw e
        }
    }
    LaunchedEffect(visible) { if (visible) dragOffset.snapTo(0f) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(BrishScrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = requestDismiss)
                    .semantics { contentDescription = "Cerrar captura" }
            )
        }
        // Container transform: la hoja nace desde el botón ＋ central en la parte inferior.
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = if (reducedMotion) fadeIn(tween(MotionTokens.reducedMotionMs))
            else slideInVertically(MotionTokens.sheetSpring()) { it / 2 } +
                scaleIn(MotionTokens.sheetSpring(), initialScale = 0.22f, transformOrigin = TransformOrigin(0.5f, 0.94f)) +
                fadeIn(tween(MotionTokens.microMs)),
            exit = if (reducedMotion) fadeOut(tween(MotionTokens.reducedMotionMs))
            else slideOutVertically(tween(230, easing = MotionTokens.exit)) { it / 2 } +
                scaleOut(tween(210, easing = MotionTokens.exit), targetScale = 0.22f, transformOrigin = TransformOrigin(0.5f, 0.94f)) +
                fadeOut(tween(180))
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
                            if (dragOffset.value > dismissThreshold || velocity > 1800f) requestDismiss()
                            else dragOffset.animateTo(0f, BrishMotion.lively())
                        }
                    )
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 42.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                )
                // El contenido se desplaza y el botón Guardar queda siempre visible encima del teclado.
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                if (state.recovered) {
                    androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Borrador recuperado", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                            TextButton(onClick = { viewModel.discard() }) { Text("Descartar") }
                        }
                    }
                }
                val savedOffset = with(density) { 8.dp.roundToPx() }
                AnimatedVisibility(
                    visible = !state.recovered && state.savedIndicator,
                    enter = fadeIn(tween(MotionTokens.microMs)) + slideInVertically(tween(MotionTokens.microMs)) { -savedOffset },
                    exit = fadeOut(tween(MotionTokens.microMs))
                ) {
                    Text(
                        "Guardado ✓",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                CaptureHeader()
                TypeSelector(selected = state.forcedType, onSelect = viewModel::onTypeChange)
                CaptureField(
                    text = state.text,
                    spans = state.draft?.spans.orEmpty(),
                    onTextChange = viewModel::onTextChange,
                    visible = visible,
                    reducedMotion = reducedMotion,
                    onDone = { viewModel.save(onDismiss) }
                )
                state.draft?.let { PreviewCard(it) }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                if (state.draft == null) {
                    Text(
                        "«gym lun mie vie 5:30pm» → rutina en Agenda\n«idea: video de reparación» → nota\n«tomar agua 8 veces al día» → hábito",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
                }
                ActionsRow(
                    save = state.save,
                    onSpeech = { spoken -> viewModel.onTextChange(listOf(state.text, spoken).filter { it.isNotBlank() }.joinToString(" ")) },
                    onSave = { viewModel.save(onDismiss) }
                )
            }
        }
        // Chip que vuela hacia la pestaña correspondiente al guardar
        val savedDraft = state.draft
        if (visible && state.save == SaveState.Saved && savedDraft != null && !reducedMotion) {
            FlyingCaptureChip(
                draft = savedDraft,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun FlyingCaptureChip(
    draft: CaptureDraft,
    screenWidthPx: Float,
    screenHeightPx: Float,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(520, easing = MotionTokens.emphasized))
    }
    // Pestaña destino: 0=Hoy, 1=Agenda, 2=Notas, 3=Hábitos
    val today = remember { LocalDate.now() }
    val targetFractionX = when (draft.type) {
        CaptureType.NOTE -> 0.16f
        CaptureType.HABIT -> 0.37f
        CaptureType.TASK, CaptureType.EVENT -> {
            if (draft.repeatDays.isNotEmpty() || (draft.date != null && draft.date != today)) -0.16f
            else -0.37f
        }
    }
    val p = progress.value
    val startY = -screenHeightPx * 0.36f
    val endY = -36f
    val arcLift = kotlin.math.sin(p * Math.PI).toFloat() * (-68f)
    val currentX = screenWidthPx * targetFractionX * p
    val currentY = startY + (endY - startY) * p + arcLift
    val chipScale = 1f - 0.52f * p
    val chipAlpha = if (p < 0.78f) 1f else ((1f - p) / 0.22f).coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .graphicsLayer {
                translationX = currentX
                translationY = currentY
                scaleX = chipScale
                scaleY = chipScale
                alpha = chipAlpha
            }
            .brishShadow(CircleShape, elevation = 12.dp, tone = BrishGoldShadow)
            .clip(CircleShape)
            .background(BrishYellow)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = BrishInk, modifier = Modifier.size(16.dp))
        Text(
            text = "${draft.type.label}: ${draft.title.take(22)}",
            color = BrishInk,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            maxLines = 1
        )
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
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = BrishWarning, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun TypeSelector(selected: CaptureType?, onSelect: (CaptureType?) -> Unit) {
    val options = listOf<Pair<CaptureType?, String>>(null to "Auto") + CaptureType.entries.map { it to it.label }
    val selectedIdx = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(3.dp)
    ) {
        val itemWidth = maxWidth / options.size
        val thumbX by androidx.compose.animation.core.animateDpAsState(
            targetValue = itemWidth * selectedIdx,
            animationSpec = MotionTokens.sheetSpring(),
            label = "captureTypeThumb"
        )
        Box(
            Modifier
                .offset(x = thumbX)
                .width(itemWidth)
                .height(38.dp)
                .clip(CircleShape)
                .background(BrishYellow)
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { index, (type, label) ->
                val isSelected = index == selectedIdx
                val fg by animateColorAsState(if (isSelected) BrishInk else MaterialTheme.colorScheme.onSurfaceVariant, label = "typeFg")
                Box(
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.RadioButton) { onSelect(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CaptureField(
    text: String,
    spans: List<CaptureTokenSpan>,
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
    val glow by animateFloatAsState(if (thinking && !reducedMotion) 1f else 0f, tween(260), label = "glow")
    // El giro solo existe mientras se ve el brillo: una animación infinita permanente gasta batería.
    val angle = if (glow > 0f) {
        val spin = rememberInfiniteTransition(label = "border")
        spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "angle").value
    } else 0f
    val highlightAlpha by animateFloatAsState(
        targetValue = if (spans.isNotEmpty()) 1f else 0f,
        animationSpec = tween(if (reducedMotion) MotionTokens.reducedMotionMs else 220, easing = MotionTokens.standard),
        label = "spanFade"
    )
    val sweep = remember {
        Brush.sweepGradient(listOf(BrishYellow, BrishOrbCoral, BrishOrbBlue, BrishYellow))
    }
    val highlightTransformation = remember(spans, highlightAlpha) {
        VisualTransformation { raw ->
            if (spans.isEmpty() || highlightAlpha <= 0.01f) {
                TransformedText(raw, OffsetMapping.Identity)
            } else {
                val styled = buildAnnotatedString {
                    append(raw.text)
                    spans.forEach { span ->
                        val start = span.range.first.coerceIn(0, raw.text.length)
                        val end = (span.range.last + 1).coerceIn(start, raw.text.length)
                        if (start < end) {
                            val (bg, fg) = when (span.kind) {
                                CaptureTokenKind.AREA -> {
                                    val chip = areaChip(span.value.orEmpty())
                                    (chip.bg ?: BrishYellowSoft) to (chip.fg ?: BrishInk)
                                }
                                else -> BrishYellowSoft to BrishInk
                            }
                            addStyle(
                                SpanStyle(
                                    background = bg.copy(alpha = 0.88f * highlightAlpha),
                                    color = fg
                                ),
                                start = start,
                                end = end
                            )
                        }
                    }
                }
                TransformedText(styled, OffsetMapping.Identity)
            }
        }
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
                visualTransformation = highlightTransformation,
                textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 27.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 168.dp) // textos largos hacen scroll dentro del campo
                    .focusRequester(focus)
                    .semantics { contentDescription = "Qué quieres capturar" }
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
            .brishShadow(BrishShapes.pill, elevation = 14.dp, tone = BrishGoldShadow, strength = 1.4f)
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
