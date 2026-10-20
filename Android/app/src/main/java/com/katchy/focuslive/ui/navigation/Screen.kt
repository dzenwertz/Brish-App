package com.katchy.focuslive.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home_panel", "Hoy", Icons.Rounded.WbSunny)
    object Courses : Screen("courses", "Cursos", Icons.Rounded.School)
    object CourseDetail : Screen("course/{courseId}", "Curso", Icons.Rounded.School)
    object Planner : Screen("planner", "Agenda", Icons.Rounded.CalendarMonth)
    object Notes : Screen("notes", "Notas", Icons.Rounded.Description)
    object Habits : Screen("habits", "Hábitos", Icons.Rounded.BarChart)

    object Settings : Screen("settings", "Ajustes", Icons.Rounded.Settings)
    object FocusMode : Screen("focus_mode", "Enfoque", Icons.Rounded.Timer)
    object NoteEditor : Screen("note_editor?noteId={noteId}&folderId={folderId}&courseId={courseId}", "Apunte", Icons.Rounded.EditNote)
    object PremiumSubscription : Screen("premium_subscription", "Premium", Icons.Rounded.WorkspacePremium)
    object FlashcardsStudy : Screen("study_flashcards/{noteId}", "Estudiar", Icons.Rounded.School)
}

/** Pestañas del pager, en orden de deslizamiento. La captura (＋) va entre Agenda y Notas. */
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Planner,
    Screen.Notes,
    Screen.Habits
)
