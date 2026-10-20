package com.katchy.focuslive.ui.main

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.katchy.focuslive.NavigationBus
import com.katchy.focuslive.ui.home.HomeScreen
import com.katchy.focuslive.ui.navigation.Screen
import com.katchy.focuslive.ui.navigation.bottomNavItems

@Composable
fun MainScreen(
    initialDestination: String? = null,
    isGuest: Boolean = false,
    onNavigateToLogin: () -> Unit = {}
) {
    val homeViewModel: com.katchy.focuslive.ui.home.HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val destination by NavigationBus.destination.collectAsState()
    val tabRoutes = remember { bottomNavItems.map { it.route } }
    fun openTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun openNote(noteId: String?, folderId: String? = null, courseId: String? = null) {
        navController.navigate("note_editor?noteId=${Uri.encode(noteId.orEmpty())}&folderId=${Uri.encode(folderId.orEmpty())}&courseId=${Uri.encode(courseId.orEmpty())}")
    }
    LaunchedEffect(destination, entry?.destination?.route) {
        // Closing the editor flushes its pending writes before this queued navigation is consumed.
        if (entry?.destination?.route == Screen.NoteEditor.route) return@LaunchedEffect
        destination?.let { route ->
            if (route in tabRoutes) openTab(route)
            else if (route == Screen.FocusMode.route) navController.navigate(route) { launchSingleTop = true }
            NavigationBus.consume()
        }
    }
    Scaffold(
        bottomBar = {
            if (entry?.destination?.route in tabRoutes) {
                NavigationBar(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).clip(RoundedCornerShape(24.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = entry?.destination?.route == screen.route,
                            onClick = { openTab(screen.route) },
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = initialDestination?.takeIf { it in tabRoutes || it == Screen.FocusMode.route } ?: Screen.Home.route,
            modifier = Modifier.fillMaxSize().padding(padding),
            enterTransition = {
                fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.985f, animationSpec = tween(280, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(tween(140, easing = FastOutSlowInEasing)) +
                    scaleOut(targetScale = 0.995f, animationSpec = tween(180, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.995f, animationSpec = tween(260, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(tween(140, easing = FastOutSlowInEasing)) +
                    scaleOut(targetScale = 0.985f, animationSpec = tween(180, easing = FastOutSlowInEasing))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    isGuest = isGuest,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToFocus = { navController.navigate(Screen.FocusMode.route) },
                    onNavigateToPlanner = { openTab(Screen.Planner.route) },
                    onOpenCourse = { navController.navigate("course/${Uri.encode(it)}") }
                )
            }
            composable(Screen.Planner.route) {
                com.katchy.focuslive.ui.planner.PlannerScreen(
                    onBack = { openTab(Screen.Home.route) },
                    onNavigateToFocus = { navController.navigate(Screen.FocusMode.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Courses.route) {
                com.katchy.focuslive.ui.courses.CoursesScreen(
                    onOpenCourse = { navController.navigate("course/${Uri.encode(it)}") },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(
                Screen.CourseDetail.route,
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                        fadeIn(tween(220, easing = FastOutSlowInEasing))
                },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { it / 5 }, animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                        fadeOut(tween(180))
                }
            ) {
                com.katchy.focuslive.ui.courses.CourseDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToFocus = { navController.navigate(Screen.FocusMode.route) },
                    onOpenNote = { id, course -> openNote(id, courseId = course) }
                )
            }
            composable(Screen.Notes.route) {
                com.katchy.focuslive.ui.notes.NotesScreen(
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToStudy = { navController.navigate("study_flashcards/${Uri.encode(it)}") },
                    onOpenNote = { id, folder, course -> openNote(id, folderId = folder, courseId = course) }
                )
            }
            composable(
                Screen.NoteEditor.route,
                arguments = listOf("noteId", "folderId", "courseId").map {
                    navArgument(it) { type = NavType.StringType; defaultValue = "" }
                },
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                        fadeIn(tween(220, easing = FastOutSlowInEasing))
                },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { it / 5 }, animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                        fadeOut(tween(180))
                }
            ) {
                com.katchy.focuslive.ui.notes.NoteEditorScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.Settings.route,
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = tween(320, easing = FastOutSlowInEasing)) + fadeIn(tween(220))
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { it / 5 }, animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
                }
            ) {
                com.katchy.focuslive.ui.settings.SettingsScreen(
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToSubscription = { navController.navigate(Screen.PremiumSubscription.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.FocusMode.route,
                enterTransition = {
                    slideInVertically(initialOffsetY = { it / 5 }, animationSpec = tween(340, easing = FastOutSlowInEasing)) + fadeIn(tween(220))
                },
                popExitTransition = {
                    slideOutVertically(targetOffsetY = { it / 5 }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
                }
            ) {
                com.katchy.focuslive.ui.focus.FocusSessionScreen(onNavigateBack = { navController.popBackStack() }, viewModel = homeViewModel)
            }
            composable(Screen.PremiumSubscription.route) {
                com.katchy.focuslive.ui.premium.PremiumSubscriptionScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.FlashcardsStudy.route) { backStack ->
                com.katchy.focuslive.ui.notes.FlashcardsStudyScreen(
                    noteId = backStack.arguments?.getString("noteId").orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
