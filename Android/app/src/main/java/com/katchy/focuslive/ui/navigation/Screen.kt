package com.katchy.focuslive.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home_panel", "Inicio", Icons.Rounded.HourglassTop)
    object Courses : Screen("courses", "Cursos", Icons.Rounded.School)
    object CourseDetail : Screen("course/{courseId}", "Curso", Icons.Rounded.School)
    object Planner : Screen("planner", "Plan", Icons.Rounded.CalendarToday)
    object Notes : Screen("notes", "Notas", Icons.Rounded.FolderOpen)
    object Habits : Screen("habits", "Hábitos", Icons.Rounded.CheckCircleOutline)
    object Finance : Screen("finance", "Finanzas", Icons.Rounded.AccountBalanceWallet)
    object More : Screen("more", "Más", Icons.Rounded.MoreHoriz)

    object Settings : Screen("settings", "Ajustes", Icons.Rounded.Settings)
    object FocusMode : Screen("focus_mode", "Enfoque", Icons.Rounded.Timer)
    object Chat : Screen("chat", "Brish AI", Icons.Rounded.AutoAwesome)
    object PremiumSubscription : Screen("premium_subscription", "Premium", Icons.Rounded.WorkspacePremium)
    object FlashcardsStudy : Screen("study_flashcards/{noteId}", "Estudiar", Icons.Rounded.School)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Courses,
    Screen.Notes,
    Screen.More
)

