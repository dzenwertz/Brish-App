package com.katchy.focuslive.ui.notes

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.katchy.focuslive.ui.util.InAppNotificationBus
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.FlashcardItem
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.ui.components.CosmicBackground
import com.katchy.focuslive.ui.util.bounceClick
import com.katchy.focuslive.ui.util.glassmorphism
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
    
    val note = remember(notes, noteId) {
        notes.find { it.id == noteId }
    }

    if (note == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val allCards = note.flashcards
    val now = System.currentTimeMillis()

    // Filter due cards or check if we want to review all
    var reviewAllMode by remember { mutableStateOf(false) }
    val activeCards = remember(allCards, reviewAllMode) {
        if (reviewAllMode) {
            allCards
        } else {
            allCards.filter { it.nextReviewDate <= now }
        }
    }

    var currentIndex by remember { mutableStateOf(0) }
    var showResults by remember { mutableStateOf(false) }

    // Leitner statistics calculation
    val stats = remember(allCards) {
        val boxes = IntArray(5) { 0 }
        allCards.forEach { card ->
            val boxIdx = (card.box - 1).coerceIn(0, 4)
            boxes[boxIdx]++
        }
        boxes
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isDark) Modifier else Modifier.background(MaterialTheme.colorScheme.background).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            )
    ) {
        if (isDark) {
            CosmicBackground(
                showParticles = false,
                showOrbs = true
            ) {}
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Flashcards: ${note.title}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                // Clear/Delete deck button
                IconButton(
                    onClick = {
                        viewModel.clearFlashcards(note)
                        onBack()
                    },
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = "Limpiar mazo",
                        tint = Color(0xFFEF4444)
                    )
                }
            }

            // Leitner box stats bar (beautiful dark grid)
            LeitnerStatsView(stats = stats)

            Spacer(modifier = Modifier.height(12.dp))

            if (allCards.isEmpty()) {
                // Empty Deck state
                EmptyDeckView(onBack = onBack)
            } else if (activeCards.isEmpty() && !showResults) {
                // Deck is completely studied/up-to-date
                UpToDateView(
                    onReviewAll = { reviewAllMode = true },
                    onBack = onBack
                )
            } else if (showResults || currentIndex >= activeCards.size) {
                // Done with current session
                StudySessionSuccessView(
                    totalCards = activeCards.size,
                    onRestart = {
                        currentIndex = 0
                        showResults = false
                    },
                    onBack = onBack
                )
            } else {
                // Main study session layout
                val currentCard = activeCards[currentIndex]

                var isFlipped by remember(currentIndex) { mutableStateOf(false) }

                val coroutineScope = rememberCoroutineScope()
                val configuration = LocalConfiguration.current
                val density = LocalDensity.current
                val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

                val offsetX = remember(currentCard.id) { Animatable(0f) }
                val offsetY = remember(currentCard.id) { Animatable(0f) }

                // Session progress bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentIndex > 0) {
                        IconButton(
                            onClick = {
                                currentIndex--
                                isFlipped = false
                            },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(32.dp)
                                .bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "Tarjeta anterior",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(40.dp))
                    }

                    Text(
                        text = "Tarjeta ${currentIndex + 1} de ${activeCards.size}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    LinearProgressIndicator(
                        progress = (currentIndex.toFloat()) / activeCards.size,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "Caja ${currentCard.box}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            viewModel.updateFlashcardLeitner(note, currentCard.id, wasCorrect = wasCorrect)
                            if (currentIndex + 1 >= activeCards.size) {
                                showResults = true
                            } else {
                                currentIndex++
                                isFlipped = false
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Control feedback buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (!isFlipped) {
                        Button(
                            onClick = { isFlipped = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Rounded.Flip, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Revelar Respuesta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // "Incorrecto" and "Correcto" Leitner control buttons
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = -screenWidthPx,
                                        animationSpec = tween(durationMillis = 250, easing = EaseOutQuart)
                                    )
                                    viewModel.updateFlashcardLeitner(note, currentCard.id, wasCorrect = false)
                                    if (currentIndex + 1 >= activeCards.size) {
                                        showResults = true
                                    } else {
                                        currentIndex++
                                        isFlipped = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(end = 8.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444)
                            )
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("Incorrecto", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = screenWidthPx,
                                        animationSpec = tween(durationMillis = 250, easing = EaseOutQuart)
                                    )
                                    viewModel.updateFlashcardLeitner(note, currentCard.id, wasCorrect = true)
                                    if (currentIndex + 1 >= activeCards.size) {
                                        showResults = true
                                    } else {
                                        currentIndex++
                                        isFlipped = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(start = 8.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            )
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("Entendido", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeitnerStatsView(stats: IntArray) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.6f else 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.06f else 0.1f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (i in 0..4) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Caja ${i + 1}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (i) {
                                0 -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                1 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                2 -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                3 -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                else -> Color(0xFF10B981).copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val numberColor = if (isDark) {
                        when (i) {
                            0 -> Color(0xFFF87171)
                            1 -> Color(0xFFFBBF24)
                            2 -> Color(0xFF60A5FA)
                            3 -> Color(0xFFA78BFA)
                            else -> Color(0xFF34D399)
                        }
                    } else {
                        when (i) {
                            0 -> Color(0xFFDC2626)
                            1 -> Color(0xFFD97706)
                            2 -> Color(0xFF2563EB)
                            3 -> Color(0xFF7C3AED)
                            else -> Color(0xFF059669)
                        }
                    }
                    Text(
                        text = stats[i].toString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = numberColor
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
    offsetX: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    offsetY: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val coroutineScope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .fillMaxHeight(0.85f)
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
                                // Swipe right: wasCorrect = true
                                launch { offsetY.animateTo(offsetY.value + velocityY * 0.2f, tween(200)) }
                                offsetX.animateTo(screenWidthPx, tween(200, easing = EaseOutQuart))
                                onSwipeDismiss(true)
                            } else if (distanceX < -swipeThreshold || velocityX < -1000f) {
                                // Swipe left: wasCorrect = false
                                launch { offsetY.animateTo(offsetY.value + velocityY * 0.2f, tween(200)) }
                                offsetX.animateTo(-screenWidthPx, tween(200, easing = EaseOutQuart))
                                onSwipeDismiss(false)
                            } else {
                                // Snap back
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
                rotationZ = offsetX.value / 25f // Dynamic tilt in Z-axis
                rotationY = rotation // Dynamic 3D flip in Y-axis
                cameraDistance = 16f * density
            }
            .blur(blurRadius)
            .bounceClick { onFlip() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.85f else 0.95f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.12f else 0.20f), 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            val swipeThreshold = screenWidthPx / 3.5f
            val swipeProgress = if (swipeThreshold > 0) (offsetX.value / swipeThreshold).coerceIn(-1f, 1f) else 0f

            if (isFront) {
                // Front: Question
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
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
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Back: Answer
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
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = card.answer,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                lineHeight = 26.sp
                            )
                        }
                    }

                    // Wiki and Video references (if present)
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
                                            tint = Color(0xFF38BDF8),
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
                                            tint = Color(0xFFF87171),
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

            // Green "ENTENDIDO" badge (top-left) when swiping right
            if (swipeProgress > 0.05f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .graphicsLayer {
                            rotationZ = -15f
                            alpha = swipeProgress.coerceIn(0f, 1f)
                        }
                        .border(3.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ENTENDIDO",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Red "INCORRECTO" badge (top-right) when swiping left
            if (swipeProgress < -0.05f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .graphicsLayer {
                            rotationZ = 15f
                            alpha = abs(swipeProgress).coerceIn(0f, 1f)
                        }
                        .border(3.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "INCORRECTO",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
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
        Icon(
            imageVector = Icons.Rounded.Layers,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Este mazo está vacío",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Genera tarjetas de memoria automáticas presionando el botón 'Generar Flashcards con IA' dentro de la nota.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.bounceClick(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Volver a Notas")
        }
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
        Icon(
            imageVector = Icons.Rounded.SentimentSatisfiedAlt,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¡Estás al día!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No tienes tarjetas pendientes de repasar para hoy según el método Leitner.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onReviewAll,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .bounceClick(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Repasar todas las tarjetas")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .bounceClick(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Volver")
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
        Icon(
            imageVector = Icons.Rounded.WorkspacePremium,
            contentDescription = null,
            tint = Color(0xFFFBBF24),
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "¡Sesión Completada!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Has repasado $totalCards tarjetas de memoria. Tu cerebro te lo agradecerá en el próximo examen.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .bounceClick(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Reiniciar Repaso")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .bounceClick(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Salir al Menú")
        }
    }
}