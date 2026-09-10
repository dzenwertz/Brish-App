package com.katchy.focuslive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.katchy.focuslive.data.repository.AppPreferencesRepository
import com.katchy.focuslive.data.repository.ThemeRepository
import com.katchy.focuslive.ui.auth.AuthViewModel
import com.katchy.focuslive.ui.auth.LoginScreen
import com.katchy.focuslive.ui.main.MainScreen
import com.katchy.focuslive.ui.onboarding.OnboardingScreen
import com.katchy.focuslive.ui.splash.SplashScreen
import com.katchy.focuslive.ui.theme.BrishTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.luminance
import com.katchy.focuslive.ui.util.InAppNotification
import com.katchy.focuslive.ui.util.InAppNotificationBus
import kotlinx.coroutines.delay

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeRepository: ThemeRepository

    @Inject
    lateinit var prefsRepository: AppPreferencesRepository
    
    @Inject
    lateinit var notificationScheduler: com.katchy.focuslive.scheduler.NotificationScheduler

    @Inject
    lateinit var userStatsRepository: com.katchy.focuslive.data.repository.UserStatsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- CHECK PREVIOUS CRASH LOG ---
        val crashFile = java.io.File(filesDir, "crash_log.txt")
        if (crashFile.exists()) {
            val trace = try {
                crashFile.readText()
            } catch (e: Exception) {
                "Error leyendo crash log: ${e.message}"
            }
            try {
                crashFile.delete()
            } catch (e: Exception) {
                // Ignore delete failure
            }
            CrashActivity.start(this, trace)
            finish()
            return
        }
        
        // Schedule notifications daily (re-schedule to ensure they persist after reboot/updates)
        // Ideally handled by BootReceiver, but rescheduling on app launch is a safe fallback
        // NOTE: Runs on IO thread to avoid ANR from Room/SharedPreferences reads on main thread
        lifecycleScope.launch(Dispatchers.IO) {
            notificationScheduler.rescheduleAllAlarms()
            prefsRepository.setNotificationsScheduled(true)
        }
        
        // Sync premium status from Firestore on startup
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val isPremium = document.getBoolean("isPremium") ?: false
                    userStatsRepository.setPremium(isPremium)
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("MainActivity", "Failed to sync premium status", e)
                }

            // Immediately pull down notes, tasks, habits, and courses from Firestore
            val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.katchy.focuslive.data.worker.SyncWorker>()
                .setInputData(androidx.work.workDataOf("FORCE_REFRESH" to true))
                .build()
            androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "StartupSyncWork",
                androidx.work.ExistingWorkPolicy.KEEP,
                syncRequest
            )
        }
        
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            val user by authViewModel.user.collectAsState()
            val onboardingCompleted by prefsRepository.onboardingCompleted.collectAsState()
            val currentTheme by themeRepository.currentTheme.collectAsState()
            val currentLanguage by prefsRepository.currentLanguage.collectAsState()
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            
            val useDarkTheme = when (currentTheme) {
                com.katchy.focuslive.data.model.AppTheme.LIGHT -> false
                com.katchy.focuslive.data.model.AppTheme.DARK -> true
                com.katchy.focuslive.data.model.AppTheme.AMOLED -> true
                com.katchy.focuslive.data.model.AppTheme.SYSTEM -> isSystemDark
            }

            val isAmoled = currentTheme == com.katchy.focuslive.data.model.AppTheme.AMOLED

            val navController = rememberNavController()
            
            // Handle Intent (Cold Start)
            LaunchedEffect(Unit) {
                handleDestination(intent, navController)
            }
            
            // Handle Intent (New Intent / Warm Start)
            DisposableEffect(Unit) {
                val listener = androidx.core.util.Consumer<android.content.Intent> { newIntent ->
                    handleDestination(newIntent, navController)
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }

            val accentColor by prefsRepository.accentColor.collectAsState()

            // REQUEST PERMISSIONS LOGIC (Fix for Android 13+ & 12+)
            val context = androidx.compose.ui.platform.LocalContext.current
            
            // 1. Notification Permission Launcher (Android 13+)
            val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    if (isGranted) {
                         // Only reschedule if granted now to avoid redundant calls
                         notificationScheduler.rescheduleAllAlarms()
                    }
                }
            )

            // Exact Alarm Settings Launcher (Android 12+): opens system settings page
            // ACTION_REQUEST_SCHEDULE_EXACT_ALARM is not a runtime permission, it's a settings intent.
            val exactAlarmLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                onResult = { _ ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
                        if (alarmManager.canScheduleExactAlarms()) {
                            notificationScheduler.rescheduleAllAlarms()
                            android.util.Log.i("MainActivity", "Exact alarm permission granted; rescheduled alarms")
                        } else {
                            android.util.Log.w("MainActivity", "Exact alarm permission still missing")
                        }
                    }
                }
            )

            val permissionsExplanationShown by prefsRepository.permissionsExplanationShown.collectAsState()
            var showPermissionsExplainDialog by remember { mutableStateOf(false) }

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // 2. Check and Request Permissions on Arrival to Main HomeScreen for the first time
            LaunchedEffect(onboardingCompleted, permissionsExplanationShown, currentRoute) {
                if (onboardingCompleted && !permissionsExplanationShown && currentRoute == "home") {
                    var needsNotification = false
                    var needsAlarm = false

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        needsNotification = ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
                        needsAlarm = !alarmManager.canScheduleExactAlarms()
                    }

                    if (needsNotification || needsAlarm) {
                        showPermissionsExplainDialog = true
                    } else {
                        prefsRepository.setPermissionsExplanationShown(true)
                    }
                }
            }
            
            
            // ... (navController logic)

            BrishTheme(
                darkTheme = useDarkTheme,
                isAmoled = isAmoled,
                dynamicColor = false, // Disable Material You to enforce the new Premium brand identity
                accentColor = if(accentColor == android.graphics.Color.parseColor("#06B6D4")) null else accentColor
            ) {
                val configuration = LocalConfiguration.current
                val contextLocale = LocalContext.current
                
                val currentLocale = java.util.Locale(currentLanguage)
                configuration.setLocale(currentLocale)
                
                val configurationContext = contextLocale.createConfigurationContext(configuration)
                val wrappedContext = object : android.content.ContextWrapper(contextLocale) {
                    override fun getResources() = configurationContext.resources
                }
                
                CompositionLocalProvider(
                    LocalConfiguration provides configuration,
                    LocalContext provides wrappedContext
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = "splash"
                        ) {
                        composable(
                            "splash",
                            exitTransition = { 
                                scaleOut(
                                    targetScale = 1.05f,
                                    animationSpec = tween(500, easing = EaseOutQuart)
                                ) + fadeOut(animationSpec = tween(500, easing = EaseOutQuart))
                            }
                        ) {
                            SplashScreen(
                                onSplashFinished = {
                                    val nextDestination = when {
                                        !onboardingCompleted -> "onboarding"
                                        user == null -> "login"
                                        else -> "home"
                                    }
                                    navController.navigate(nextDestination) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }
                        val slideSpring = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.85f, stiffness = 300f)
                        val scaleSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 300f)
                        
                        composable(
                            "onboarding",
                            enterTransition = { 
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(500, easing = EaseOutQuart)
                                ) + fadeIn(animationSpec = tween(500, easing = EaseOutQuart))
                            },
                            exitTransition = { 
                                scaleOut(
                                    targetScale = 0.92f,
                                    animationSpec = tween(400, easing = EaseOutQuart)
                                ) + fadeOut(animationSpec = tween(400, easing = EaseOutQuart))
                            },
                            popEnterTransition = { 
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(400, easing = EaseOutQuart)
                                ) + fadeIn(animationSpec = tween(400, easing = EaseOutQuart))
                            },
                            popExitTransition = { 
                                scaleOut(
                                    targetScale = 1.05f,
                                    animationSpec = tween(400, easing = EaseOutQuart)
                                ) + fadeOut(animationSpec = tween(400, easing = EaseOutQuart))
                            }
                        ) {
                            OnboardingScreen(
                                onFinish = {
                                    prefsRepository.setOnboardingCompleted(true)
                                    navController.navigate(if (user == null) "login" else "home") {
                                        popUpTo("onboarding") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(
                            "login",
                            enterTransition = { 
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(500, easing = EaseOutQuart)
                                ) + fadeIn(animationSpec = tween(500, easing = EaseOutQuart))
                            },
                            exitTransition = { 
                                scaleOut(
                                    targetScale = 0.92f,
                                    animationSpec = tween(400, easing = EaseOutQuart)
                                ) + fadeOut(animationSpec = tween(400, easing = EaseOutQuart))
                            },
                            popEnterTransition = { 
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(400, easing = EaseOutQuart)
                                ) + fadeIn(animationSpec = tween(400, easing = EaseOutQuart))
                            },
                            popExitTransition = { 
                                scaleOut(
                                    targetScale = 1.05f,
                                    animationSpec = tween(400, easing = EaseOutQuart)
                                ) + fadeOut(animationSpec = tween(400, easing = EaseOutQuart))
                            }
                        ) {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(
                            "home",
                            enterTransition = { 
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(500, easing = EaseOutQuart)
                                ) + fadeIn(animationSpec = tween(500, easing = EaseOutQuart))
                            },
                            exitTransition = {
                                scaleOut(
                                    targetScale = 0.92f,
                                    animationSpec = tween(400, easing = EaseOutQuart)
                                ) + fadeOut(animationSpec = tween(400, easing = EaseOutQuart))
                            },
                            popEnterTransition = { 
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(400, easing = EaseOutQuart)
                                ) + fadeIn(animationSpec = tween(400, easing = EaseOutQuart))
                            }
                        ) {
                            // MainScreen maneja su propio NavHost interno (home_panel, planner, etc.)
                            // Usuario anónimo (sin login social) se considera invitado.
                            MainScreen(
                                isGuest = user?.isAnonymous == true,
                                onNavigateToLogin = { 
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                            )
                            }
                        }
                        
                        // Custom In-App Notification Banner Overlay
                        var activeNotification by remember { mutableStateOf<InAppNotification?>(null) }
                        LaunchedEffect(Unit) {
                            InAppNotificationBus.notifications.collect { notification ->
                                activeNotification = notification
                            }
                        }

                        AnimatedVisibility(
                            visible = activeNotification != null,
                            enter = slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = spring(
                                    dampingRatio = 0.8f,
                                    stiffness = 400f
                                )
                            ) + fadeIn(animationSpec = tween(150)),
                            exit = slideOutVertically(
                                targetOffsetY = { -it },
                                animationSpec = spring(
                                    dampingRatio = 1f,
                                    stiffness = 500f
                                )
                            ) + fadeOut(animationSpec = tween(150)),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                                .zIndex(99f)
                        ) {
                            activeNotification?.let { notif ->
                                LaunchedEffect(notif) {
                                    delay(notif.durationMillis)
                                    activeNotification = null
                                }
                                
                                val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                                
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = if (isDarkTheme) Color(0xFF1C1E26) else Color.White,
                                    shadowElevation = 6.dp,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color(0xFFE5E7EB)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeNotification = null }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = notif.message,
                                            color = if (isDarkTheme) Color.White else Color(0xFF111827),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (notif.actionLabel != null && notif.onAction != null) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = notif.actionLabel,
                                                color = if (isDarkTheme) Color.Black else Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(100.dp))
                                                    .background(if (isDarkTheme) Color.White else Color(0xFF111827))
                                                    .clickable {
                                                        notif.onAction.invoke()
                                                        activeNotification = null
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showPermissionsExplainDialog) {
                            com.katchy.focuslive.ui.components.PermissionsExplainDialog(
                                onDismiss = {
                                    showPermissionsExplainDialog = false
                                    prefsRepository.setPermissionsExplanationShown(true)
                                },
                                onConfirm = {
                                    showPermissionsExplainDialog = false
                                    prefsRepository.setPermissionsExplanationShown(true)

                                    // Request Notification permission on Android 13+ (API 33)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!hasPermission) {
                                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }

                                    // Exact Alarm Settings Launcher (Android 12+): transparent request to system settings
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                        val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
                                        if (!alarmManager.canScheduleExactAlarms()) {
                                            val intent = android.content.Intent(
                                                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                            ).apply {
                                                data = android.net.Uri.parse("package:" + context.packageName)
                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            runCatching { exactAlarmLauncher.launch(intent) }
                                                .onFailure { android.util.Log.e("MainActivity", "Could not launch exact-alarm settings", it) }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        userStatsRepository.checkDailyLogin()
    }
    
    private fun handleDestination(intent: android.content.Intent, navController: androidx.navigation.NavController) {
        val destination = intent.getStringExtra("destination") 
            ?: intent.getStringExtra("navigation_destination") 
            ?: return
        
        // Mapear claves externas a rutas internas de MainScreen NavHost
        val targetRoute = when(destination) {
            "notes" -> com.katchy.focuslive.ui.navigation.Screen.Notes.route
            "planner" -> com.katchy.focuslive.ui.navigation.Screen.Planner.route
            "habits" -> com.katchy.focuslive.ui.navigation.Screen.Habits.route
            "finance" -> com.katchy.focuslive.ui.navigation.Screen.Finance.route
            "focus" -> com.katchy.focuslive.ui.navigation.Screen.FocusMode.route 
            else -> null
        }
        
        // Enviar la ruta de destino a través de NavigationBus para que MainScreen la procese
        if (targetRoute != null) {
            NavigationBus.postDestination(targetRoute)
        }
    }
}

// Simple Event Bus for Navigation
object NavigationBus {
    private val _destination = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val destination = _destination.asStateFlow()

    fun postDestination(dest: String) {
        _destination.value = dest
    }

    fun consume() {
        _destination.value = null
    }
}
