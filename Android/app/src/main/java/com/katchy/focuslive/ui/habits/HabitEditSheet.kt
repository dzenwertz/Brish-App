package com.katchy.focuslive.ui.habits

import android.app.TimePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.ui.components.BrishDigitRoll
import com.katchy.focuslive.ui.components.BrishPrimaryButton
import com.katchy.focuslive.ui.components.BrishTextField
import com.katchy.focuslive.ui.components.rememberPersistentDraftText
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.BrishYellowDeep
import java.util.Calendar

// Lunes a domingo con las constantes de Calendar que usa Habit.daysOfWeek.
private val WeekDays = listOf(
    Calendar.MONDAY to "L", Calendar.TUESDAY to "M", Calendar.WEDNESDAY to "M", Calendar.THURSDAY to "J",
    Calendar.FRIDAY to "V", Calendar.SATURDAY to "S", Calendar.SUNDAY to "D"
)
private val DayNames = mapOf(
    Calendar.MONDAY to "lunes", Calendar.TUESDAY to "martes", Calendar.WEDNESDAY to "miércoles", Calendar.THURSDAY to "jueves",
    Calendar.FRIDAY to "viernes", Calendar.SATURDAY to "sábado", Calendar.SUNDAY to "domingo"
)

/** Edita nombre, días, veces al día y hora del aviso de un hábito. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditSheet(habit: Habit, onDismiss: () -> Unit, onSave: (Habit, () -> Unit) -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val titleDraft = rememberPersistentDraftText("habit:${habit.id}:title", HabitLogic.title(habit))
    val targetDraft = rememberPersistentDraftText("habit:${habit.id}:target", HabitLogic.target(habit).toString())
    val daysDraft = rememberPersistentDraftText("habit:${habit.id}:days", habit.daysOfWeek.ifEmpty { WeekDays.map { it.first } }.sorted().joinToString(","))
    val reminderDraft = rememberPersistentDraftText("habit:${habit.id}:reminder", habit.reminderTime.orEmpty())
    var title by titleDraft::value
    var targetText by targetDraft::value
    val target = targetText.toIntOrNull()?.coerceIn(1, 30) ?: 1
    var daysText by daysDraft::value
    val days = daysText.split(',').mapNotNull(String::toIntOrNull).toSet()
    var reminderText by reminderDraft::value
    val reminder = reminderText.takeIf(String::isNotBlank)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Editar hábito", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            if (titleDraft.recovered || targetDraft.recovered || daysDraft.recovered || reminderDraft.recovered) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Borrador recuperado", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { titleDraft.discard(); targetDraft.discard(); daysDraft.discard(); reminderDraft.discard() }) { Text("Descartar") }
                }
            }
            BrishTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                placeholder = "Ej. Leer 20 min",
                label = "Nombre"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Días", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    WeekDays.forEach { (day, letter) ->
                        val on = day in days
                        val bg by animateColorAsState(if (on) BrishYellow else MaterialTheme.colorScheme.surfaceVariant, label = "day")
                        Box(
                            Modifier.size(42.dp).clip(CircleShape).background(bg)
                                .clickable(role = Role.Checkbox) {
                                    val changed = if (on && days.size > 1) days - day else days + day
                                    daysText = changed.sorted().joinToString(",")
                                }
                                .semantics { contentDescription = (if (on) "Quitar " else "Añadir ") + DayNames.getValue(day) },
                            contentAlignment = Alignment.Center
                        ) { Text(letter, fontWeight = FontWeight.SemiBold, color = if (on) BrishInk else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Veces al día", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(if (target == 1) "Una vez: se marca con un toque" else "Se suma de una en una (como el agua)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Step("−", target > 1) { targetText = (target - 1).toString() }
                Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                    BrishDigitRoll(
                        text = "$target",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Step("+", target < 30) { targetText = (target + 1).toString() }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Aviso", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(reminder?.let { "Cada día elegido a las $it" } ?: "Sin aviso", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (reminder != null) {
                    Text(
                        "Cambiar",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clip(CircleShape).clickable(role = Role.Button) { pickTime(context, reminder) { reminderText = it } }.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Switch(
                    checked = reminder != null,
                    onCheckedChange = { on -> if (on) pickTime(context, reminder) { reminderText = it } else reminderText = "" },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = androidx.compose.ui.graphics.Color.White, checkedBorderColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }

            BrishPrimaryButton(
                text = "Guardar",
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        habit.copy(
                            title = title.trim() + if (target > 1) " · $target al día" else "",
                            daysOfWeek = days.sorted(),
                            reminderTime = reminder
                        )
                    ) {
                        titleDraft.clearAfterSave(); targetDraft.clearAfterSave(); daysDraft.clearAfterSave(); reminderDraft.clearAfterSave()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Eliminar hábito",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally).clip(CircleShape).clickable(role = Role.Button, onClick = onDelete).padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

private fun pickTime(context: android.content.Context, current: String?, onPicked: (String) -> Unit) {
    val (h, m) = current?.split(':')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }?.let { it[0] to it[1] } ?: (20 to 0)
    TimePickerDialog(context, { _, hour, minute -> onPicked(String.format(java.util.Locale.US, "%02d:%02d", hour, minute)) }, h, m, true).show()
}

@Composable
private fun Step(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f)) }
}
