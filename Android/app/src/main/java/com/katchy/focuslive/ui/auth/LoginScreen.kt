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

    val benefits = remember {
        listOf(
            LoginBenefitInfo(R.drawable.art_sun, AreaHomeBg, AreaHome, "Tu día claro", "Hoy te dice qué toca.", "En Hoy ves lo que toca ahora y lo que viene después."),
            LoginBenefitInfo(R.drawable.art_bolt, AreaWorkBg, AreaWork, "Captura rápida", "Escribe como hablas.", "Toca el ＋ y escribe «dentista mañana 5pm»: Brish lo entiende."),
            LoginBenefitInfo(R.drawable.art_cloud, AreaHealthBg, AreaHealth, "Siempre contigo", "Con o sin internet.", "Todo se guarda en el teléfono y se sincroniza al volver la conexión.")
        )
    }
    // La tarjeta destacada rota sola; los puntos de abajo la siguen.
    var highlighted by remember { mutableIntStateOf(0) }
    val reduced = rememberReducedMotion()
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        while (true) {
            delay(3200)
            highlighted = (highlighted + 1) % benefits.size
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LoginClouds(Modifier.fillMaxWidth().height(420.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(420)) + slideInVertically(spring(dampingRatio = 0.85f, stiffness = 300f)) { it / 8 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    LoginHero(Modifier.fillMaxWidth().padding(horizontal = 12.dp).aspectRatio(1.5f))
                    BrishWordmark()
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Tu día, tu semana y tus ideas.\nEn un solo lugar.",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 25.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))

                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        benefits.forEachIndexed { index, info ->
                            LoginBenefit(
                                info = info,
                                index = index,
                                highlighted = highlighted == index,
                                onClick = {
                                    highlighted = index
                                    InAppNotificationBus.post(info.detail)
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    PageDots(count = benefits.size, selected = highlighted)

                    // Plantas a los lados y una loma crema que sostiene los botones.
                    Box(Modifier.fillMaxWidth()) {
                        Image(
                            painterResource(R.drawable.art_foliage_left), contentDescription = null,
                            modifier = Modifier.align(Alignment.TopStart).offset(x = (-18).dp, y = (-64).dp).size(150.dp)
                        )
                        Image(
                            painterResource(R.drawable.art_foliage_right), contentDescription = null,
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 18.dp, y = (-56).dp).size(130.dp)
                        )
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp)
                                .background(MaterialTheme.colorScheme.surface, LoginHillShape)
                                .padding(start = 24.dp, end = 24.dp, top = 34.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AnimatedContent(loading || triggerSignIn, label = "loginActions") { busy ->
                                if (busy) {
                                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(32.dp))
                                    }
                                } else {
                                    GoogleButton(isDark = isDark, onClick = { triggerSignIn = true })
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                Text("o", modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            TextButton(
                                onClick = { viewModel.signInAnonymously() },
                                enabled = !loading && !triggerSignIn
                            ) {
                                Text("Probar sin cuenta", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(
                                text = "Sin cuenta puedes empezar ya. Inicia sesión con Google para no perder tus datos si cambias de teléfono.",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class LoginBenefitInfo(
    @androidx.annotation.DrawableRes val art: Int,
    val bg: Color,
    val fg: Color,
    val title: String,
    val caption: String,
    val detail: String
)

/** Borde superior en loma suave para el panel de los botones. */
private val LoginHillShape = GenericShape { size, _ ->
    val rise = size.height.coerceAtMost(size.width) * 0.09f
    moveTo(0f, rise)
    cubicTo(size.width * 0.3f, -rise * 0.4f, size.width * 0.7f, -rise * 0.4f, size.width, rise)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

/** Ilustración principal: flota despacio sobre su sombra. */
@Composable
private fun LoginHero(modifier: Modifier) {
    val reduced = rememberReducedMotion()
    val float = rememberInfiniteTransition(label = "heroFloat")
    val bob by float.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroBob"
    )
    Image(
        painter = painterResource(R.drawable.art_login_hero),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer { translationY = -6.dp.toPx() * bob }
    )
}

/** Nubes crema muy suaves detrás del encabezado. */
@Composable
private fun LoginClouds(modifier: Modifier) {
    val cloud = if (isDarkTheme()) Color.White.copy(alpha = 0.03f) else Color(0xFFEFE6D2).copy(alpha = 0.7f)
    Canvas(modifier) {
        fun puff(cx: Float, cy: Float, r: Float) {
            drawCircle(cloud, r, Offset(cx, cy))
            drawCircle(cloud, r * 0.75f, Offset(cx - r * 0.9f, cy + r * 0.25f))
            drawCircle(cloud, r * 0.7f, Offset(cx + r * 0.95f, cy + r * 0.3f))
        }
        puff(size.width * 0.04f, size.height * 0.28f, size.width * 0.12f)
        puff(size.width * 0.97f, size.height * 0.5f, size.width * 0.1f)
        puff(size.width * 0.95f, size.height * 0.95f, size.width * 0.08f)
    }
}

/** «Brish» grande con su chispa amarilla, como el logo. */
@Composable
private fun BrishWordmark() {
    Box {
        Text(
            text = "Brish",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, letterSpacing = (-2).sp, lineHeight = 70.sp),
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Sparkle(BrishYellowDeep, Modifier.align(Alignment.TopEnd).offset(x = 26.dp, y = 2.dp).size(30.dp))
    }
}

/** Tres trazos cortos que salen de una esquina, como un destello. */
@Composable
private fun Sparkle(color: Color, modifier: Modifier, mirrored: Boolean = false) {
    Canvas(modifier) {
        val w = size.width
        val stroke = w * 0.16f
        val lines = listOf(
            Offset(0.18f, 0.62f) to Offset(0.42f, 0.18f),
            Offset(0.5f, 0.82f) to Offset(0.92f, 0.62f)
        )
        lines.forEach { (a, b) ->
            val ax = if (mirrored) 1f - a.x else a.x
            val bx = if (mirrored) 1f - b.x else b.x
            drawLine(color, Offset(ax * w, a.y * w), Offset(bx * w, b.y * w), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun PageDots(count: Int, selected: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == selected) 22.dp else 8.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "dot")
            Box(
                Modifier
                    .size(width, 8.dp)
                    .clip(CircleShape)
                    .background(if (i == selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant)
            )
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
            .background(if (isDark) Color.White else BrishInk)
            .bounceClick(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_google_logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
        }
        Text(
            "Iniciar sesión con Google",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = if (isDark) BrishInk else Color.White
        )
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = if (isDark) BrishInk else Color.White, modifier = Modifier.size(24.dp))
    }
}

/** Tarjeta de beneficio: entra escalonada, su ilustración flota y crece cuando está destacada. */
@Composable
private fun LoginBenefit(info: LoginBenefitInfo, index: Int, highlighted: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val reduced = rememberReducedMotion()
    var shown by remember { mutableStateOf(reduced) }
    LaunchedEffect(Unit) { delay(220L + 110L * index); shown = true }
    val appear by animateFloatAsState(if (shown) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 380f), label = "benefit")
    val lift by animateFloatAsState(if (highlighted) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "lift")
    val float = rememberInfiniteTransition(label = "benefitFloat")
    val bob by float.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(2600 + 300 * index, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val shape = RoundedCornerShape(26.dp)
    val fg = info.fg
    Column(
        modifier
            .graphicsLayer {
                alpha = appear
                val s = (0.85f + 0.15f * appear) * (1f + 0.04f * lift)
                scaleX = s
                scaleY = s
                translationY = (1f - appear) * 24.dp.toPx() - 6.dp.toPx() * lift
            }
            .brishShadow(shape, elevation = 10.dp, tone = fg, strength = 0.5f + 0.5f * lift)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(info.bg, info.bg.copy(alpha = 0.5f).compositeOver(MaterialTheme.colorScheme.surface))))
            .brishEdgeHighlight(inset = 14.dp)
            .clickable(role = Role.Button, onClickLabel = info.title, onClick = onClick)
            .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Sparkle(fg.copy(alpha = 0.8f), Modifier.align(Alignment.TopStart).offset(x = (-14).dp, y = (-8).dp).size(22.dp), mirrored = true)
            Box(
                Modifier
                    .graphicsLayer { translationY = -4.dp.toPx() * bob }
                    .size(64.dp)
                    .brishShadow(CircleShape, elevation = 8.dp, tone = fg, strength = 0.8f)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(info.art), contentDescription = null, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(info.title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = fg, textAlign = TextAlign.Center, lineHeight = 19.sp)
        Spacer(Modifier.height(6.dp))
        Text(info.caption, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = fg.copy(alpha = 0.8f), textAlign = TextAlign.Center, lineHeight = 16.sp)
        Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
        Box(
            Modifier.fillMaxWidth().height(40.dp).clip(BrishShapes.pill).background(fg.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        }
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
