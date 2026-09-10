package com.katchy.focuslive.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.katchy.focuslive.ui.home.HomeScreen
import com.katchy.focuslive.ui.navigation.Screen
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.bounceClick

@Composable
fun MainScreen(
    initialDestination: String? = null,
    isGuest: Boolean = false,
    onNavigateToLogin: () -> Unit = {},
    viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isDark = isDarkTheme()
    
    // Listen for deep link events
    val deepLinkDestination by com.katchy.focuslive.NavigationBus.destination.collectAsState(initial = null)
    
    val activeBottomNavItems = remember { listOf(Screen.Home, Screen.Courses, Screen.Notes, Screen.More) }
    
    LaunchedEffect(deepLinkDestination) {
        deepLinkDestination?.let { dest ->
            try {
                val isTab = when(dest) {
                    Screen.Home.route, Screen.Courses.route, Screen.Notes.route, Screen.More.route -> true
                    else -> false
                }
                
                navController.navigate(dest) {
                    if (isTab) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainScreen", "Navigation exception", e)
            }
            com.katchy.focuslive.NavigationBus.consume()
        }
    }

    // Fluid spring transitions
    val slideSpring = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.85f, stiffness = 350f)
    val tabSlideSpring = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.9f, stiffness = 380f)
    val tabFadeSpring = tween<Float>(durationMillis = 180)

    val getTabIndex = { route: String? ->
        activeBottomNavItems.indexOfFirst { it.route == route }
    }

    val getTabEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val fromIndex = getTabIndex(initialState.destination.route)
        val toIndex = getTabIndex(targetState.destination.route)
        if (fromIndex != -1 && toIndex != -1) {
            if (toIndex > fromIndex) {
                slideInHorizontally(animationSpec = tabSlideSpring) { it / 8 } + fadeIn(animationSpec = tabFadeSpring)
            } else {
                slideInHorizontally(animationSpec = tabSlideSpring) { -it / 8 } + fadeIn(animationSpec = tabFadeSpring)
            }
        } else {
            fadeIn(animationSpec = tabFadeSpring)
        }
    }

    val getTabExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val fromIndex = getTabIndex(initialState.destination.route)
        val toIndex = getTabIndex(targetState.destination.route)
        if (fromIndex != -1 && toIndex != -1) {
            if (toIndex > fromIndex) {
                slideOutHorizontally(animationSpec = tabSlideSpring) { -it / 8 } + fadeOut(animationSpec = tabFadeSpring)
            } else {
                slideOutHorizontally(animationSpec = tabSlideSpring) { it / 8 } + fadeOut(animationSpec = tabFadeSpring)
            }
        } else {
            fadeOut(animationSpec = tabFadeSpring)
        }
    }

    val nestedEnter = slideInHorizontally(initialOffsetX = { it }, animationSpec = slideSpring) + fadeIn(tween(200))
    val nestedExit = slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = slideSpring) + fadeOut(tween(200))
    val nestedPopEnter = slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = slideSpring) + fadeIn(tween(200))
    val nestedPopExit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = slideSpring) + fadeOut(tween(200))

    val isImmersive = when {
        currentDestination?.route == Screen.FocusMode.route -> true
        currentDestination?.route == Screen.Chat.route -> true
        currentDestination?.route == Screen.PremiumSubscription.route -> true
        currentDestination?.route == Screen.Settings.route -> true
        currentDestination?.route == Screen.CourseDetail.route -> true
        currentDestination?.route?.startsWith("study_flashcards") == true -> true
        currentDestination?.route == Screen.Planner.route -> true
        currentDestination?.route == Screen.Habits.route -> true
        currentDestination?.route == Screen.Finance.route -> true
        else -> false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = initialDestination ?: Screen.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(
                route = Screen.Home.route,
                enterTransition = { getTabEnterTransition() },
                exitTransition = { getTabExitTransition() },
                popEnterTransition = { getTabEnterTransition() },
                popExitTransition = { getTabExitTransition() }
            ) {
                HomeScreen(
                    isGuest = isGuest,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToFocus = { navController.navigate(Screen.FocusMode.route) },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToPlanner = { navController.navigate(Screen.Planner.route) },
                    onNavigateToCourses = {
                        navController.navigate(Screen.Courses.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToNotes = {
                        navController.navigate(Screen.Notes.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToHabits = { navController.navigate(Screen.Habits.route) },
                    onNavigateToFinance = { navController.navigate(Screen.Finance.route) }
                )
            }

            composable(
                route = Screen.Courses.route,
                enterTransition = { getTabEnterTransition() },
                exitTransition = { getTabExitTransition() },
                popEnterTransition = { getTabEnterTransition() },
                popExitTransition = { getTabExitTransition() }
            ) {
                com.katchy.focuslive.ui.courses.CoursesScreen(
                    onOpenCourse = { courseId -> navController.navigate("course/$courseId") },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(
                route = Screen.CourseDetail.route,
                enterTransition = { nestedEnter },
                exitTransition = { nestedExit },
                popEnterTransition = { nestedPopEnter },
                popExitTransition = { nestedPopExit }
            ) {
                com.katchy.focuslive.ui.courses.CourseDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToFocus = { navController.navigate(Screen.FocusMode.route) }
                )
            }

            composable(
                route = Screen.Planner.route,
                enterTransition = { nestedEnter },
                exitTransition = { nestedExit },
                popEnterTransition = { nestedPopEnter },
                popExitTransition = { nestedPopExit }
            ) {
                com.katchy.focuslive.ui.planner.PlannerScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(
                route = Screen.Notes.route,
                enterTransition = { getTabEnterTransition() },
                exitTransition = { getTabExitTransition() },
                popEnterTransition = { getTabEnterTransition() },
                popExitTransition = { getTabExitTransition() }
            ) {
                com.katchy.focuslive.ui.notes.NotesScreen(
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToStudy = { noteId -> navController.navigate("study_flashcards/$noteId") }
                )
            }

            composable(
                route = Screen.Habits.route,
                enterTransition = { nestedEnter },
                exitTransition = { nestedExit },
                popEnterTransition = { nestedPopEnter },
                popExitTransition = { nestedPopExit }
            ) { 
                com.katchy.focuslive.ui.habits.HabitsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                ) 
            }

            composable(
                route = Screen.Finance.route,
                enterTransition = { nestedEnter },
                exitTransition = { nestedExit },
                popEnterTransition = { nestedPopEnter },
                popExitTransition = { nestedPopExit }
            ) { 
                com.katchy.focuslive.ui.finance.FinanceScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(
                route = Screen.More.route,
                enterTransition = { getTabEnterTransition() },
                exitTransition = { getTabExitTransition() },
                popEnterTransition = { getTabEnterTransition() },
                popExitTransition = { getTabExitTransition() }
            ) {
                com.katchy.focuslive.ui.more.MoreScreen(
                    onNavigateToPlanner = { navController.navigate(Screen.Planner.route) },
                    onNavigateToHabits = { navController.navigate(Screen.Habits.route) },
                    onNavigateToFinance = { navController.navigate(Screen.Finance.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                )
            }
            
            composable(
                route = Screen.Settings.route,
                enterTransition = { nestedEnter },
                exitTransition = { nestedExit },
                popEnterTransition = { nestedPopEnter },
                popExitTransition = { nestedPopExit }
            ) { 
                com.katchy.focuslive.ui.settings.SettingsScreen(
                    onNavigateToLogin = onNavigateToLogin,
                    onBack = { navController.popBackStack() }
                ) 
            }
            
            composable(
                route = Screen.FocusMode.route,
                enterTransition = { 
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = { 
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = { 
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeIn(animationSpec = tween(200))
                },
                popExitTransition = { 
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)
                    ) + fadeOut(animationSpec = tween(200))
                }
            ) {
                com.katchy.focuslive.ui.focus.FocusSessionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                )
            }

            composable(
                route = Screen.Chat.route,
                enterTransition = { nestedEnter },
                exitTransition = { nestedExit },
                popEnterTransition = { nestedPopEnter },
                popExitTransition = { nestedPopExit }
            ) {
                com.katchy.focuslive.ui.chat.ChatScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.PremiumSubscription.route,
                enterTransition = { nestedEnter },
                exitTransition = { nestedExit },
                popEnterTransition = { nestedPopEnter },
                popExitTransition = { nestedPopExit }
            ) {
                com.katchy.focuslive.ui.premium.PremiumSubscriptionScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.FlashcardsStudy.route,
                enterTransition = { nestedEnter },
                exitTransition = { nestedExit },
                popEnterTransition = { nestedPopEnter },
                popExitTransition = { nestedPopExit }
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                com.katchy.focuslive.ui.notes.FlashcardsStudyScreen(
                    noteId = noteId,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Modern Docked Bottom Navigation Bar
        AnimatedVisibility(
            visible = !isImmersive,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                initialOffsetY = { it }
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium),
                targetOffsetY = { it }
            ) + fadeOut(tween(150))
        ) {
            val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
            val selectedIndex = remember(activeBottomNavItems, currentDestination) {
                val activeRoute = when (currentDestination?.route) {
                    Screen.Planner.route, Screen.Habits.route, Screen.Finance.route -> Screen.More.route
                    else -> currentDestination?.route
                }
                activeBottomNavItems.indexOfFirst { it.route == activeRoute }.coerceAtLeast(0)
            }
            
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = if (isDark) Color(0xFF0F1116) else Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF0F2F6)
                ),
                shadowElevation = if (isDark) 0.dp else 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeBottomNavItems.forEachIndexed { index, screen ->
                        val isSelected = index == selectedIndex

                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) Color.White else Color(0xFF111827)
                            } else {
                                if (isDark) Color(0xFFA6ADBC) else Color(0xFF697386)
                            },
                            label = "navIconTint",
                            animationSpec = tween(180)
                        )

                        val textTint by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                            } else {
                                if (isDark) Color(0xFFA6ADBC) else Color(0xFF697386)
                            },
                            label = "navTextTint",
                            animationSpec = tween(180)
                        )

                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF111827).copy(alpha = 0.07f)
                            } else Color.Transparent,
                            label = "navPillBg",
                            animationSpec = tween(180)
                        )

                        val pillBorderColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
                            } else Color.Transparent,
                            label = "navPillBorder",
                            animationSpec = tween(180)
                        )

                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.06f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                            label = "navIconScale"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .bounceClick(scaleDown = 0.94f) {
                                    if (currentDestination?.route != screen.route) {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                        ) {
                            // Sleek indicator pill enclosing only the icon
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(52.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(pillBg)
                                    .border(1.dp, pillBorderColor, RoundedCornerShape(100.dp))
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = iconTint,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    letterSpacing = 0.1.sp
                                ),
                                color = textTint,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
