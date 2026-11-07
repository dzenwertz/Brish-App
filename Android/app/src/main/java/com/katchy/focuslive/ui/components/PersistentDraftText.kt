package com.katchy.focuslive.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.katchy.focuslive.ui.theme.MotionTokens
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.katchy.focuslive.FocusLiveApp
import com.katchy.focuslive.data.draft.DraftStore
import kotlinx.coroutines.delay

class PersistentDraftText internal constructor(
    val key: String,
    private val initialValue: String,
    private val store: DraftStore
) {
    private val state = mutableStateOf(initialValue)
    var value: String
        get() = state.value
        set(newValue) {
            state.value = newValue
            userEdited = true
            recovered = false
            saved = false
            val toStore = newValue.takeIf { it.isNotBlank() && it != initialValue }
            store.scheduleWrite(key, toStore)
        }
    private var userEdited = false
    internal val edited: Boolean get() = userEdited
    var recovered by mutableStateOf(false)
        internal set
    var saved by mutableStateOf(false)
        internal set

    suspend fun hydrate() {
        val restored = runCatching { store.read(key) }.getOrNull()
        if (restored != null) {
            restore(restored)
        }
    }

    fun hydrateBlocking() {
        kotlinx.coroutines.runBlocking {
            hydrate()
        }
    }

    fun flushIfDirty() {
        if (!userEdited) return
        val toStore = state.value.takeIf { it.isNotBlank() && it != initialValue }
        store.writeBlocking(key, toStore)
    }

    fun discard() {
        state.value = initialValue
        userEdited = false
        recovered = false
        saved = false
        store.writeBlocking(key, null)
    }

    fun clearAfterSave() = discard()

    internal fun restore(value: String) {
        if (!userEdited && value != initialValue) {
            state.value = value
            recovered = true
        }
    }

    fun clearStored() {
        userEdited = false
        recovered = false
        saved = false
        store.writeBlocking(key, null)
    }
}

@Composable
fun rememberPersistentDraftText(key: String, initialValue: String = ""): PersistentDraftText {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext as FocusLiveApp
    val store = context.draftStore
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val draft = remember(key, store) { PersistentDraftText(key, initialValue, store) }
    var hydrated by remember(key) { mutableStateOf(false) }

    LaunchedEffect(key, draft) {
        draft.hydrate()
        hydrated = true
    }
    LaunchedEffect(key, draft.value, hydrated) {
        if (!hydrated || !draft.edited) return@LaunchedEffect
        val toStore = draft.value.takeIf { it.isNotBlank() && it != initialValue }
        store.scheduleWrite(key, toStore)
        draft.saved = false
        if (toStore != null) {
            delay(DraftStore.WRITE_DEBOUNCE_MS + 80L)
            runCatching { store.flush() }
            draft.saved = true
            delay(1200L)
            draft.saved = false
        }
    }
    androidx.compose.runtime.DisposableEffect(key, lifecycle, draft) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                runCatching { draft.flushIfDirty() }
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            runCatching { draft.flushIfDirty() }
        }
    }
    return draft
}

@Composable
fun DraftRecoveryNotice(
    draft: PersistentDraftText,
    modifier: Modifier = Modifier
) {
    val savedOffset = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.roundToPx() }
    androidx.compose.foundation.layout.Box(modifier) {
        AnimatedVisibility(
            visible = draft.recovered,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("Borrador recuperado", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = draft::discard) { Text("Descartar") }
            }
        }
        AnimatedVisibility(
            visible = !draft.recovered && draft.saved,
            enter = fadeIn(androidx.compose.animation.core.tween(MotionTokens.microMs)) + slideInVertically(
                androidx.compose.animation.core.tween(MotionTokens.microMs)
            ) { -savedOffset },
            exit = fadeOut(androidx.compose.animation.core.tween(MotionTokens.microMs))
        ) {
            Text("Guardado ✓", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
        }
    }
}
