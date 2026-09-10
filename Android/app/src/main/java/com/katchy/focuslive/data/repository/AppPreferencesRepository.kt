package com.katchy.focuslive.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    data class AcademicCourse(
        val id: String = java.util.UUID.randomUUID().toString(),
        val name: String,
        val color: Long = 0xFF06B6D4
    )

    private val _courses = MutableStateFlow(loadCourses())
    val courses: StateFlow<List<AcademicCourse>> = _courses.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(
        prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    )
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _permissionsExplanationShown = MutableStateFlow(
        prefs.getBoolean(KEY_PERMISSIONS_EXPLANATION_SHOWN, false)
    )
    val permissionsExplanationShown: StateFlow<Boolean> = _permissionsExplanationShown.asStateFlow()

    private val _userName = MutableStateFlow(
        prefs.getString(KEY_USER_NAME, "") ?: ""
    )
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _currentAcademicCycle = MutableStateFlow(
        prefs.getString(KEY_CURRENT_ACADEMIC_CYCLE, "2026-2") ?: "2026-2"
    )
    val currentAcademicCycle: StateFlow<String> = _currentAcademicCycle.asStateFlow()

    private val _currentLearningSpaceId = MutableStateFlow(
        prefs.getString(KEY_CURRENT_LEARNING_SPACE_ID, "") ?: ""
    )
    val currentLearningSpaceId: StateFlow<String> = _currentLearningSpaceId.asStateFlow()

    private val _workDuration = MutableStateFlow(
        prefs.getLong(KEY_WORK_DURATION, 25L)
    )
    val workDuration: StateFlow<Long> = _workDuration.asStateFlow()

    private val _breakDuration = MutableStateFlow(
        prefs.getLong(KEY_BREAK_DURATION, 5L)
    )
    val breakDuration: StateFlow<Long> = _breakDuration.asStateFlow()

    private val _notificationsScheduled = MutableStateFlow(
        prefs.getBoolean(KEY_NOTIFICATIONS_SCHEDULED, false)
    )
    val notificationsScheduled: StateFlow<Boolean> = _notificationsScheduled.asStateFlow()

    // Module Visibility
    private val _isPlannerEnabled = MutableStateFlow(prefs.getBoolean(KEY_PLANNER_ENABLED, true))
    val isPlannerEnabled: StateFlow<Boolean> = _isPlannerEnabled.asStateFlow()

    private val _isNotesEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTES_ENABLED, true))
    val isNotesEnabled: StateFlow<Boolean> = _isNotesEnabled.asStateFlow()

    private val _isFinanceEnabled = MutableStateFlow(prefs.getBoolean(KEY_FINANCE_ENABLED, true))
    val isFinanceEnabled: StateFlow<Boolean> = _isFinanceEnabled.asStateFlow()

    private val _isHabitsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HABITS_ENABLED, true))
    val isHabitsEnabled: StateFlow<Boolean> = _isHabitsEnabled.asStateFlow()

    private val oldIndigoDefault = android.graphics.Color.parseColor("#4F46E5")
    private val oldBrownDefault = android.graphics.Color.parseColor("#7B5945")
    private val newDefaultColor = android.graphics.Color.parseColor("#06B6D4")

    private val savedColor = prefs.getInt(KEY_ACCENT_COLOR, newDefaultColor).let {
        if (it == oldIndigoDefault || it == oldBrownDefault) {
            prefs.edit().putInt(KEY_ACCENT_COLOR, newDefaultColor).apply()
            newDefaultColor
        } else it
    }

    private val _accentColor = MutableStateFlow(savedColor)
    val accentColor: StateFlow<Int> = _accentColor.asStateFlow()

    private val _isPomodoroSoundEnabled = MutableStateFlow(prefs.getBoolean(KEY_POMODORO_SOUND_ENABLED, true))
    val isPomodoroSoundEnabled: StateFlow<Boolean> = _isPomodoroSoundEnabled.asStateFlow()

    private val _currentLanguage = MutableStateFlow(prefs.getString(KEY_CURRENT_LANGUAGE, "es") ?: "es")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _customSoundUri = MutableStateFlow(prefs.getString(KEY_CUSTOM_SOUND_URI, null))
    val customSoundUri: StateFlow<String?> = _customSoundUri.asStateFlow()

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
        _userName.value = name
    }

    fun setCurrentAcademicCycle(cycle: String) {
        val cleanCycle = cycle.trim()
        if (cleanCycle.isBlank()) return
        prefs.edit().putString(KEY_CURRENT_ACADEMIC_CYCLE, cleanCycle).apply()
        _currentAcademicCycle.value = cleanCycle
    }

    fun setCurrentLearningSpaceId(spaceId: String) {
        val cleanId = spaceId.trim()
        prefs.edit().putString(KEY_CURRENT_LEARNING_SPACE_ID, cleanId).apply()
        _currentLearningSpaceId.value = cleanId
    }

    fun addCourse(name: String): Boolean {
        val cleanName = name.trim()
        if (cleanName.isBlank() || _courses.value.any { it.name.equals(cleanName, ignoreCase = true) }) {
            return false
        }
        val colors = listOf(0xFF06B6D4, 0xFF818CF8, 0xFFF59E0B, 0xFF10B981, 0xFFF43F5E, 0xFF8B5CF6)
        val updated = _courses.value + AcademicCourse(name = cleanName, color = colors[_courses.value.size % colors.size])
        saveCourses(updated)
        return true
    }

    fun removeCourse(courseId: String) {
        saveCourses(_courses.value.filterNot { it.id == courseId })
    }

    fun clearLegacyCourses() = saveCourses(emptyList())

    private fun loadCourses(): List<AcademicCourse> {
        return try {
            val stored = prefs.getString(KEY_ACADEMIC_COURSES, null) ?: return emptyList()
            gson.fromJson<List<AcademicCourse>>(stored, object : TypeToken<List<AcademicCourse>>() {}.type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCourses(courses: List<AcademicCourse>) {
        prefs.edit().putString(KEY_ACADEMIC_COURSES, gson.toJson(courses)).apply()
        _courses.value = courses
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _onboardingCompleted.value = completed
    }

    fun setPermissionsExplanationShown(shown: Boolean) {
        prefs.edit().putBoolean(KEY_PERMISSIONS_EXPLANATION_SHOWN, shown).commit()
        _permissionsExplanationShown.value = shown
    }

    fun setWorkDuration(minutes: Long) {
        prefs.edit().putLong(KEY_WORK_DURATION, minutes).apply()
        _workDuration.value = minutes
    }

    fun setBreakDuration(minutes: Long) {
        prefs.edit().putLong(KEY_BREAK_DURATION, minutes).apply()
        _breakDuration.value = minutes
    }

    fun setNotificationsScheduled(scheduled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_SCHEDULED, scheduled).apply()
        _notificationsScheduled.value = scheduled
    }

    fun setPlannerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PLANNER_ENABLED, enabled).apply()
        _isPlannerEnabled.value = enabled
    }

    fun setNotesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTES_ENABLED, enabled).apply()
        _isNotesEnabled.value = enabled
    }

    fun setFinanceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FINANCE_ENABLED, enabled).apply()
        _isFinanceEnabled.value = enabled
    }

    fun setHabitsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HABITS_ENABLED, enabled).apply()
        _isHabitsEnabled.value = enabled
    }

    fun setAccentColor(color: Int) {
        prefs.edit().putInt(KEY_ACCENT_COLOR, color).apply()
        _accentColor.value = color
    }

    fun setPomodoroSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_POMODORO_SOUND_ENABLED, enabled).apply()
        _isPomodoroSoundEnabled.value = enabled
    }

    fun setLanguage(languageCode: String) {
        prefs.edit().putString(KEY_CURRENT_LANGUAGE, languageCode).apply()
        _currentLanguage.value = languageCode
    }

    fun setCustomSoundUri(uri: String?) {
        prefs.edit().putString(KEY_CUSTOM_SOUND_URI, uri).apply()
        _customSoundUri.value = uri
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_WORK_DURATION = "work_duration"
        private const val KEY_BREAK_DURATION = "break_duration"
        private const val KEY_NOTIFICATIONS_SCHEDULED = "notifications_scheduled"
        private const val KEY_PLANNER_ENABLED = "planner_enabled"
        private const val KEY_NOTES_ENABLED = "notes_enabled"
        private const val KEY_FINANCE_ENABLED = "finance_enabled"
        private const val KEY_HABITS_ENABLED = "habits_enabled"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_CURRENT_ACADEMIC_CYCLE = "current_academic_cycle"
        private const val KEY_CURRENT_LEARNING_SPACE_ID = "current_learning_space_id"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_POMODORO_SOUND_ENABLED = "pomodoro_sound_enabled"
        private const val KEY_CURRENT_LANGUAGE = "current_language"
        private const val KEY_CUSTOM_SOUND_URI = "custom_sound_uri"
        private const val KEY_PERMISSIONS_EXPLANATION_SHOWN = "permissions_explanation_shown"
        private const val KEY_ACADEMIC_COURSES = "academic_courses"
    }
}
