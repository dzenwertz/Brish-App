package com.katchy.focuslive.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.katchy.focuslive.R

/**
 * Iconos grandes de Brish para bienvenida y estados vacíos: un símbolo del set propio
 * (design/icons, SVG → VectorDrawable) dentro de un círculo de acento suave.
 * Antes eran ilustraciones 3D en PNG que flotaban con una animación infinita.
 */
enum class BrishArtKind(@DrawableRes val icon: Int) {
    Sun(R.drawable.ic_b_sun),
    Capture(R.drawable.ic_b_idea),
    Calendar(R.drawable.ic_b_calendar),
    Plant(R.drawable.ic_b_leaf),
    Tasks(R.drawable.ic_b_list),
    Note(R.drawable.ic_b_note),
    NotePage(R.drawable.ic_b_note),
    Book(R.drawable.ic_b_book)
}

@Composable
fun BrishArt(kind: BrishArtKind, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize(0.72f)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(kind.icon), contentDescription = null, tint = accent, modifier = Modifier.fillMaxSize(0.5f))
        }
    }
}

/** Icono del set propio de Brish equivalente a un icono de Material; `null` si no hay equivalente. */
@DrawableRes
fun brishIconFor(icon: ImageVector): Int? = when (icon) {
    Icons.Rounded.WbSunny -> R.drawable.ic_b_sun
    Icons.Rounded.CalendarMonth -> R.drawable.ic_b_calendar
    Icons.Rounded.Description -> R.drawable.ic_b_note
    Icons.Rounded.BarChart -> R.drawable.ic_b_habits
    Icons.Rounded.WaterDrop -> R.drawable.ic_b_water
    Icons.Rounded.FitnessCenter -> R.drawable.ic_b_dumbbell
    Icons.Rounded.Bedtime -> R.drawable.ic_b_moon
    Icons.AutoMirrored.Rounded.MenuBook -> R.drawable.ic_b_book
    Icons.Rounded.Restaurant -> R.drawable.ic_b_food
    Icons.Rounded.Lightbulb -> R.drawable.ic_b_idea
    Icons.Rounded.Lock -> R.drawable.ic_b_lock
    Icons.Rounded.Laptop -> R.drawable.ic_b_work
    Icons.Rounded.Event -> R.drawable.ic_b_event
    Icons.Rounded.Home -> R.drawable.ic_b_home
    Icons.Rounded.Style -> R.drawable.ic_b_list
    Icons.Rounded.Bolt -> R.drawable.ic_b_bolt
    Icons.Rounded.CloudDone -> R.drawable.ic_b_cloud
    Icons.Rounded.Person -> R.drawable.ic_b_person
    Icons.Rounded.CheckCircle -> R.drawable.ic_b_check
    Icons.Rounded.SelfImprovement -> R.drawable.ic_b_leaf
    Icons.Rounded.Timer -> R.drawable.ic_b_focus
    else -> null
}

/** Icono de Brish: el símbolo del set propio si existe y, si no, el icono de Material; siempre teñido. */
@Composable
fun BrishGlyph(icon: ImageVector, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    val own = brishIconFor(icon)
    if (own != null) {
        Icon(painterResource(own), contentDescription = null, tint = tint, modifier = modifier.size(size))
    } else {
        Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(size))
    }
}
