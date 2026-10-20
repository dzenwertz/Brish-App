package com.katchy.focuslive.ui.auth

import android.content.Context
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.katchy.focuslive.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.katchy.focuslive.ui.components.BrishArt
import com.katchy.focuslive.ui.components.BrishArtKind
import com.katchy.focuslive.ui.components.brishPrimaryActionColor
import com.katchy.focuslive.ui.components.brishPrimaryActionContentColor
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.AreaHealthBg
import com.katchy.focuslive.ui.theme.AreaHome
import com.katchy.focuslive.ui.theme.AreaHomeBg
import com.katchy.focuslive.ui.theme.AreaWork
import com.katchy.focuslive.ui.theme.AreaWorkBg
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.InAppNotificationBus
import com.katchy.focuslive.ui.util.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var triggerSignIn by remember { mutableStateOf(false) }
    val user by viewModel.user.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isDark = isDarkTheme()

    LaunchedEffect(user) {
        if (user != null) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(triggerSignIn) {
        if (triggerSignIn) {
            try {
                signInWithGoogle(context, viewModel)
            } finally {
                triggerSignIn = false
            }
        }
    }

    LaunchedEffect(error) {
        if (error != null) {
            InAppNotificationBus.post(error ?: "Error desconocido")
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(420)) + slideInVertically(spring(dampingRatio = 0.85f, stiffness = 300f)) { it / 8 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(12.dp))
                BrishArt(BrishArtKind.Sun, Modifier.size(220.dp))
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Brish",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tu día, tu semana y tus ideas.\nEn un solo lugar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(26.dp))

                Column(
                    Modifier.fillMaxWidth().brishCard().padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    LoginBenefit(Icons.Rounded.WbSunny, AreaHomeBg, AreaHome, "Hoy te dice qué toca ahora")
                    LoginBenefit(Icons.Rounded.Bolt, AreaWorkBg, AreaWork, "Captura tareas escribiendo como hablas")
                    LoginBenefit(Icons.Rounded.Sync, AreaHealthBg, AreaHealth, "Tus datos sincronizados entre teléfonos")
                }
                Spacer(Modifier.height(26.dp))

                AnimatedContent(loading || triggerSignIn, label = "loginActions") { busy ->
                    if (busy) {
                        Box(Modifier.fillMaxWidth().height(124.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(36.dp))
                        }
                    } else {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { triggerSignIn = true },
                                shape = BrishShapes.pill,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brishPrimaryActionColor(),
                                    contentColor = brishPrimaryActionContentColor()
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .brishShadow(BrishShapes.pill, elevation = 12.dp, tone = BrishInk, strength = if (isDark) 0f else 1.2f)
                                    .bounceClick()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("Continuar con Google", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.signInAnonymously() },
                                shape = BrishShapes.pill,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick()
                            ) {
                                Text("Probar sin cuenta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Sin cuenta puedes empezar ya. Inicia sesión con Google para no perder tus datos si cambias de teléfono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LoginBenefit(icon: ImageVector, bg: Color, fg: Color, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(19.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

private suspend fun signInWithGoogle(context: Context, viewModel: AuthViewModel) {
    val webClientId = context.getString(R.string.default_web_client_id)
    val runtimeSigningSha1 = getRuntimeSigningSha1(context)
    try {
        val credentialManager = CredentialManager.create(context)
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = context
        )
        
        val credential = result.credential
        val firebaseCredential = when {
            credential is GoogleIdTokenCredential -> {
                GoogleAuthProvider.getCredential(credential.idToken, null)
            }
            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                 val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                 GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            }
            else -> {
                Log.e("LoginScreen", "Unexpected credential type: ${credential::class.java.name}")
                return
            }
        }
        viewModel.signIn(firebaseCredential)
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        if (e is GetCredentialCancellationException) return
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("auth_provider", "google")
            setCustomKey("auth_stage", "credential_manager")
            setCustomKey("package_name", context.packageName)
            setCustomKey("web_client_id", webClientId)
            setCustomKey("runtime_signing_sha1", runtimeSigningSha1 ?: "unknown")
            log("Google sign-in failed before FirebaseAuth")
            recordException(e)
        }
        Log.e("LoginScreen", "Runtime signing SHA1: ${runtimeSigningSha1 ?: "unknown"}")
        Log.e("LoginScreen", "Login Error", e)
        when (e) {
            is NoCredentialException -> {
                InAppNotificationBus.post(
                    "Google no encontró una cuenta disponible. Comprueba tu cuenta en los ajustes del dispositivo e inténtalo otra vez."
                )
            }
            is GetCredentialException -> {
                InAppNotificationBus.post(
                    "${context.getString(R.string.login_error_credentials)} ${e.message}"
                )
            }
            else -> {
                InAppNotificationBus.post(
                    "${context.getString(R.string.login_error_general)} ${e.message}"
                )
            }
        }
    }
}

private fun getRuntimeSigningSha1(context: Context): String? {
    return runCatching {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            )
        }

        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures.orEmpty()
        }

        val firstSignature = signatures.firstOrNull()?.toByteArray() ?: return null
        val digest = MessageDigest.getInstance("SHA1").digest(firstSignature)
        digest.joinToString(":") { byte -> "%02X".format(byte) }
    }.getOrNull()
}
