package com.katchy.focuslive.data.repository

import android.content.Context
import com.katchy.focuslive.data.model.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(resolveInitialTheme())
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private fun resolveInitialTheme(): AppTheme {
        // One-shot: Brish 2 is the light dashboard. Existing installs kept Dark/System
        // and looked like the old app after an update.
        if (!prefs.getBoolean("brish2_force_light_v22", false)) {
            prefs.edit()
                .putBoolean("brish2_force_light_v22", true)
                .putString("selected_theme", AppTheme.LIGHT.name)
                .apply()
            return AppTheme.LIGHT
        }
        return AppTheme.fromName(
            prefs.getString("selected_theme", AppTheme.LIGHT.name) ?: AppTheme.LIGHT.name
        )
    }

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        prefs.edit().putString("selected_theme", theme.name).apply()
    }
}
