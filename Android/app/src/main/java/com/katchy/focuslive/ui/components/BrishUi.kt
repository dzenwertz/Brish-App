package com.katchy.focuslive.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.AreaHealthBg
import com.katchy.focuslive.ui.theme.AreaHome
import com.katchy.focuslive.ui.theme.AreaHomeBg
import com.katchy.focuslive.ui.theme.AreaPersonal
import com.katchy.focuslive.ui.theme.AreaPersonalBg
import com.katchy.focuslive.ui.theme.AreaWork
import com.katchy.focuslive.ui.theme.AreaWorkBg
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishInset
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishNightInset
import com.katchy.focuslive.ui.theme.BrishNightText
import com.katchy.focuslive.ui.theme.BrishYellowSoft
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.bounceClick

/** Colores e icono de cada área de vida. La categoría libre de tareas antiguas cae en Personal. */
data class AreaStyle(val name: String, val background: Color, val content: Color, val icon: ImageVector)

fun areaStyle(category: String?): AreaStyle {
    val c = category.orEmpty().lowercase()
    return when {
        listOf("trabajo", "work", "proyecto", "oficina").any { it in c } -> AreaStyle("Trabajo", AreaWorkBg, AreaWork, Icons.Rounded.Laptop)
        listOf("salud", "gym", "ejercicio", "deporte").any { it in c } -> AreaStyle("Salud", AreaHealthBg, AreaHealth, Icons.Rounded.FitnessCenter)
        listOf("hogar", "casa", "compras").any { it in c } -> AreaStyle("Hogar", AreaHomeBg, AreaHome, Icons.Rounded.Home)
        c.isBlank() || c == "general" -> AreaStyle("General", BrishYellowSoft, BrishInk, Icons.Rounded.Event)
        else -> AreaStyle(category!!.replaceFirstChar { it.uppercase() }, AreaPersonalBg, AreaPersonal, Icons.Rounded.Person)
    }
}

/** Cabecera grande de pestaña: fecha o subtítulo encima del título y acciones redondas a la derecha. */
@Composable
fun BrishScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            overline?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                title,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Botón redondo de 46 dp con sombra suave (menú, ordenar, volver). */
@Composable
fun BrishRoundButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, container: Color? = null, size: Dp = 46.dp) {
    Box(
        modifier
            .size(size)
            .brishShadow(CircleShape, elevation = 8.dp, strength = if (isDarkTheme()) 0f else 0.8f)
            .clip(CircleShape)
            .background(container ?: MaterialTheme.colorScheme.surface)
            .bounceClick(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (container != null) BrishInk else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

/** Título de sección con acción opcional a la derecha («Ver agenda ›»). */
@Composable
fun BrishSectionTitle(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        if (action != null) {
            Text(
                "$action ›",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

/** Píldora de área o dato («Trabajo», «2 de 4 subtareas»). */
@Composable
fun BrishPill(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).background(background).padding(horizontal = 11.dp, vertical = 5.dp)) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = content, maxLines = 1)
    }
}

/** Selector segmentado con un pulgar oscuro que se desliza con resorte entre opciones. */
@Composable
fun BrishSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val dark = isDarkTheme()
    BoxWithConstraints(
        modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(if (dark) BrishNightInset else BrishInset)
            .padding(4.dp)
    ) {
        val itemWidth = maxWidth / options.size
        val thumbX by animateDpAsState(itemWidth * selected, BrishMotion.soft(), label = "segThumb")
        Box(
            Modifier
                .offset(x = thumbX)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(if (dark) BrishNightText else BrishInk)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
            options.forEachIndexed { i, label ->
                val color by animateColorAsState(
                    if (i == selected) (if (dark) BrishInk else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "segText"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .selectable(selected = i == selected, role = Role.Tab, onClick = { onSelect(i) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

/** Icono dentro de una baldosa redondeada de color (filas de listas). */
@Composable
fun BrishIconTile(icon: ImageVector, background: Color, content: Color, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.34f)).background(background), contentAlignment = Alignment.Center) {
        BrishGlyph(icon, content, size * 0.52f)
    }
}
