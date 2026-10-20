package com.katchy.focuslive.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.katchy.focuslive.ui.theme.BrishHeroBrush
import com.katchy.focuslive.ui.theme.BrishOnYellow
import com.katchy.focuslive.ui.theme.BrishShapes
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.brishShadow

/** Pestaña Hábitos. Su contenido llega en la fase de pantallas; por ahora explica qué vendrá. */
@Composable
fun HabitsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Hábitos", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .brishShadow(BrishShapes.hero, strength = 1.4f)
                .background(BrishHeroBrush, BrishShapes.hero)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Muy pronto", style = MaterialTheme.typography.titleLarge, color = BrishInk)
            Text(
                "Metas semanales, contadores como el agua y marcar desde Hoy. Tus hábitos anteriores se conservan.",
                style = MaterialTheme.typography.bodyMedium,
                color = BrishOnYellow
            )
        }
    }
}
