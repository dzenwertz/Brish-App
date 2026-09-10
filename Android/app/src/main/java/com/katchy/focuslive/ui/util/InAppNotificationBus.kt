package com.katchy.focuslive.ui.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class InAppNotification(
    val message: String,
    val icon: String? = null,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val durationMillis: Long = 4000L
)

object InAppNotificationBus {
    private val _notifications = MutableSharedFlow<InAppNotification>(extraBufferCapacity = 10)
    val notifications = _notifications.asSharedFlow()

    fun post(
        message: String,
        icon: String? = null,
        actionLabel: String? = null,
        durationMillis: Long = 4000L,
        onAction: (() -> Unit)? = null
    ) {
        _notifications.tryEmit(InAppNotification(message, icon, actionLabel, onAction, durationMillis))
    }
}
