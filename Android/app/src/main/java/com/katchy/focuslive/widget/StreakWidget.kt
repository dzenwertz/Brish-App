package com.katchy.focuslive.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.katchy.focuslive.MainActivity
import com.katchy.focuslive.R

class StreakWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val streak = prefs[intPreferencesKey("current_streak")] ?: 0

            // Motivational message based on streak count
            val motivationalText = when {
                streak == 0 -> "¡Empieza hoy!"
                streak == 1 -> "¡Gran inicio! 🔥"
                streak < 7 -> "¡Sigue así! 💪"
                streak < 30 -> "¡Imparable! 🚀"
                else -> "¡Leyenda! 👑"
            }

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ImageProvider(R.drawable.widget_gradient_background))
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Fire Icon
                        Image(
                            provider = ImageProvider(R.drawable.ic_fire_streak),
                            contentDescription = "Racha",
                            modifier = GlanceModifier.size(28.dp)
                        )

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        // Streak Number — big and bold
                        Text(
                            text = "$streak",
                            style = TextStyle(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFFF1F5F9)), // Slate 100
                                textAlign = TextAlign.Center
                            )
                        )

                        // "días" label
                        Text(
                            text = if (streak == 1) "día" else "días",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorProvider(Color(0xFF94A3B8)), // Slate 400
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Motivational text
                        Text(
                            text = motivationalText,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFFF1F5F9).copy(alpha = 0.9f)),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }
    }
}
