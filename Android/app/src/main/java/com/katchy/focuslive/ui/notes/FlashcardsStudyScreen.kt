package com.katchy.focuslive.ui.notes

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SentimentSatisfiedAlt
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.FlashcardItem
import com.katchy.focuslive.ui.components.BrishArt
import com.katchy.focuslive.ui.components.BrishCardSurface
import com.katchy.focuslive.ui.components.BrishEmptyState
import com.katchy.focuslive.ui.components.BrishPrimaryButton
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.BrishSecondaryButton
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.AreaWork
import com.katchy.focuslive.ui.theme.BrishLeitnerBox1
import com.katchy.focuslive.ui.theme.BrishLeitnerBox2
import com.katchy.focuslive.ui.theme.BrishLeitnerBox3
import com.katchy.focuslive.ui.theme.BrishLeitnerBox4
import com.katchy.focuslive.ui.theme.BrishLeitnerBox5
import com.katchy.focuslive.ui.theme.BrishNow
import com.katchy.focuslive.ui.theme.BrishWarning
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.util.InAppNotificationBus
import com.katchy.focuslive.ui.util.bounceClick
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun FlashcardsStudyScreen(
    noteId: String,
    onBack: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    val error by viewModel.error.collectAsState()
    val busy by viewModel.busy.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }

    error?.let { message ->
        SmoothDialog(onDismissRequest = viewModel::dismissError) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("No se pudo guardar", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BrishPrimaryButton("Aceptar", onClick = viewModel::dismissError, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    val note = remember(notes, noteId) {
        notes.find { it.id == noteId }
    }

    if (note == null) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrishEmptyState(
                art = com.katchy.focuslive.ui.components.BrishArtKind.NotePage,
                title = "El apunte no está disponible",
                subtitle = "No pudimos encontrar este apunte.",
                actionLabel = "Volver a Notas",
                onAction = onBack
            )
        }
        return
    }

    val allCards = note.flashcards
    if (confirmClear) {
        SmoothDialog(onDismissRequest = { confirmClear = false }) {
            BrishCardSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("Eliminar tarjetas", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("Se eliminarán las tarjetas de este apunte. El texto del apunte se conserva.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrishSecondaryButton("Cancelar", onClick = { confirmClear = false }, modifier = Modifier.weight(1f))
                    BrishPrimaryButton(
                        text = "Eliminar",
                        onClick = { viewModel.clearFlashcards(note) { confirmClear = false; onBack() } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    val now = System.currentTimeMillis()

    var reviewAllMode by remember { mutableStateOf(false) }
    var sessionNumber by remember { mutableIntStateOf(0) }
    val sessionIds = remember(noteId, reviewAllMode, sessionNumber) {
        allCards.filter { reviewAllMode || it.nextReviewDate <= now }.map { it.id }
    }
    val activeCards = sessionIds.mapNotNull { id -> allCards.find { it.id == id } }

    var currentIndex by remember { mutableIntStateOf(0) }
    var showResults by remember { mutableStateOf(false) }

    val stats = remember(allCards) {
        val boxes = IntArray(5) { 0 }
        allCards.forEach { card ->
            val boxIdx = (card.box - 1).coerceIn(0, 4)
            boxes[boxIdx]++
        }
        boxes
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BrishRoundButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    description = "Volver",
                    onClick = onBack
                )

                Text(
                    text = note.title.ifBlank { "Repaso" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )

                BrishRoundButton(
                    icon = Icons.Rounded.DeleteSweep,
                    description = "Limpiar mazo",
                    onClick = { confirmClear = true }
                )
            }

            LeitnerStatsView(stats = stats)

            Spacer(modifier = Modifier.height(14.dp))

            if (allCards.isEmpty()) {
                EmptyDeckView(onBack = onBack)
            } else if (activeCards.isEmpty() && !showResults) {
                UpToDateView(
                    onReviewAll = { reviewAllMode = true },
                    onBack = onBack
                )
            } else if (showResults || currentIndex >= activeCards.size) {
                StudySessionSuccessView(
                    totalCards = activeCards.size,
                    onRestart = {
                        currentIndex = 0
                        showResults = false
                        sessionNumber++
                    },
                    onBack = onBack
                )
            } else {
                val currentCard = activeCards[currentIndex]
                var isFlipped by remember(currentIndex) { mutableStateOf(false) }

                val configuration = LocalConfiguration.current
                val density = LocalDensity.current
                val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

                val offsetX = remember(currentCard.id) { Animatable(0f) }
                val offsetY = remember(currentCard.id) { Animatable(0f) }
                LaunchedEffect(error) {
                    if (error != null) { offsetX.snapTo(0f); offsetY.snapTo(0f) }
                }
                fun saveAnswer(wasCorrect: Boolean) {
                    viewModel.updateFlashcardLeitner(note, currentCard.id, wasCorrect) {
                        if (currentIndex + 1 >= activeCards.size) showResults = true
                        else { currentIndex++; isFlipped = false }
                    }
                }

                // Session progress bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentIndex > 0) {
                        BrishRoundButton(
                            icon = Icons.Rounded.ChevronLeft,
                            description = "Tarjeta anterior",
                            onClick = {
                                currentIndex--
                                isFlipped = false
                            },
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Spacer(modifier = Modifier.width(44.dp))
                    }

                    Text(
                        text = "Tarjeta ${currentIndex + 1} de ${activeCards.size}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    LinearProgressIndicator(
                        progress = { currentIndex.toFloat() / activeCards.size.coerceAtLeast(1) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = BrishYellow,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "Caja ${currentCard.box}",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Flippable 3D Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FlippableCard(
                        card = currentCard,
                        isFlipped = isFlipped,
                        onFlip = { isFlipped = !isFlipped },
                        onOpenUrl = { url ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                InAppNotificationBus.post("No se pudo abrir el enlace")
                            }
                        },
                        offsetX = offsetX,
                        offsetY = offsetY,
                        screenWidthPx = screenWidthPx,
                        onSwipeDismiss = { wasCorrect ->
                            saveAnswer(wasCorrect)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Control feedback buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isFlipped) {
                        BrishPrimaryButton(
                            text = "Revelar respuesta",
                            leadingIcon = Icons.Rounded.Flip,
                            onClick = { isFlipped = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        BrishSecondaryButton(
                            text = "Repasar luego",
                            leadingIcon = Icons.Rounded.Close,
                            enabled = !busy,
                            onClick = { saveAnswer(false) },
                            modifier = Modifier.weight(1f)
                        )
                        BrishPrimaryButton(
                            text = "Entendido",
                            leadingIcon = Icons.Rounded.Check,
                            enabled = !busy,
                            onClick = { saveAnswer(true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeitnerStatsView(stats: IntArray) {
    val boxColors = listOf(BrishLeitnerBox1, BrishLeitnerBox2, BrishLeitnerBox3, BrishLeitnerBox4, BrishLeitnerBox5)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .brishCard(RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (i in 0..4) {
            val tone = boxColors[i]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Caja ${i + 1}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tone.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stats[i].toString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tone
                    )
                }
            }
        }
    }
}

@Composable
fun FlippableCard(
    card: FlashcardItem,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onOpenUrl: (String) -> Unit,
    offsetX: Animatable<Float, *>,
    offsetY: Animatable<Float, *>,
    screenWidthPx: Float,
    onSwipeDismiss: (Boolean) -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 250f
        ),
        label = "cardFlip"
    )

    val isFront = rotation <= 90f
    val blurRadius = if (rotation in 80f..100f) 6.dp else 0.dp
    val coroutineScope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .fillMaxHeight(0.86f)
            .pointerInput(card.id) {
                detectDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    },
                    onDragEnd = {
                        val velocityX = velocityTracker.calculateVelocity().x
                        val velocityY = velocityTracker.calculateVelocity().y
                        val distanceX = offsetX.value
                        val swipeThreshold = screenWidthPx / 3.5f

                        coroutineScope.launch {
                            if (distanceX > swipeThreshold || velocityX > 1000f) {
                                launch { offsetY.animateTo(offsetY.value + velocityY * 0.2f, tween(200)) }
                                offsetX.animateTo(screenWidthPx, tween(200, easing = EaseOutQuart))
                                onSwipeDismiss(true)
                            } else if (distanceX < -swipeThreshold || velocityX < -1000f) {
                                launch { offsetY.animateTo(offsetY.value + velocityY * 0.2f, tween(200)) }
                                offsetX.animateTo(-screenWidthPx, tween(200, easing = EaseOutQuart))
                                onSwipeDismiss(false)
                            } else {
                                launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
                                launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
                            launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
                        }
                    }
                )
            }
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = offsetX.value / 25f
                rotationY = rotation
                cameraDistance = 16f * density
            }
            .blur(blurRadius)
            .brishCard(shape)
            .clip(shape)
            .bounceClick { onFlip() }
            .padding(24.dp)
    ) {
        val swipeThreshold = screenWidthPx / 3.5f
        val swipeProgress = if (swipeThreshold > 0) (offsetX.value / swipeThreshold).coerceIn(-1f, 1f) else 0f

        if (isFront) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.HelpOutline,
                    contentDescription = null,
                    tint = BrishWarning,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = card.question,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Toca para voltear",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = BrishWarning,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = card.answer,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        )
                    }
                }

                if (card.sourceWikiUrl != null || card.sourceVideoUrl != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Text(
                            text = "Aprende más en la red:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            card.sourceWikiUrl?.let { wiki ->
                                Button(
                                    onClick = { onOpenUrl(wiki) },
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .height(40.dp)
                                        .bounceClick(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MenuBook,
                                        contentDescription = null,
                                        tint = AreaWork,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Wikipedia", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            card.sourceVideoUrl?.let { video ->
                                Button(
                                    onClick = { onOpenUrl(video) },
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .height(40.dp)
                                        .bounceClick(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = BrishNow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("YouTube", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (swipeProgress > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .graphicsLayer {
                        rotationZ = -15f
                        alpha = swipeProgress.coerceIn(0f, 1f)
                    }
                    .border(3.dp, AreaHealth, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ENTENDIDO",
                    color = AreaHealth,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        if (swipeProgress < -0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .graphicsLayer {
                        rotationZ = 15f
                        alpha = abs(swipeProgress).coerceIn(0f, 1f)
                    }
                    .border(3.dp, BrishNow, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "REPASAR",
                    color = BrishNow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun EmptyDeckView(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrishEmptyState(
            title = "Este mazo está vacío",
            subtitle = "Añade tarjetas de repaso desde el editor de la nota para estudiarlas aquí.",
            illustration = { Icon(Icons.Rounded.Layers, contentDescription = null, tint = BrishWarning, modifier = Modifier.size(64.dp)) },
            actionLabel = "Volver a Notas",
            onAction = onBack
        )
    }
}

@Composable
fun UpToDateView(onReviewAll: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrishCardSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SentimentSatisfiedAlt,
                    contentDescription = null,
                    tint = AreaHealth,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "¡Estás al día!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "No tienes tarjetas pendientes de repasar para hoy según el método Leitner.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                BrishPrimaryButton(
                    text = "Repasar todas las tarjetas",
                    onClick = onReviewAll,
                    modifier = Modifier.fillMaxWidth()
                )
                BrishSecondaryButton(
                    text = "Volver",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StudySessionSuccessView(
    totalCards: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrishCardSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = BrishWarning,
                    modifier = Modifier.size(76.dp)
                )
                Text(
                    text = "¡Sesión completada!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Has repasado $totalCards tarjetas de memoria.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                BrishPrimaryButton(
                    text = "Reiniciar repaso",
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth()
                )
                BrishSecondaryButton(
                    text = "Salir a Notas",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
