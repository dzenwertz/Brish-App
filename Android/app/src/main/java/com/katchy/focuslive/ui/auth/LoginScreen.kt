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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.katchy.focuslive.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import com.katchy.focuslive.ui.theme.brishEdgeHighlight
import com.katchy.focuslive.ui.theme.rememberReducedMotion
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.katchy.focuslive.ui.components.brishPrimaryActionColor
import com.katchy.focuslive.ui.components.brishPrimaryActionContentColor
import com.katchy.focuslive.ui.components.DraftRecoveryNotice
import com.katchy.focuslive.ui.components.brishSharedElement
import com.katchy.focuslive.ui.components.rememberPersistentDraftText
import com.katchy.focuslive.ui.theme.AreaHealth
import com.katchy.focuslive.ui.theme.AreaHealthBg
import com.katchy.focuslive.ui.theme.AreaHome
import com.katchy.focuslive.ui.theme.AreaHomeBg
import com.katchy.focuslive.ui.theme.AreaWork
import com.katchy.focuslive.ui.theme.AreaWorkBg
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishCream
import com.katchy.focuslive.ui.theme.BrishCard
import com.katchy.focuslive.ui.theme.BrishInsetSoft
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishTextTertiary
import com.katchy.focuslive.ui.theme.BrishYellow
import com.katchy.focuslive.ui.theme.MotionTokens
import com.katchy.focuslive.ui.theme.brishCascade
import com.katchy.focuslive.ui.theme.brishCard
import com.katchy.focuslive.ui.theme.brishShadow
import com.katchy.focuslive.ui.theme.isDarkTheme
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import com.katchy.focuslive.ui.util.InAppNotificationBus
import com.katchy.focuslive.ui.util.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.katchy.focuslive.ui.theme.BrishYellowDeep
import com.katchy.focuslive.ui.theme.BrishNight
import com.katchy.focuslive.ui.theme.BrishNightCard
import com.katchy.focuslive.ui.theme.BrishNightInset
import com.katchy.focuslive.ui.theme.BrishNightText
import com.katchy.focuslive.ui.theme.BrishNightTextSecondary

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    initialRegister: Boolean = false,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var triggerSignIn by remember { mutableStateOf(false) }
    val user by viewModel.user.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Respaldo: el selector clásico de Google, para equipos donde Credential Manager no muestra nada.
    val classicLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.signIn(GoogleAuthProvider.getCredential(idToken, null))
            } else {
                InAppNotificationBus.post("Google no devolvió tu cuenta. Inténtalo otra vez.")
            }
        } catch (e: ApiException) {
            if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                FirebaseCrashlytics.getInstance().apply {
                    setCustomKey("auth_stage", "classic_google_sign_in")
                    recordException(e)
                }
                InAppNotificationBus.post(classicErrorMessage(e.statusCode))
            }
        }
    }

    LaunchedEffect(triggerSignIn) {
        if (triggerSignIn) {
            try {
                if (signInWithGoogle(context, viewModel) == GoogleResult.UseClassic) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(context.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(activity, options)
                        // Cierra la sesión previa del cliente para que siempre pregunte qué cuenta usar.
                        runCatching { client.signOut().await() }
                        classicLauncher.launch(client.signInIntent)
                    }
                }
            } finally {
                triggerSignIn = false
            }
        }
    }

    val reduced = rememberReducedMotion()
    val emailDraft = rememberPersistentDraftText("login:email")
    var email by emailDraft::value
    var password by remember { mutableStateOf("") }
    var emailFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }
    var register by remember { mutableStateOf(initialRegister) }
    val reveal = remember { Animatable(0f) }
    val errorOffset = remember { Animatable(0f) }
    val headerWave = remember { Animatable(if (reduced) 1f else 0f) }
    val cardRise = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            launch {
                headerWave.animateTo(1f, tween(MotionTokens.screenMs, easing = MotionTokens.emphasized))
            }
            launch {
                delay(MotionTokens.staggerMs * 2)
                cardRise.animateTo(1f, tween(MotionTokens.screenMs, easing = MotionTokens.emphasized))
            }
        }
    }
    LaunchedEffect(user) {
        if (user != null) {
            emailDraft.clearAfterSave()
            if (!reduced) reveal.animateTo(1f, tween(450, easing = MotionTokens.emphasized))
            onLoginSuccess()
        }
    }
    LaunchedEffect(error) {
        if (error != null) {
            InAppNotificationBus.post(error.orEmpty())
            if (!reduced) repeat(3) {
                errorOffset.animateTo(6f, MotionTokens.pressSpring())
                errorOffset.animateTo(-6f, MotionTokens.pressSpring())
            }
            errorOffset.animateTo(0f, MotionTokens.pressSpring())
        }
    }

    val dark = isDarkTheme()
    val panelColor = if (dark) BrishNightCard else BrishCard
    val insetColor = if (dark) BrishNightInset else BrishInsetSoft
    val textColor = if (dark) BrishNightText else BrishInk
    val placeholderColor = if (dark) BrishNightTextSecondary else BrishTextTertiary
    val emailBorder by androidx.compose.animation.animateColorAsState(
        if (emailFocused) BrishYellow else Color.Transparent,
        tween(MotionTokens.microMs),
        label = "emailBorder"
    )
    val passwordBorder by androidx.compose.animation.animateColorAsState(
        if (passwordFocused) BrishYellow else Color.Transparent,
        tween(MotionTokens.microMs),
        label = "passwordBorder"
    )
    BoxWithConstraints(Modifier.fillMaxSize().background(if (dark) BrishNight else BrishCream)) {
        val animatedButtonWidth by animateDpAsState(
            if (loading || triggerSignIn) 56.dp else maxWidth,
            tween(MotionTokens.componentMs, easing = MotionTokens.emphasized),
            label = "loginButtonWidth"
        )
        val buttonWidth = if (reduced) if (loading || triggerSignIn) 56.dp else maxWidth else animatedButtonWidth
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = -(1f - headerWave.value) * 36.dp.toPx()
                        alpha = headerWave.value.coerceIn(0f, 1f)
                    }
                    .background(BrishYellow)
                    // Yellow runs under the status bar; content starts below it.
                    .statusBarsPadding()
                    .height(232.dp)
            ) {
                // Decorative corner blob sits behind the header content.
                Box(Modifier.align(Alignment.TopEnd).offset(x = 48.dp, y = (-42).dp).size(130.dp)
                    .clip(CircleShape).background(BrishYellowDeep.copy(alpha = 0.68f)))
                Image(
                    painter = painterResource(R.drawable.ic_brish_logo),
                    contentDescription = "Logo de Brish",
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 24.dp).size(38.dp)
                        .brishSharedElement("brish-logo")
                )
                // Like the reference: the opposite mode lives top-right.
                Text(if (register) "Iniciar sesión" else "Registrarme",
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 22.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = !loading && !triggerSignIn) { register = !register }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge, color = BrishInk)
                Column(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, end = 24.dp, bottom = 54.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (register) "Regístrate" else "Iniciar sesión",
                        style = MaterialTheme.typography.headlineLarge, color = BrishInk,
                        modifier = Modifier.brishCascade(0, register))
                    Text(if (register) "Crea tu cuenta para guardar tus datos en todos tus dispositivos."
                        else "Organiza tu día, tus cursos y tus ideas desde un solo lugar.",
                        style = MaterialTheme.typography.bodyMedium, color = BrishInk.copy(alpha = 0.84f),
                        modifier = Modifier.brishCascade(1, register))
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    // Card reaches the bottom edge instead of leaving a band.
                    .heightIn(min = this@BoxWithConstraints.maxHeight - 200.dp)
                    .offset(y = (-28).dp)
                    .graphicsLayer {
                        translationX = errorOffset.value * density
                        translationY = (1f - cardRise.value) * 24.dp.toPx()
                        alpha = cardRise.value.coerceIn(0f, 1f)
                    }
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(panelColor)
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DraftRecoveryNotice(emailDraft, Modifier.fillMaxWidth())
                TextField(
                    value = email, onValueChange = { email = it.trimStart() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { emailFocused = it.isFocused }
                        .border(1.5.dp, emailBorder, BrishShapes.pill),
                    singleLine = true, label = { Text("Correo electrónico", color = placeholderColor) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                    shape = BrishShapes.pill,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = insetColor, unfocusedContainerColor = insetColor,
                        disabledContainerColor = insetColor, focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor
                    )
                )
                TextField(
                    value = password, onValueChange = { password = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { passwordFocused = it.isFocused }
                        .border(1.5.dp, passwordBorder, BrishShapes.pill),
                    singleLine = true, label = { Text("Contraseña", color = placeholderColor) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                    shape = BrishShapes.pill,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = insetColor, unfocusedContainerColor = insetColor,
                        disabledContainerColor = insetColor, focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor
                    )
                )
                if (!register) TextButton(onClick = { viewModel.sendPasswordReset(email) }, modifier = Modifier.align(Alignment.End)) {
                    Text("¿Olvidaste tu contraseña?", color = textColor)
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.width(buttonWidth).height(54.dp).clip(BrishShapes.pill).background(BrishInk)
                            .clickable(enabled = !loading && !triggerSignIn && email.isNotBlank() && password.isNotBlank()) {
                                if (register) viewModel.registerWithEmail(email, password)
                                else viewModel.signInWithEmail(email, password)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (loading || triggerSignIn) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(23.dp))
                        else Text(if (register) "Crear cuenta" else "Iniciar sesión", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text("o", Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }
                GoogleButton(isDark = dark, onClick = { triggerSignIn = true })
                TextButton(onClick = { register = !register; password = "" }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (register) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate", color = textColor)
                }
                TextButton(onClick = { viewModel.signInAnonymously() }, enabled = !loading && !triggerSignIn,
                    modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Probar sin cuenta", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (user != null && reveal.value > 0f && !reduced) androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawCircle(if (dark) BrishNight else BrishCream, radius = size.maxDimension * reveal.value, center = Offset(size.width / 2f, size.height / 2f))
        }
    }
}
@Composable
private fun GoogleButton(isDark: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .brishShadow(BrishShapes.pill, elevation = 14.dp, tone = BrishInk, strength = if (isDark) 0f else 1.3f)
            .clip(BrishShapes.pill)
            .background(if (isDark) BrishNightCard else Color.White)
            .bounceClick(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_google_logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
        }
        Text(
            "Continuar con Google",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = if (isDark) BrishNightText else BrishInk
        )
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = if (isDark) BrishNightText else BrishInk, modifier = Modifier.size(24.dp))
    }
}

private enum class GoogleResult { Done, UseClassic }

/** Tiempo máximo que esperamos a que aparezca el selector de Credential Manager. */
private const val CredentialTimeoutMs = 15_000L

private fun classicErrorMessage(statusCode: Int): String = when (statusCode) {
    CommonStatusCodes.NETWORK_ERROR -> "Sin conexión. Revisa tu internet e inténtalo otra vez."
    CommonStatusCodes.DEVELOPER_ERROR ->
        "Google rechazó esta versión de la app (código 10). Revisa que la huella SHA-1 esté en Firebase."
    else -> "No se pudo iniciar sesión con Google (código $statusCode)."
}

private suspend fun signInWithGoogle(context: Context, viewModel: AuthViewModel): GoogleResult {
    val webClientId = context.getString(R.string.default_web_client_id)
    val runtimeSigningSha1 = getRuntimeSigningSha1(context)
    // El selector de cuentas necesita la Activity real; LocalContext es un envoltorio de idioma.
    val activity = context.findActivity()
    if (activity == null) {
        InAppNotificationBus.post("No se pudo abrir el inicio de sesión con Google. Vuelve a intentarlo.")
        return GoogleResult.Done
    }
    try {
        val credentialManager = CredentialManager.create(activity)

        // Opción del botón «Iniciar sesión con Google»: siempre muestra el selector de cuentas.
        val googleOption = GetSignInWithGoogleOption.Builder(webClientId).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        // En algunos teléfonos (p. ej. con HyperOS) la hoja nunca aparece y la llamada no vuelve:
        // si pasa el tiempo, abrimos el selector clásico de Google.
        val result = withTimeoutOrNull(CredentialTimeoutMs) {
            credentialManager.getCredential(request = request, context = activity)
        } ?: run {
            Log.w("LoginScreen", "Credential Manager no respondió; se usa el selector clásico")
            return GoogleResult.UseClassic
        }

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
                return GoogleResult.UseClassic
            }
        }
        viewModel.signIn(firebaseCredential)
        return GoogleResult.Done
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        if (e is GetCredentialCancellationException) return GoogleResult.Done
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
        // Cualquier fallo de Credential Manager (sin cuenta, proveedor ausente, etc.) se reintenta
        // con el selector clásico, que además da un código de error claro si algo está mal.
        return GoogleResult.UseClassic
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

private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
