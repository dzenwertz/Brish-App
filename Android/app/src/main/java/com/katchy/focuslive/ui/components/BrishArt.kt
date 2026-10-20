package com.katchy.focuslive.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katchy.focuslive.R
import com.katchy.focuslive.ui.theme.rememberReducedMotion

/** Ilustraciones 3D de Brish (generadas con Codex) para pantallas de bienvenida, vacías y de cierre. */
enum class BrishArtKind(@DrawableRes val drawable: Int) {
    Sun(R.drawable.art_sun),
    Capture(R.drawable.art_idea),
    Calendar(R.drawable.art_calendar),
    Plant(R.drawable.art_foliage_right)
}

@Composable
fun BrishArt(kind: BrishArtKind, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val float = rememberInfiniteTransition(label = "artFloat")
    val bob by float.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        // Halo cálido detrás de la ilustración.
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(Color(0x55F8D468), Color(0x00F8D468)), center = center, radius = r), r, center)
        }
        Image(
            painterResource(kind.drawable),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .graphicsLayer { translationY = -size.height * 0.03f * bob }
        )
    }
}

/** Ilustración que sustituye a ciertos iconos planos; `null` si ese icono no tiene versión 3D. */
@DrawableRes
fun brishArtFor(icon: ImageVector): Int? = when (icon) {
    Icons.Rounded.WaterDrop -> R.drawable.art_water
    Icons.Rounded.FitnessCenter -> R.drawable.art_dumbbell
    Icons.Rounded.Bedtime -> R.drawable.art_moon
    Icons.AutoMirrored.Rounded.MenuBook -> R.drawable.art_book
    Icons.Rounded.Restaurant -> R.drawable.art_food
    Icons.Rounded.Lightbulb -> R.drawable.art_idea
    Icons.Rounded.Lock -> R.drawable.art_lock
    Icons.Rounded.Laptop -> R.drawable.art_laptop
    Icons.Rounded.Event -> R.drawable.art_calendar
    Icons.Rounded.Home -> R.drawable.art_groceries
    Icons.Rounded.Style -> R.drawable.art_list
    Icons.Rounded.WbSunny -> R.drawable.art_sun
    Icons.Rounded.Bolt -> R.drawable.art_bolt
    Icons.Rounded.CloudDone -> R.drawable.art_cloud
    else -> null
}

/** Icono de Brish: la versión 3D si existe y, si no, el icono plano teñido. */
@Composable
fun BrishGlyph(icon: ImageVector, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    val art = brishArtFor(icon)
    if (art != null) {
        Image(painterResource(art), contentDescription = null, modifier = modifier.size(size * 1.35f))
    } else {
        Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(size))
    }
}
