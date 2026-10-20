package com.katchy.focuslive.ui.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Resultado de pedir la huella o el bloqueo de pantalla. */
sealed interface UnlockResult {
    data object Success : UnlockResult
    data object Cancelled : UnlockResult
    data class Unavailable(val message: String) : UnlockResult
}

private const val Allowed = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

private tailrec fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Devuelve una función que muestra el diálogo del sistema (huella, rostro o PIN del teléfono)
 * y llama a [onResult]. Si el teléfono no tiene bloqueo de pantalla, lo informa en vez de abrir.
 */
@Composable
fun rememberUnlocker(): (title: String, onResult: (UnlockResult) -> Unit) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { title, onResult ->
            val activity = context.findActivity()
            val status = BiometricManager.from(context).canAuthenticate(Allowed)
            when {
                activity == null -> onResult(UnlockResult.Unavailable("No se pudo abrir el desbloqueo."))
                status != BiometricManager.BIOMETRIC_SUCCESS ->
                    onResult(UnlockResult.Unavailable("Configura una huella o un bloqueo de pantalla en tu teléfono para usar notas bloqueadas."))
                else -> {
                    val prompt = BiometricPrompt(
                        activity,
                        ContextCompat.getMainExecutor(activity),
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(UnlockResult.Success)
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(UnlockResult.Cancelled)
                        }
                    )
                    prompt.authenticate(
                        BiometricPrompt.PromptInfo.Builder()
                            .setTitle(title)
                            .setSubtitle("Usa tu huella o el bloqueo de tu teléfono")
                            .setAllowedAuthenticators(Allowed)
                            .build()
                    )
                }
            }
        }
    }
}

/** True si el teléfono tiene algún método para desbloquear notas. */
fun canLockNotes(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(Allowed) == BiometricManager.BIOMETRIC_SUCCESS
