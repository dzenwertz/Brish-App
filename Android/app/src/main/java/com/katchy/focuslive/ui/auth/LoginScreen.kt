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
import com.katchy.focuslive.ui.theme.AntiPrimary
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
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF101216) else Color(0xFFFAFAFC))
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 32.dp)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 40 },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Logo Container
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isDark) Color(0xFF1C1E24) else Color(0xFFEEF2F6))
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_app_logo_vector),
                        contentDescription = "Brish Logo",
                        tint = if (isDark) Color.White else Color(0xFF111827),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Title & Subtitle
                Text(
                    text = "Brish",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = if (isDark) Color.White else Color(0xFF111827)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Un lugar para estudiar, organizarte y avanzar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                if (loading || triggerSignIn) {
                    CircularProgressIndicator(
                        color = if (isDark) Color.White else Color(0xFF111827),
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Google Sign-In Button (Solid Pill)
                        Button(
                            onClick = { triggerSignIn = true },
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color.White else Color(0xFF111827),
                                contentColor = if (isDark) Color.Black else Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .bounceClick()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = "Google Logo",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Continuar con Google",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // Guest Mode Button (Outlined Pill)
                        OutlinedButton(
                            onClick = { viewModel.signInAnonymously() },
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.20f) else Color(0xFFD1D5DB)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDark) Color.White else Color(0xFF111827)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .bounceClick()
                        ) {
                            Text(
                                text = "Continuar como invitado",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Minimal Footer
        Text(
            text = "Tu siguiente paso empieza aquí.",
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) Color.White.copy(alpha = 0.3f) else Color(0xFF9CA3AF),
            modifier = Modifier
                .padding(top = 32.dp, start = 28.dp, end = 28.dp),
            textAlign = TextAlign.Center
        )
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
