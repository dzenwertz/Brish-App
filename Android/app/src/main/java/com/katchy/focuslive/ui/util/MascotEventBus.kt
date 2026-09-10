package com.katchy.focuslive.ui.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Eventos que disparan reacciones en la mascota (celebraciones, saltos, etc.).
 * Singleton global para reacciones de mascota (tarea/hábito completado).
 */
enum class MascotEvent {
    TASK_DONE,
    HABIT_DONE,
    FOCUS_COMPLETE,
    BADGE_UNLOCKED,
    STREAK_MILESTONE
}

object MascotEventBus {
    private val _events = MutableSharedFlow<MascotEvent>(
        replay = 0,
        extraBufferCapacity = 8
    )
    val events: SharedFlow<MascotEvent> = _events.asSharedFlow()

    fun emit(event: MascotEvent) {
        _events.tryEmit(event)
    }
}
