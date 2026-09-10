package com.katchy.focuslive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

class CrashActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val errorTrace = intent.getStringExtra("EXTRA_ERROR_TRACE") ?: "Error desconocido"
        try {
            java.io.File(filesDir, "crash_log.txt").delete()
        } catch (_: Exception) {
        }
        
        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("💥 BRISH Crash Report") },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Red, titleContentColor = Color.White)
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "¡Por favor, envíame una captura de este error exacto para arreglarlo!", 
                            style = MaterialTheme.typography.titleMedium, 
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorTrace, 
                            fontFamily = FontFamily.Monospace, 
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Crash Log", errorTrace)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this@CrashActivity, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Copiar Error")
                        }
                    }
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
