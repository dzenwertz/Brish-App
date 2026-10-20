package com.katchy.focuslive.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.ui.components.BrishGlyph
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.AreaHealthBg
import com.katchy.focuslive.ui.theme.AreaPersonal
import com.katchy.focuslive.ui.theme.AreaPersonalBg
import com.katchy.focuslive.ui.theme.AreaWork
import com.katchy.focuslive.ui.theme.AreaWorkBg
import com.katchy.focuslive.ui.theme.BrishHeroBrush
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishOnYellow
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.BrishYellowSoft
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import java.time.LocalDate

private val DayLetters = mapOf(1 to "L", 2 to "M", 3 to "M", 4 to "J", 5 to "V", 6 to "S", 7 to "D")

@Composable
fun HabitsScreen(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val habits by viewModel.habits.collectAsState()
    val error by viewModel.error.collectAsState()
    var toDelete by remember { mutableStateOf<Habit?>(null) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    val today = LocalDate.now()
    val list = habits.orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 24.dp + com.katchy.focuslive.ui.main.LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hábitos", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                Box(
                    Modifier.height(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                        .clickable(role = Role.Button, onClick = onCreate).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) { Text("＋ Nuevo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
            }
        }
        if (habits != null && list.isEmpty()) {
            item { EmptyHabits(onCreate) }
        } else if (list.isNotEmpty()) {
            item { WeekSummary(list, today) }
            items(list, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    today = today,
                    onTap = { viewModel.tap(habit) },
                    onLongPress = { toDelete = habit },
                    onEdit = { editing = habit },
                    modifier = Modifier.animateItem()
                )
            }
            item {
                Text(
                    "Toca un hábito para editarlo; mantenlo presionado para eliminarlo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) } }
    }

    editing?.let { habit ->
        HabitEditSheet(habit, onDismiss = { editing = null }, onSave = { viewModel.save(it) { editing = null } }, onDelete = { editing = null; toDelete = habit })
    }

    toDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Eliminar «${HabitLogic.title(habit)}»?") },
            text = { Text("Se borra el hábito y su historial.") },
            confirmButton = { TextButton(onClick = { viewModel.delete(habit); toDelete = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun EmptyHabits(onCreate: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .brishShadow(BrishShapes.hero, strength = 1.4f)
            .clip(BrishShapes.hero)
            .background(BrishHeroBrush)
            .brishEdgeHighlight()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Crea tu primer hábito", style = MaterialTheme.typography.titleLarge, color = BrishInk)
        Text(
            "Escríbelo como lo dirías: «leer 20 min todos los días» o «tomar agua 8 veces al día». Sin rachas ni puntos: solo tu meta de la semana.",
            style = MaterialTheme.typography.bodyMedium,
            color = BrishOnYellow
        )
        Box(
            Modifier.height(48.dp).clip(CircleShape).background(BrishInk).clickable(role = Role.Button, onClick = onCreate).padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) { Text("Crear hábito", color = Color.White, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable
private fun WeekSummary(habits: List<Habit>, today: LocalDate) {
    val percent = HabitLogic.weekPercent(habits, today)
    val previous = HabitLogic.weekPercent(habits, today.minusDays(7))
    val shown by animateFloatAsState((percent ?: 0) / 100f, BrishMotion.soft(), label = "weekPercent")
    Column(
        Modifier
            .fillMaxWidth()
            .brishShadow(BrishShapes.hero, strength = 1.4f)
            .clip(BrishShapes.hero)
            .background(BrishHeroBrush)
            .brishEdgeHighlight()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Cumplido en los últimos 7 días", style = MaterialTheme.typography.labelMedium, color = BrishOnYellow, fontWeight = FontWeight.Bold)
        Text("${(shown * 100).toInt()}%", style = MaterialTheme.typography.displayMedium, color = BrishInk)
        Box(Modifier.fillMaxWidth(0.7f).height(10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.6f))) {
            Box(Modifier.fillMaxWidth(shown).height(10.dp).clip(CircleShape).background(BrishInk))
        }
        val comparison = when {
            percent == null -> "Aún no hay días programados."
            previous == null -> "Tu primera semana con Brish."
            percent > previous -> "Mejor que la semana anterior ($previous%)."
            percent == previous -> "Igual que la semana anterior."
            else -> "La semana anterior: $previous%."
        }
        Text(comparison, style = MaterialTheme.typography.bodyMedium, color = BrishOnYellow)
    }
}

private fun habitIcon(title: String): Triple<ImageVector, Color, Color> {
    val t = title.lowercase()
    return when {
        listOf("leer", "libro", "lectura", "estudiar").any { it in t } -> Triple(Icons.AutoMirrored.Rounded.MenuBook, AreaHealthBg, AreaHealth)
        listOf("agua", "beber", "tomar").any { it in t } -> Triple(Icons.Rounded.WaterDrop, AreaWorkBg, AreaWork)
        listOf("gym", "gimnasio", "pesas", "entrenar").any { it in t } -> Triple(Icons.Rounded.FitnessCenter, AreaHealthBg, AreaHealth)
        listOf("correr", "caminar", "bici").any { it in t } -> Triple(Icons.Rounded.DirectionsRun, AreaHealthBg, AreaHealth)
        listOf("dormir", "sueño", "acostar").any { it in t } -> Triple(Icons.Rounded.Bedtime, AreaPersonalBg, AreaPersonal)
        listOf("meditar", "respirar", "yoga").any { it in t } -> Triple(Icons.Rounded.SelfImprovement, AreaPersonalBg, AreaPersonal)
        else -> Triple(Icons.Rounded.CheckCircle, BrishYellowSoft, BrishInk)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCard(habit: Habit, today: LocalDate, onTap: () -> Unit, onLongPress: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val title = HabitLogic.title(habit)
    val target = HabitLogic.target(habit)
    val count = HabitLogic.countOn(habit, today)
    val doneToday = count >= target
    val scheduled = HabitLogic.scheduledThisWeek(habit, today)
    val doneWeek = HabitLogic.doneThisWeek(habit, today)
    val (icon, iconBg, iconFg) = habitIcon(title)
    val haptics = LocalHapticFeedback.current

    Column(
        modifier
            .fillMaxWidth()
            .brishCard()
            .clip(BrishShapes.card)
            .combinedClickable(onClick = onEdit, onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPress()
            })
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                BrishGlyph(icon, iconFg, 22.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (target > 1) "$target al día · hoy $count" else "Meta $scheduled de 7 · llevas $doneWeek",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (target > 1) {
                Box(
                    Modifier.height(46.dp).clip(CircleShape).background(if (doneToday) AreaHealthBg else AreaWorkBg)
                        .clickable(role = Role.Button) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTap()
                        }
                        .semantics { contentDescription = if (doneToday) "Reiniciar $title de hoy" else "Sumar uno a $title" }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (doneToday) "✓" else "+1", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (doneToday) AreaHealth else AreaWork)
                }
            } else {
                LiquidCheck(done = doneToday, label = title, onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTap()
                })
            }
        }
        if (target > 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(target.coerceAtMost(12)) { i ->
                    val filled by animateColorAsState(if (i < count) AreaWork else AreaWorkBg, BrishMotion.lively(), label = "segment")
                    Box(Modifier.weight(1f).height(12.dp).clip(CircleShape).background(filled))
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HabitLogic.lastWeek(today).forEach { day ->
                    val scheduledDay = HabitLogic.scheduledOn(habit, day)
                    val done = HabitLogic.doneOn(habit, day)
                    val isToday = day == today
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            DayLetters.getValue(day.dayOfWeek.value),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            Modifier
                                .size(26.dp)
                                .then(if (isToday) Modifier.border(2.dp, BrishYellow, CircleShape).padding(3.dp) else Modifier)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        done -> AreaHealth
                                        scheduledDay -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> Color.Transparent
                                    }
                                )
                                .then(if (!scheduledDay && !done) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) else Modifier)
                        )
                    }
                }
            }
        }
    }
}

/** Botón que se llena de verde desde abajo, como un líquido, al marcar el hábito. */
@Composable
private fun LiquidCheck(done: Boolean, label: String, onClick: () -> Unit) {
    val fill by animateFloatAsState(if (done) 1f else 0f, BrishMotion.soft(), label = "liquid")
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .border(2.dp, if (done) AreaHealth else outline, CircleShape)
            .drawBehind {
                val h = size.height * fill
                drawRect(AreaHealth, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
            }
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics {
                contentDescription = "Hecho hoy: $label"
                stateDescription = if (done) "Marcado" else "Sin marcar"
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(done, enter = scaleIn(BrishMotion.lively()) + fadeIn()) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}
