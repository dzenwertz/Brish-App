package com.katchy.focuslive.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdateHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun updateStreak(streak: Int) {
        val manager = GlanceAppWidgetManager(context)
        val widget = StreakWidget()
        
        val glanceIds = manager.getGlanceIds(StreakWidget::class.java)
        
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[intPreferencesKey("current_streak")] = streak
                }
            }
            widget.update(context, glanceId)
        }
    }
}
