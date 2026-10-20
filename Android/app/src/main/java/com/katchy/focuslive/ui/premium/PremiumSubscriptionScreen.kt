package com.katchy.focuslive.ui.premium

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.repository.PurchaseState
import com.katchy.focuslive.ui.util.InAppNotificationBus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSubscriptionScreen(
    onBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val isPremium by viewModel.isPremium.collectAsState()
    val purchaseState by viewModel.purchaseState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(purchaseState) {
        if (purchaseState is PurchaseState.Error) {
            InAppNotificationBus.post((purchaseState as PurchaseState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estado de suscripción") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (isPremium) "Tu cuenta tiene una suscripción Pro registrada." else "Tu cuenta no tiene una suscripción Pro registrada.",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Las funciones Pro no están disponibles en esta versión. Las nuevas compras están deshabilitadas.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = viewModel::restoreActivePurchases,
                enabled = purchaseState !is PurchaseState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restaurar compras")
            }
            if (isPremium) {
                OutlinedButton(
                    onClick = {
                        val uri = Uri.parse("https://play.google.com/store/account/subscriptions?package=${context.packageName}")
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (e: android.content.ActivityNotFoundException) {
                            InAppNotificationBus.post("No se pudo abrir Google Play para administrar la suscripción.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Administrar en Google Play")
                }
            }
        }
    }
}
