package com.katchy.focuslive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.katchy.focuslive.ui.components.BrishCardSurface
import com.katchy.focuslive.ui.components.BrishPrimaryButton
import com.katchy.focuslive.ui.components.BrishScreenHeader
import com.katchy.focuslive.ui.components.BrishSecondaryButton
import com.katchy.focuslive.ui.theme.BrishTheme

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val errorTrace = intent.getStringExtra("EXTRA_ERROR_TRACE") ?: "Error desconocido"
        try {
            java.io.File(filesDir, "crash_log.txt").delete()
        } catch (_: Exception) {
        }

        setContent {
            BrishTheme(dynamicColor = false) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .systemBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BrishScreenHeader(
                        title = "Algo salió mal",
                        overline = "DIAGNÓSTICO DE BRISH"
                    )
                    Text(
                        text = "Tus notas, tareas y borradores siguen guardados en tu teléfono. Puedes copiar el detalle técnico o volver a abrir la app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BrishCardSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = errorTrace,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    BrishPrimaryButton(
                        text = "Copiar detalle del error",
                        leadingIcon = Icons.Rounded.ContentCopy,
                        onClick = {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Crash Log", errorTrace)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this@CrashActivity, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    BrishSecondaryButton(
                        text = "Volver a abrir Brish",
                        leadingIcon = Icons.Rounded.Refresh,
                        onClick = {
                            val restartIntent = Intent(this@CrashActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(restartIntent)
                            finish()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }

    companion object {
        fun start(context: Context, trace: String) {
            val truncatedTrace = if (trace.length > 50000) {
                trace.take(50000) + "\n\n[TRUNCATED due to size...]"
            } else {
                trace
            }
            val intent = Intent(context, CrashActivity::class.java).apply {
                putExtra("EXTRA_ERROR_TRACE", truncatedTrace)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }
}
