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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.katchy.focuslive.NavigationBus
import com.katchy.focuslive.ui.home.HomeScreen
import com.katchy.focuslive.ui.navigation.Screen
import com.katchy.focuslive.ui.navigation.bottomNavItems
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import kotlinx.coroutines.launch

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
    // Las cuatro pestañas viven en un pager dentro de la ruta Home, para poder deslizar entre ellas.
    val pagerState = rememberPagerState(
        initialPage = tabRoutes.indexOf(initialDestination).coerceAtLeast(0),
        pageCount = { tabRoutes.size }
    )
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()
    fun openTab(route: String) {
        val page = tabRoutes.indexOf(route).takeIf { it >= 0 } ?: return
        if (entry?.destination?.route != Screen.Home.route) {
            navController.popBackStack(Screen.Home.route, inclusive = false)
        }
        scope.launch {
            if (reducedMotion) pagerState.scrollToPage(page)
            else pagerState.animateScrollToPage(page, animationSpec = BrishMotion.soft())
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
            else if (route == Screen.FocusMode.route || route == Screen.Courses.route) {
                navController.navigate(route) { launchSingleTop = true }
            }
            NavigationBus.consume()
        }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (initialDestination == Screen.FocusMode.route) Screen.FocusMode.route else Screen.Home.route,
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
                BackHandler(enabled = pagerState.currentPage != 0) { openTab(Screen.Home.route) }
                Box(Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        flingBehavior = PagerDefaults.flingBehavior(
                            state = pagerState,
                            snapAnimationSpec = BrishMotion.soft()
                        ),
                        key = { tabRoutes[it] },
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        Box(Modifier.fillMaxSize().padding(bottom = BrishBottomBarSpace)) {
                            when (tabRoutes[page]) {
                                Screen.Home.route -> HomeScreen(
                                    viewModel = homeViewModel,
                                    isGuest = isGuest,
                                    onNavigateToLogin = onNavigateToLogin,
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                    onNavigateToFocus = { navController.navigate(Screen.FocusMode.route) },
                                    onNavigateToPlanner = { openTab(Screen.Planner.route) },
                                    onOpenCourse = { navController.navigate("course/${Uri.encode(it)}") }
                                )
                                Screen.Planner.route -> com.katchy.focuslive.ui.planner.PlannerScreen(
                                    onBack = { openTab(Screen.Home.route) },
                                    onNavigateToFocus = { navController.navigate(Screen.FocusMode.route) },
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                                )
                                Screen.Notes.route -> com.katchy.focuslive.ui.notes.NotesScreen(
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                    onNavigateToStudy = { navController.navigate("study_flashcards/${Uri.encode(it)}") },
                                    onOpenNote = { id, folder, course -> openNote(id, folderId = folder, courseId = course) }
                                )
                                Screen.Habits.route -> com.katchy.focuslive.ui.habits.HabitsScreen()
                            }
                        }
                    }
                    BrishBottomBar(
                        tabs = bottomNavItems,
                        pagerState = pagerState,
                        onTabClick = { openTab(tabRoutes[it]) },
                        // Hasta que llegue la hoja de Captura, el ＋ abre una nota nueva.
                        onCaptureClick = { openNote(null) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 12.dp, end = 12.dp, bottom = 14.dp)
                    )
                }
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
