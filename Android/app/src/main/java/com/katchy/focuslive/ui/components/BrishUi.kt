package com.katchy.focuslive.ui.components

import com.katchy.focuslive.ui.theme.BrishAccentDark
import com.katchy.focuslive.ui.theme.BrishAccent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import com.katchy.focuslive.ui.theme.BrishButtonBrush
import com.katchy.focuslive.ui.theme.BrishGoldShadow
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishInset
import com.katchy.focuslive.ui.theme.BrishInsetSoft
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.BrishNight
import com.katchy.focuslive.ui.theme.BrishNightInset
import com.katchy.focuslive.ui.theme.BrishNightText
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishSparkAmber
import com.katchy.focuslive.ui.theme.BrishSparkMint
import com.katchy.focuslive.ui.theme.BrishSparkPink
import com.katchy.focuslive.ui.theme.BrishSparkSky
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.BrishYellowSoft
import com.katchy.focuslive.ui.theme.MotionTokens
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import com.katchy.focuslive.ui.util.bounceClick
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Señal que se incrementa cuando el usuario toca la pestaña ya activa para hacer scroll al inicio. */
val LocalTabReselectSignal = compositionLocalOf { 0 }

/** Colores e icono de cada área de vida. La categoría libre de tareas antiguas cae en Personal. */
data class AreaStyle(val name: String, val background: Color, val content: Color, val icon: ImageVector)

fun areaStyle(category: String?): AreaStyle {
    val c = category.orEmpty().lowercase()
    return when {
        listOf("trabajo", "work", "proyecto", "oficina").any { it in c } -> AreaStyle("Trabajo", AreaWorkBg, AreaWork, Icons.Rounded.Laptop)
        listOf("salud", "gym", "ejercicio", "deporte").any { it in c } -> AreaStyle("Salud", AreaHealthBg, AreaHealth, Icons.Rounded.FitnessCenter)
        listOf("hogar", "casa", "compras").any { it in c } -> AreaStyle("Hogar", AreaHomeBg, AreaHome, Icons.Rounded.Home)
        c.isBlank() || c == "general" -> AreaStyle("General", com.katchy.focuslive.ui.theme.BrishYellowSoftLight, com.katchy.focuslive.ui.theme.BrishInkLight, Icons.Rounded.Event)
        else -> AreaStyle(category!!.replaceFirstChar { it.uppercase() }, AreaPersonalBg, AreaPersonal, Icons.Rounded.Person)
    }
}

/** Cabecera grande de pestaña: fecha o subtítulo encima del título y acciones redondas a la derecha. */
@Composable
fun BrishScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            Row(verticalAlignment = Alignment.CenterVertically, content = leading)
            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
        }
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
fun BrishRoundButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, container: Color? = null, size: Dp = 40.dp) {
    Box(
        modifier
            .size(size)
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
                action,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.primary,
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
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (dark) BrishNightInset else BrishInset)
            .padding(2.dp)
    ) {
        val itemWidth = maxWidth / options.size
        val thumbX by animateDpAsState(itemWidth * selected, BrishMotion.soft(), label = "segThumb")
        Box(
            Modifier
                .offset(x = thumbX)
                .width(itemWidth)
                .fillMaxHeight()
                .brishShadow(RoundedCornerShape(8.dp), elevation = 4.dp, strength = if (dark) 0f else 1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (dark) Color(0xFF636366) else Color.White)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
            options.forEachIndexed { i, label ->
                val color by animateColorAsState(
                    if (i == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "segText"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(selected = i == selected, role = Role.Tab, onClick = { onSelect(i) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
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

/**
 * Celebración breve de hito (<= 700 ms): pop con overshoot + 6 partículas pequeñas en cascada
 * y vibración ligera cuando [trigger] aumenta. Respeta Reduce Motion.
 */
@Composable
fun BrishCelebrationBurst(
    active: Boolean,
    modifier: Modifier = Modifier,
    particleColor: Color = BrishYellow,
    content: @Composable () -> Unit
) {
    var previous by remember { mutableStateOf(active) }
    var trigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        if (active && !previous) {
            trigger++
        }
        previous = active
    }
    BrishCelebrationBurst(
        trigger = trigger,
        modifier = modifier,
        particleColor = particleColor,
        content = content
    )
}

@Composable
fun BrishCelebrationBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
    particleColor: Color = BrishYellow,
    content: @Composable () -> Unit
) {
    val reduced = rememberReducedMotion()
    val haptics = LocalHapticFeedback.current
    val progress = remember { Animatable(0f) }
    val popScale = remember { Animatable(1f) }

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (reduced) {
            popScale.snapTo(1f)
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        coroutineScope {
            launch {
                popScale.snapTo(0.84f)
                popScale.animateTo(
                    1f,
                    tween(
                        durationMillis = 360,
                        easing = MotionTokens.overshoot
                    )
                )
            }
            launch {
                progress.snapTo(0f)
                progress.animateTo(
                    1f,
                    tween(
                        durationMillis = MotionTokens.celebrationMs,
                        easing = MotionTokens.emphasized
                    )
                )
                progress.snapTo(0f)
            }
        }
    }

    val accentColors = remember(particleColor) {
        listOf(
            particleColor,
            BrishSparkMint,
            BrishSparkAmber,
            BrishSparkSky,
            particleColor,
            BrishSparkPink
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = popScale.value
                scaleY = popScale.value
            }
            .drawWithContent {
                drawContent()
                val p = progress.value
                if (!reduced && p > 0.01f && p < 0.99f) {
                    val baseRadius = size.minDimension * 0.42f
                    val maxTravel = size.minDimension * 0.65f + 12.dp.toPx()
                    for (i in 0 until 6) {
                        val stagger = i * 0.055f
                        val localP = ((p - stagger) / (1f - stagger)).coerceIn(0f, 1f)
                        if (localP > 0f && localP < 1f) {
                            val angleRad = Math.toRadians((i * 60.0) - 30.0)
                            val dist = baseRadius + maxTravel * localP
                            val cx = center.x + (cos(angleRad) * dist).toFloat()
                            val cy = center.y + (sin(angleRad) * dist).toFloat()
                            val dotRadius = (3.4.dp.toPx() * (1f - localP * 0.65f)).coerceAtLeast(1f)
                            val alpha = (1f - localP).coerceIn(0f, 1f)
                            drawCircle(
                                color = accentColors[i % accentColors.size].copy(alpha = alpha),
                                radius = dotRadius,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Botón primario píldora de Brish 2.0 (tinta negra en claro, crema en oscuro, o amarillo si [highlighted]).
 * Incluye escala con resorte al presionar y estado de carga opcional.
 */
@Composable
fun BrishPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    highlighted: Boolean = false,
    leadingIcon: ImageVector? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    height: Dp = 52.dp
) {
    val dark = isDarkTheme()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) MotionTokens.pressScale else 1f,
        animationSpec = MotionTokens.pressSpring(),
        label = "primaryBtnPress"
    )
    // Un solo estilo de acción principal: acento azul con texto blanco (highlighted se mantiene por compatibilidad).
    val defaultBg = containerColor ?: if (dark) BrishAccentDark else BrishAccent
    val defaultFg = contentColor ?: Color.White
    val brush = Brush.verticalGradient(listOf(defaultBg, defaultBg))

    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .clip(BrishShapes.button)
            .background(brush, alpha = if (enabled) 1f else 0.4f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = defaultFg,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = defaultFg,
                        modifier = Modifier.size(18.dp)
                    )
                }
                AnimatedContent(
                    targetState = text,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(110)) },
                    label = "primaryBtnText"
                ) { label ->
                    Text(
                        text = label,
                        color = defaultFg,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Botón secundario píldora de Brish 2.0: fondo tarjeta con borde sutil y resorte al tocar.
 */
@Composable
fun BrishSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    contentColor: Color? = null,
    height: Dp = 48.dp
) {
    val fg = contentColor ?: MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .height(height)
            .clip(BrishShapes.button)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.5f))
            .then(if (enabled) Modifier.bounceClick(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = fg.copy(alpha = if (enabled) 1f else 0.45f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                color = fg.copy(alpha = if (enabled) 1f else 0.45f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * Chip de selección con transición suave de fondo (resorte) y color de texto (160 ms).
 */
@Composable
fun BrishChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    activeContainer: Color? = null,
    activeContent: Color? = null
) {
    val dark = isDarkTheme()
    val targetBg = if (selected) {
        activeContainer ?: if (dark) BrishAccentDark else BrishAccent
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val targetFg = if (selected) {
        activeContent ?: Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bg by animateColorAsState(targetBg, BrishMotion.soft(), label = "chipBg")
    val fg by animateColorAsState(targetFg, tween(160, easing = MotionTokens.standard), label = "chipFg")

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

/**
 * Tarjeta unificada de Brish 2.0 con sombra en capas en claro y borde sutil en oscuro.
 */
@Composable
fun BrishCardSurface(
    modifier: Modifier = Modifier,
    shape: Shape = BrishShapes.card,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isDarkTheme()
    Column(
        modifier = modifier
            .brishCard(shape)
            .clip(shape)
            .then(
                if (dark) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                else Modifier
            )
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Campo de texto en píldora redondeada como en LoginScreen:
 * fondo hundido suave (`BrishInsetSoft` / `BrishNightInset`), sin subrayado y borde amarillo de 1.5 dp al enfocar.
 */
@Composable
fun BrishTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    shape: Shape = RoundedCornerShape(12.dp),
    textStyle: TextStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val dark = isDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(MotionTokens.microMs),
        label = "textFieldBorder"
    )
    val container = if (dark) BrishNightInset else BrishInsetSoft

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) } },
        label = label?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        shape = shape,
        interactionSource = interactionSource,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container.copy(alpha = 0.6f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, shape)
    )
}

/**
 * Estado vacío unificado con ilustración de [BrishArt], título, subtítulo y botón opcional.
 */
@Composable
fun BrishEmptyState(
    art: BrishArtKind,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    artSize: Dp = 128.dp,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .brishCard(BrishShapes.hero)
            .padding(horizontal = 24.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BrishArt(art, Modifier.size(artSize))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            BrishPrimaryButton(
                text = actionLabel,
                onClick = onAction,
                leadingIcon = actionIcon,
                height = 46.dp
            )
        }
    }
}

@Composable
fun BrishEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    illustration: @Composable () -> Unit,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .brishCard(BrishShapes.hero)
            .padding(horizontal = 24.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        illustration()
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            BrishPrimaryButton(
                text = actionLabel,
                onClick = onAction,
                leadingIcon = actionIcon,
                height = 46.dp
            )
        }
    }
}

/**
 * Contador tipo odómetro (digit roll): el número nuevo entra desde abajo cuando sube
 * y desde arriba cuando baja. Respeta [rememberReducedMotion].
 */
@Composable
fun BrishDigitRoll(
    value: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
    style: TextStyle = LocalTextStyle.current,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    BrishDigitRoll(
        text = "$value$suffix",
        style = style,
        color = color,
        modifier = modifier
    )
}

@Composable
fun BrishDigitRoll(
    text: String,
    style: TextStyle = LocalTextStyle.current,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val reduced = rememberReducedMotion()
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            if (reduced) {
                fadeIn(tween(MotionTokens.reducedMotionMs)) togetherWith fadeOut(tween(MotionTokens.reducedMotionMs))
            } else {
                val up = targetState >= initialState
                (slideInVertically(tween(220, easing = MotionTokens.overshoot)) { h -> if (up) h else -h } +
                    fadeIn(tween(180))) togetherWith
                    (slideOutVertically(tween(160, easing = MotionTokens.exit)) { h -> if (up) -h else h } +
                        fadeOut(tween(130)))
            }
        },
        modifier = modifier,
        label = "digitRollText"
    ) { current ->
        Text(
            text = current,
            style = style,
            color = color,
            maxLines = 1
        )
    }
}

/**
 * Contenedor pull-to-refresh con indicador amarillo propio de Brish.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrishPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                containerColor = BrishYellow,
                color = BrishInk,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        content = content
    )
}
