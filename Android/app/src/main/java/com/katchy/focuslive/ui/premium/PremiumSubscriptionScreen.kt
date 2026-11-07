package com.katchy.focuslive.ui.premium

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.repository.PurchaseState
import com.katchy.focuslive.ui.components.BrishCardSurface
import com.katchy.focuslive.ui.components.BrishIconTile
import com.katchy.focuslive.ui.components.BrishPrimaryButton
import com.katchy.focuslive.ui.components.BrishRoundButton
import com.katchy.focuslive.ui.components.BrishScreenHeader
import com.katchy.focuslive.ui.components.BrishSecondaryButton
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishYellowSoft
import com.katchy.focuslive.ui.util.InAppNotificationBus

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrishRoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", onBack)
            Spacer(Modifier.width(12.dp))
            BrishScreenHeader("Estado de suscripción", Modifier.weight(1f))
        }

        BrishCardSurface(
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrishIconTile(Icons.Rounded.WorkspacePremium, BrishYellowSoft, BrishInk, size = 44.dp)
                    Text(
                        if (isPremium) "Tu cuenta tiene una suscripción Pro registrada." else "Tu cuenta no tiene una suscripción Pro registrada.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    "Las funciones Pro no están disponibles en esta versión. Las nuevas compras están deshabilitadas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        BrishPrimaryButton(
            text = "Restaurar compras",
            onClick = viewModel::restoreActivePurchases,
            enabled = purchaseState !is PurchaseState.Loading,
            modifier = Modifier.fillMaxWidth()
        )

        if (isPremium) {
            BrishSecondaryButton(
                text = "Administrar en Google Play",
                onClick = {
                    val uri = Uri.parse("https://play.google.com/store/account/subscriptions?package=${context.packageName}")
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (e: android.content.ActivityNotFoundException) {
                        InAppNotificationBus.post("No se pudo abrir Google Play para administrar la suscripción.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
