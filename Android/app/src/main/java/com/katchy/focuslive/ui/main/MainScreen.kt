package com.katchy.focuslive.ui.main

import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
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
import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.katchy.focuslive.ui.components.LocalNavAnimatedScope
import com.katchy.focuslive.ui.components.LocalTabReselectSignal
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.katchy.focuslive.ui.theme.BrishMotion
import com.katchy.focuslive.ui.theme.MotionTokens
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
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
    val reselectSignals = remember { mutableStateMapOf<Int, Int>() }
    fun openTab(route: String) {
        val page = tabRoutes.indexOf(route).takeIf { it >= 0 } ?: return
        val onHomeRoute = entry?.destination?.route == Screen.Home.route
        if (!onHomeRoute) {
            navController.popBackStack(Screen.Home.route, inclusive = false)
        }
        if (onHomeRoute && pagerState.currentPage == page && !pagerState.isScrollInProgress) {
            reselectSignals[page] = (reselectSignals[page] ?: 0) + 1
            return
        }
        scope.launch {
            // Pestaña vecina: deslizamiento corto. Más lejos o con movimiento reducido: cambio directo,
            // como la barra de iOS (deslizar por las páginas intermedias las componía todas).
            if (reducedMotion || kotlin.math.abs(page - pagerState.currentPage) > 1) pagerState.scrollToPage(page)
            else pagerState.animateScrollToPage(page, animationSpec = androidx.compose.animation.core.tween(220, easing = com.katchy.focuslive.ui.theme.MotionTokens.emphasized))
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
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            // Navegación estilo iOS: push horizontal con paralaje; las rutas modales suben desde abajo.
            enterTransition = {
                if (targetState.destination.route in ModalRoutes) iosModalIn() else iosPushIn()
            },
            exitTransition = {
                if (targetState.destination.route in ModalRoutes) iosStay() else iosPushOut()
            },
            popEnterTransition = {
                if (initialState.destination.route in ModalRoutes) iosStayIn() else iosPopIn()
            },
            popExitTransition = {
                if (initialState.destination.route in ModalRoutes) iosModalOut() else iosPopOut()
            }
        ) {
            composable(Screen.Home.route) {
              CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                var captureOpen by rememberSaveable { mutableStateOf(false) }
                val hazeState = remember { HazeState() }
                val navBarBottom = androidx.compose.foundation.layout.WindowInsets.navigationBars
                    .asPaddingValues().calculateBottomPadding()
                val captureViewModel: com.katchy.focuslive.ui.capture.CaptureViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                // Atrás predictivo entre pestañas: el contenido se asoma hacia Hoy mientras dura el gesto.
                PredictiveBackHandler(enabled = pagerState.currentPage != 0 && !captureOpen) { progress ->
                    val from = pagerState.currentPage
                    try {
                        progress.collect { event -> pagerState.scrollToPage(from, -0.3f * event.progress) }
                        pagerState.animateScrollToPage(0, animationSpec = BrishMotion.soft())
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        scope.launch { pagerState.animateScrollToPage(from, animationSpec = BrishMotion.lively()) }
                        throw e
                    }
                }
                // Sin efecto de profundidad: escalar, recortar y desenfocar toda la app en cada fotograma
                // era lo más costoso al abrir la Captura. El velo de la hoja basta (como en iOS).
                Box(Modifier.fillMaxSize()) {
                  Box(
                    Modifier.fillMaxSize()
                  ) {
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
                        // El contenido pasa por debajo de la barra; cada lista deja este espacio al final.
                        CompositionLocalProvider(
                            LocalBottomBarInset provides BrishBottomBarSpace + navBarBottom,
                            LocalTabReselectSignal provides (reselectSignals[page] ?: 0)
                        ) {
                        Box(Modifier.fillMaxSize()) {
                            when (tabRoutes[page]) {
                                Screen.Home.route -> HomeScreen(
                                    viewModel = homeViewModel,
                                    isGuest = isGuest,
                                    onNavigateToLogin = onNavigateToLogin,
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                    onNavigateToFocus = { navController.navigate(Screen.FocusMode.route) },
                                    onNavigateToPlanner = { openTab(Screen.Planner.route) },
                                    onOpenHabits = { openTab(Screen.Habits.route) },
                                    onCapture = { captureOpen = true },
                                    onReview = { navController.navigate(Screen.WeeklyReview.route) { launchSingleTop = true } }
                                )
                                Screen.Planner.route -> com.katchy.focuslive.ui.planner.PlannerScreen(
                                    onBack = { openTab(Screen.Home.route) },
                                    onNavigateToFocus = { navController.navigate(Screen.FocusMode.route) },
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                    onOpenReview = { navController.navigate(Screen.WeeklyReview.route) { launchSingleTop = true } },
                                    onOpenNote = { openNote(it) }
                                )
                                Screen.Notes.route -> com.katchy.focuslive.ui.notes.NotesScreen(
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                    onNavigateToStudy = { navController.navigate("study_flashcards/${Uri.encode(it)}") },
                                    onOpenNote = { id, folder, course -> openNote(id, folderId = folder, courseId = course) }
                                )
                                Screen.Habits.route -> com.katchy.focuslive.ui.habits.HabitsScreen(
                                    onCreate = {
                                        captureViewModel.reset("hábito: ")
                                        captureOpen = true
                                    }
                                )
                            }
                        }
                        }
                    }
                    BrishBottomBar(
                        tabs = bottomNavItems,
                        pagerState = pagerState,
                        onTabClick = { openTab(tabRoutes[it]) },
                        onCaptureClick = { captureOpen = !captureOpen },
                        captureOpen = captureOpen,
                        hazeState = hazeState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                  }
                  com.katchy.focuslive.ui.capture.CaptureSheet(
                      visible = captureOpen,
                      onDismiss = { captureOpen = false },
                      viewModel = captureViewModel
                  )
                  val mainViewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                  val guideShown by mainViewModel.guideShown.collectAsState()
                  if (!guideShown && !captureOpen) {
                      BrishGuide(onFinish = mainViewModel::finishGuide)
                  }
                }
              }
            }
            composable(Screen.Courses.route) {
              CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                com.katchy.focuslive.ui.courses.CoursesScreen(
                    onOpenCourse = { navController.navigate("course/${Uri.encode(it)}") },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
              }
            }
            composable(Screen.CourseDetail.route) {
              CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                com.katchy.focuslive.ui.courses.CourseDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToFocus = { navController.navigate(Screen.FocusMode.route) },
                    onOpenNote = { id, course -> openNote(id, courseId = course) }
                )
              }
            }
            composable(
                Screen.NoteEditor.route,
                arguments = listOf("noteId", "folderId", "courseId").map {
                    navArgument(it) { type = NavType.StringType; defaultValue = "" }
                },
            ) {
                CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                    com.katchy.focuslive.ui.notes.NoteEditorScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(Screen.Settings.route) {
                com.katchy.focuslive.ui.settings.SettingsScreen(
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToSubscription = { navController.navigate(Screen.PremiumSubscription.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.FocusMode.route) {
                CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                    com.katchy.focuslive.ui.focus.FocusSessionScreen(onNavigateBack = { navController.popBackStack() }, viewModel = homeViewModel)
                }
            }
            composable(Screen.WeeklyReview.route) {
                com.katchy.focuslive.ui.review.WeeklyReviewScreen(
                    onClose = { navController.popBackStack() },
                    onPlanWeek = { navController.popBackStack(); openTab(Screen.Planner.route) }
                )
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

/** Pantallas que se presentan como hoja modal (suben desde abajo) en lugar de push. */
private val ModalRoutes = setOf(Screen.FocusMode.route, Screen.WeeklyReview.route)

private fun <T> iosTween(ms: Int = com.katchy.focuslive.ui.theme.IosMotion.pushMs) =
    tween<T>(ms, easing = com.katchy.focuslive.ui.theme.IosMotion.curve)

private fun iosPushIn(): androidx.compose.animation.EnterTransition =
    slideInHorizontally(iosTween()) { it }

private fun iosPushOut(): androidx.compose.animation.ExitTransition =
    slideOutHorizontally(iosTween()) { -(it * com.katchy.focuslive.ui.theme.IosMotion.parallax).toInt() } +
        fadeOut(iosTween(), targetAlpha = com.katchy.focuslive.ui.theme.IosMotion.dimAlpha)

private fun iosPopIn(): androidx.compose.animation.EnterTransition =
    slideInHorizontally(iosTween()) { -(it * com.katchy.focuslive.ui.theme.IosMotion.parallax).toInt() } +
        fadeIn(iosTween(), initialAlpha = com.katchy.focuslive.ui.theme.IosMotion.dimAlpha)

private fun iosPopOut(): androidx.compose.animation.ExitTransition =
    slideOutHorizontally(iosTween()) { it }

private fun iosModalIn(): androidx.compose.animation.EnterTransition =
    slideInVertically(iosTween(com.katchy.focuslive.ui.theme.IosMotion.modalMs)) { it }

private fun iosModalOut(): androidx.compose.animation.ExitTransition =
    slideOutVertically(iosTween(com.katchy.focuslive.ui.theme.IosMotion.modalMs)) { it }

/** La pantalla de debajo de un modal no se mueve: solo se atenúa un poco. */
private fun iosStay(): androidx.compose.animation.ExitTransition =
    fadeOut(iosTween(com.katchy.focuslive.ui.theme.IosMotion.modalMs), targetAlpha = 0.85f)

private fun iosStayIn(): androidx.compose.animation.EnterTransition =
    fadeIn(iosTween(com.katchy.focuslive.ui.theme.IosMotion.modalMs), initialAlpha = 0.85f)
