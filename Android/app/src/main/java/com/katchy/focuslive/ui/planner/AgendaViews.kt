package com.katchy.focuslive.ui.planner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.BrishScreenHeader
import com.katchy.focuslive.ui.components.BrishSegmented
import com.katchy.focuslive.ui.components.areaStyle
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.AreaHome
import com.katchy.focuslive.ui.theme.AreaPersonal
import com.katchy.focuslive.ui.theme.AreaWork
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishNow
import com.katchy.focuslive.ui.theme.BrishOnYellow
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import com.katchy.focuslive.ui.util.bounceClick
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val Spanish = Locale("es", "PE")
private val DayHour = 64.dp
private val WeekHour = 34.dp
private val LabelWidth = 50.dp
private val DayLetters = listOf("L", "M", "M", "J", "V", "S", "D")

private fun nowMinutes(): Int = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
private fun dayNumber(day: Long) = Calendar.getInstance().apply { timeInMillis = day }.get(Calendar.DAY_OF_MONTH)

/** Contenido de la pestaña Agenda: cabecera, vista Día con línea de tiempo y vista Semana. */
@Composable
internal fun AgendaContent(
    selectedDate: Long,
    tasks: List<Task>,
    week: List<Pair<Long, List<Task>>>,
    onSelectDate: (Long) -> Unit,
    onAdd: (start: Int?, end: Int?) -> Unit,
    onEdit: (Task) -> Unit,
    onToggle: (Task) -> Unit,
    onFocus: (Task) -> Unit,
    onMove: (Task, Int) -> Unit
) {
    var mode by rememberSaveable { mutableStateOf(0) }
    val today = startOfDay(System.currentTimeMillis())
    val weekDays = remember(selectedDate) { weekOf(selectedDate) }
    val reduced = rememberReducedMotion()

    Column(Modifier.fillMaxSize().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BrishScreenHeader("Agenda", Modifier.padding(horizontal = 18.dp)) {
            BrishSegmented(listOf("Día", "Semana"), mode, { mode = it }, Modifier.width(190.dp))
        }
        WeekNavigator(
            weekDays = weekDays,
            showToday = today !in weekDays,
            onShift = { days -> onSelectDate(Calendar.getInstance().apply { timeInMillis = selectedDate; add(Calendar.DAY_OF_YEAR, days) }.timeInMillis) },
            onToday = { onSelectDate(today) },
            onAdd = { onAdd(null, null) }
        )
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                if (reduced) fadeIn() togetherWith fadeOut()
                else {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(BrishMotion.soft()) { it / 3 * dir } + fadeIn()) togetherWith
                        (slideOutHorizontally(BrishMotion.soft()) { -it / 3 * dir } + fadeOut())
                }
            },
            label = "agendaMode"
        ) { m ->
            if (m == 0) {
                DayView(selectedDate, today, tasks, week, onSelectDate, onAdd, onEdit, onToggle, onFocus, onMove)
            } else {
                WeekView(weekDays, today, week, onOpenDay = { onSelectDate(it); mode = 0 }, onEdit = onEdit)
            }
        }
    }
}

@Composable
private fun WeekNavigator(weekDays: List<Long>, showToday: Boolean, onShift: (Int) -> Unit, onToday: () -> Unit, onAdd: () -> Unit) {
    val label = remember(weekDays) {
        val first = weekDays.first()
        val last = weekDays.last()
        val month = SimpleDateFormat("MMM", Spanish).format(Date(last)).replace(".", "")
        "${dayNumber(first)} – ${dayNumber(last)} $month"
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        BrishRoundButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Semana anterior", { onShift(-7) }, size = 40.dp)
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 10.dp))
        BrishRoundButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Semana siguiente", { onShift(7) }, size = 40.dp)
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(showToday, enter = fadeIn() + scaleIn(), exit = fadeOut()) {
            Text(
                "Hoy",
                fontWeight = FontWeight.ExtraBold,
                color = BrishInk,
                modifier = Modifier.padding(end = 8.dp).clip(CircleShape).background(BrishYellow).clickable(role = Role.Button, onClick = onToday)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
        BrishRoundButton(Icons.Rounded.Add, "Añadir en este día", onAdd, size = 40.dp, container = BrishYellow)
    }
}

@Composable
private fun DayView(
    selectedDate: Long,
    today: Long,
    tasks: List<Task>,
    week: List<Pair<Long, List<Task>>>,
    onSelectDate: (Long) -> Unit,
    onAdd: (Int?, Int?) -> Unit,
    onEdit: (Task) -> Unit,
    onToggle: (Task) -> Unit,
    onFocus: (Task) -> Unit,
    onMove: (Task, Int) -> Unit
) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val timed = tasks.filter { plannerTimeMinutes(it.time) != null }
    val untimed = tasks.filter { plannerTimeMinutes(it.time) == null }
    val placed = remember(timed) { layoutDay(timed) }
    val firstHour = minOf(7, (placed.minOfOrNull { it.start } ?: 7 * 60) / 60)
    val lastHour = maxOf(22, ((placed.maxOfOrNull { it.end } ?: 22 * 60) + 59) / 60).coerceAtMost(24)
    val isToday = selectedDate == today
    val gaps = remember(timed, isToday) {
        val from = if (isToday) maxOf(firstHour * 60, ((nowMinutes() + 14) / 15) * 15) else 8 * 60
        if (selectedDate < today) emptyList() else freeGaps(timed, from, minOf(lastHour, 22) * 60)
    }
    val busyMinutes = placed.filter { !it.task.isCompleted }.sumOf { it.end - it.start }
    val freeMinutes = gaps.sumOf { it.last + 1 - it.first }

    LaunchedEffect(selectedDate) {
        val target = if (isToday) nowMinutes() - 90 else 7 * 60
        scroll.scrollTo(with(density) { ((target - firstHour * 60).coerceAtLeast(0) / 60f * DayHour.toPx()).roundToInt() })
    }

    Column(Modifier.fillMaxSize()) {
        DayStrip(week, selectedDate, today, onSelectDate)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryChip("${tasks.size} ${if (tasks.size == 1) "bloque" else "bloques"}")
            if (busyMinutes > 0) SummaryChip("${busyMinutes / 60} h ${busyMinutes % 60} min ocupadas".replace(" 0 min", ""))
            if (freeMinutes > 0) SummaryChip("${freeMinutes / 60} h libres")
            if (tasks.isNotEmpty() && tasks.all { it.isCompleted }) SummaryChip("Todo hecho ✓")
        }
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(start = 14.dp, end = 18.dp, bottom = 24.dp)) {
            if (untimed.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 14.dp).brishCard().padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text("Sin hora", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 6.dp))
                    untimed.forEach { task -> UntimedRow(task, onToggle = { onToggle(task) }, onEdit = { onEdit(task) }) }
                }
            }
            if (tasks.isEmpty()) {
                Text(
                    "Día libre. Toca un hueco o ＋ para planificar algo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )
            }
            Timeline(
                firstHour = firstHour,
                lastHour = lastHour,
                placed = placed,
                gaps = gaps,
                nowMinute = if (isToday) nowMinutes() else null,
                onAdd = onAdd,
                onEdit = onEdit,
                onToggle = onToggle,
                onFocus = onFocus,
                onMove = onMove
            )
        }
    }
}

@Composable
private fun SummaryChip(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.brishShadow(CircleShape, elevation = 4.dp, strength = 0.6f).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun DayStrip(week: List<Pair<Long, List<Task>>>, selectedDate: Long, today: Long, onSelect: (Long) -> Unit) {
    val days = week.map { it.first }.ifEmpty { weekOf(selectedDate) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).brishCard().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEachIndexed { i, day ->
            val selected = day == selectedDate
            val hasTasks = week.getOrNull(i)?.second?.any { !it.isCompleted } == true
            val bg by androidx.compose.animation.animateColorAsState(if (selected) BrishYellow else Color.Transparent, BrishMotion.soft(), label = "daySel")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(bg)
                    .clickable(role = Role.Tab) { onSelect(day) }.padding(vertical = 8.dp)
                    .semantics { contentDescription = SimpleDateFormat("EEEE d", Spanish).format(Date(day)) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(DayLetters[i], fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) BrishOnYellow else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${dayNumber(day)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) BrishInk else if (day == today) AreaWork else MaterialTheme.colorScheme.onSurface
                )
                Box(Modifier.size(5.dp).clip(CircleShape).background(if (hasTasks) (if (selected) BrishInk else MaterialTheme.colorScheme.outline) else Color.Transparent))
            }
        }
    }
}

@Composable
private fun UntimedRow(task: Task, onToggle: () -> Unit, onEdit: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth().heightIn(min = 46.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CheckDot(task.isCompleted, task.title) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggle() }
        Text(
            task.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onEdit).padding(vertical = 6.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CheckDot(checked: Boolean, label: String, tint: Color = BrishInk, onClick: () -> Unit) {
    val bg by androidx.compose.animation.animateColorAsState(if (checked) tint else Color.Transparent, BrishMotion.lively(), label = "dot")
    Box(
        Modifier.size(26.dp).clip(CircleShape).background(bg).border(2.dp, if (checked) tint else tint.copy(alpha = 0.45f), CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { contentDescription = (if (checked) "Desmarcar " else "Marcar hecho ") + label },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(checked, enter = scaleIn(BrishMotion.lively()) + fadeIn(), exit = fadeOut()) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun Timeline(
    firstHour: Int,
    lastHour: Int,
    placed: List<PlacedBlock>,
    gaps: List<IntRange>,
    nowMinute: Int?,
    onAdd: (Int?, Int?) -> Unit,
    onEdit: (Task) -> Unit,
    onToggle: (Task) -> Unit,
    onFocus: (Task) -> Unit,
    onMove: (Task, Int) -> Unit
) {
    val hours = lastHour - firstHour
    val outline = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    fun yOf(minute: Int) = DayHour * ((minute - firstHour * 60) / 60f)

    BoxWithConstraints(Modifier.fillMaxWidth().height(DayHour * hours + 12.dp)) {
        val trackWidth = maxWidth - LabelWidth
        Canvas(Modifier.fillMaxSize()) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            for (h in 0..hours) {
                val y = (DayHour * h).toPx() + 6.dp.toPx()
                drawLine(outline, Offset(LabelWidth.toPx(), y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
            }
        }
        for (h in 0..hours) {
            Text(
                hhmm((firstHour + h) * 60),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                modifier = Modifier.offset(y = DayHour * h - 2.dp).width(LabelWidth - 6.dp),
                textAlign = TextAlign.Start
            )
        }
        gaps.forEach { gap ->
            val start = gap.first
            val end = minOf(gap.last + 1, start + 120)
            Column(
                Modifier
                    .offset(x = LabelWidth, y = yOf(start) + 8.dp)
                    .width(trackWidth)
                    .height(yOf(gap.last + 1) - yOf(start) - 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button) { onAdd(start, end) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = labelColor, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Libre", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Toca para llenar este hueco", fontSize = 12.sp, color = labelColor, maxLines = 1)
                    }
                    Text("${hhmm(gap.first)} – ${hhmm(gap.last + 1)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = labelColor)
                }
            }
        }
        placed.forEach { block ->
            key(block.task.id) {
                TimelineBlock(
                    block = block,
                    x = LabelWidth + trackWidth / block.lanes * block.lane,
                    y = yOf(block.start) + 8.dp,
                    width = trackWidth / block.lanes - if (block.lanes > 1) 4.dp else 0.dp,
                    height = (yOf(block.end) - yOf(block.start) - 4.dp).coerceAtLeast(40.dp),
                    onEdit = { onEdit(block.task) },
                    onToggle = { onToggle(block.task) },
                    onFocus = { onFocus(block.task) },
                    onMove = { onMove(block.task, it) }
                )
            }
        }
        if (nowMinute != null && nowMinute in firstHour * 60..lastHour * 60) {
            val y = yOf(nowMinute) + 6.dp
            Box(Modifier.offset(x = LabelWidth - 6.dp, y = y - 5.dp).size(10.dp).clip(CircleShape).background(BrishNow).zIndex(5f))
            Box(Modifier.offset(x = LabelWidth, y = y - 1.dp).width(trackWidth).height(2.dp).background(BrishNow).zIndex(5f))
            Text(
                hhmm(nowMinute),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.offset(y = y - 10.dp).clip(RoundedCornerShape(8.dp)).background(BrishNow).padding(horizontal = 5.dp, vertical = 2.dp).zIndex(6f)
            )
        }
    }
}

/** Bloque de la línea de tiempo. Mantén presionado y arrastra para moverlo; se ajusta cada 15 min. */
@Composable
private fun TimelineBlock(
    block: PlacedBlock,
    x: androidx.compose.ui.unit.Dp,
    y: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onFocus: () -> Unit,
    onMove: (Int) -> Unit
) {
    val task = block.task
    val area = areaStyle(task.category)
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val movable = task.repeatMode == "NONE" && !task.isCompleted
    var dragging by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val hourPx = with(density) { DayHour.toPx() }
    val snappedDelta = ((dragY / hourPx * 60) / 15f).roundToInt() * 15
    val lift by animateFloatAsState(if (dragging) 1.03f else 1f, BrishMotion.lively(), label = "lift")
    val elevation by animateDpAsState(if (dragging) 22.dp else 6.dp, BrishMotion.soft(), label = "elev")
    val shape = RoundedCornerShape(if (height < 56.dp) 14.dp else 20.dp)
    val compact = height < 56.dp

    Box(
        Modifier
            .offset { IntOffset(with(density) { x.roundToPx() }, with(density) { y.roundToPx() } + if (dragging) dragY.roundToInt() else 0) }
            .width(width)
            .height(height)
            .zIndex(if (dragging) 10f else 1f)
            .graphicsLayer { scaleX = lift; scaleY = lift }
            .brishShadow(shape, elevation = elevation, strength = 0.8f)
            .clip(shape)
            .background(area.background)
            .alpha(if (task.isCompleted) 0.6f else 1f)
            .then(
                if (movable) Modifier.pointerInput(task.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragging = true; dragY = 0f; haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onDragEnd = {
                            val delta = ((dragY / hourPx * 60) / 15f).roundToInt() * 15
                            dragging = false
                            dragY = 0f
                            if (delta != 0) onMove(block.start + delta)
                        },
                        onDragCancel = { dragging = false; dragY = 0f },
                        onDrag = { change, amount -> change.consume(); dragY += amount.y }
                    )
                } else Modifier
            )
            .clickable(role = Role.Button, onClick = onEdit)
            .semantics { contentDescription = "${task.title}, ${hhmm(block.start)} a ${hhmm(block.end)}" }
            .padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 10.dp)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!compact && width > 160.dp) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
                    Icon(area.icon, contentDescription = null, tint = area.content, modifier = Modifier.size(18.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    task.title,
                    fontSize = if (compact) 14.sp else 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = area.content,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
                val time = if (dragging) "${hhmm(block.start + snappedDelta)} – ${hhmm(block.end + snappedDelta)}" else "${hhmm(block.start)} – ${hhmm(block.end)}"
                Text(
                    buildString {
                        append(time)
                        if (!compact && task.subtasks.isNotEmpty()) append(" · ${task.subtasks.count { it.isCompleted }}/${task.subtasks.size}")
                        if (!compact && task.repeatMode != "NONE") append(" · se repite")
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = area.content.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
            if (!dragging) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheckDot(task.isCompleted, task.title, tint = area.content, onClick = onToggle)
                    if (!compact && !task.isCompleted && height >= 96.dp) {
                        Text(
                            "Enfocar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = area.content,
                            modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.65f))
                                .bounceClick(onClick = onFocus).padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekView(weekDays: List<Long>, today: Long, week: List<Pair<Long, List<Task>>>, onOpenDay: (Long) -> Unit, onEdit: (Task) -> Unit) {
    val first = 7
    val last = maxOf(22, (week.flatMap { it.second }.mapNotNull { blockEnd(it) }.maxOrNull() ?: 0).let { (it + 59) / 60 }).coerceAtMost(24)
    val hours = last - first
    val outline = MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().brishCard().padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 28.dp, bottom = 8.dp)) {
                weekDays.forEachIndexed { i, day ->
                    Text(
                        "${DayLetters[i]} ${dayNumber(day)}",
                        fontSize = 11.sp,
                        fontWeight = if (day == today) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (day == today) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { onOpenDay(day) }.padding(vertical = 4.dp)
                    )
                }
            }
            Row(Modifier.fillMaxWidth().height(WeekHour * hours)) {
                Box(Modifier.width(28.dp).fillMaxSize()) {
                    for (h in 0..hours step 2) {
                        Text("${first + h}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.offset(x = 4.dp, y = WeekHour * h - 6.dp))
                    }
                }
                weekDays.forEachIndexed { i, day ->
                    val dayTasks = week.getOrNull(i)?.second.orEmpty()
                    BoxWithConstraints(
                        Modifier.weight(1f).fillMaxSize()
                            .background(if (day == today) BrishYellow.copy(alpha = 0.12f) else Color.Transparent)
                    ) {
                        Canvas(Modifier.fillMaxSize()) { drawLine(outline, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.dp.toPx()) }
                        val colWidth = maxWidth
                        layoutDay(dayTasks).forEach { b ->
                            val area = areaStyle(b.task.category)
                            val top = WeekHour * ((b.start - first * 60).coerceAtLeast(0) / 60f)
                            val h = (WeekHour * ((b.end - b.start) / 60f)).coerceAtLeast(18.dp)
                            Box(
                                Modifier.offset(x = colWidth / b.lanes * b.lane + 2.dp, y = top)
                                    .width(colWidth / b.lanes - 4.dp).height(h - 2.dp)
                                    .clip(RoundedCornerShape(8.dp)).background(area.background)
                                    .alpha(if (b.task.isCompleted) 0.5f else 1f)
                                    .clickable(role = Role.Button) { onEdit(b.task) }
                                    .padding(horizontal = 2.dp, vertical = 3.dp)
                            ) {
                                Text(b.task.title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = area.content, maxLines = if (h > 40.dp) 3 else 1, overflow = TextOverflow.Clip, lineHeight = 11.sp)
                            }
                        }
                        if (day == today) {
                            val now = nowMinutes()
                            if (now in first * 60..last * 60) {
                                Box(Modifier.offset(y = WeekHour * ((now - first * 60) / 60f)).fillMaxWidth().height(2.dp).background(BrishNow))
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Trabajo" to AreaWork, "Salud" to AreaHealth, "Personal" to AreaPersonal, "Hogar" to AreaHome).forEach { (name, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Text(
            "Toca un día para verlo en detalle. Mantén presionado un bloque en la vista Día para moverlo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}
