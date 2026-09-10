package com.katchy.focuslive.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.katchy.focuslive.R

import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import android.content.Intent
import com.katchy.focuslive.service.TimerService

class PomodoroWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val timeLeft = prefs[stringPreferencesKey("time_left")] ?: "25:00"
            val isRunning = prefs[booleanPreferencesKey("is_running")] ?: false
            val status = prefs[stringPreferencesKey("status")] ?: "Modo Enfoque"

            GlanceTheme {
                PomodoroContent(timeLeft, isRunning, status)
            }
        }
    }

    @Composable
    private fun PomodoroContent(timeLeft: String, isRunning: Boolean, status: String) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_pomodoro_background))
                .clickable(
                    actionStartActivity<com.katchy.focuslive.MainActivity>(
                        actionParametersOf(ActionParameters.Key<String>("destination") to "focus")
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status label
                Text(
                    text = "BRISH • ${status.uppercase()}",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF9CA3AF)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Timer - big, bold, central
                Text(
                    text = timeLeft,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF9FAFB)),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = GlanceModifier.height(16.dp))

                // Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Reset Button
                    Box(
                        modifier = GlanceModifier
                            .size(40.dp)
                            .background(ImageProvider(R.drawable.widget_button_circle_secondary))
                            .clickable(actionRunCallback<ResetTimerAction>())
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_reset),
                            contentDescription = "Reiniciar",
                            modifier = GlanceModifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    // Play/Pause Button (larger, primary)
                    Box(
                        modifier = GlanceModifier
                            .size(52.dp)
                            .background(ImageProvider(R.drawable.widget_button_circle))
                            .clickable(actionRunCallback<ToggleTimerAction>())
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(
                                if (isRunning) R.drawable.ic_widget_pause 
                                else R.drawable.ic_widget_play
                            ),
                            contentDescription = if (isRunning) "Pausar" else "Iniciar",
                            modifier = GlanceModifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}


class ToggleTimerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_TOGGLE_TIMER
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("PomodoroWidget", "Exception caught", e)
        }
    }
}

class ResetTimerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESET_TIMER
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("PomodoroWidget", "Exception caught", e)
        }
    }
}
