package com.katchy.focuslive.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.katchy.focuslive.MainActivity
import com.katchy.focuslive.R
import androidx.glance.ImageProvider

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.LocalContext
import dagger.hilt.android.EntryPointAccessors
import com.katchy.focuslive.di.WidgetEntryPoint

class NotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val appContext = LocalContext.current.applicationContext
            val entryPoint = EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
            val repository = entryPoint.noteRepository()
            
            val notes by repository.getNotes().collectAsState(initial = emptyList())

            GlanceTheme {
                NotesContent(notes)
            }
        }
    }

    @Composable
    private fun NotesContent(notes: List<com.katchy.focuslive.data.model.Note>) {
        // Aesthetic Palette (Minimalist Monochrome Brish Theme)
        val inkColor = Color(0xFFF9FAFB)
        val accentColor = Color.White
        val secondaryTextColor = Color(0xFF9CA3AF)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_gradient_background))
                .appWidgetBackground()
                .padding(16.dp)
                .clickable(
                    actionStartActivity<MainActivity>(
                        actionParametersOf(ActionParameters.Key<String>("destination") to "notes")
                    )
                )
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Header Region
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Brish • Notas",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(inkColor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    // "Add" Icon Visual
                    Text(
                        text = "+",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(accentColor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                }
                
                Spacer(modifier = GlanceModifier.height(12.dp))
                
                // Content Region
                if (notes.isEmpty()) {
                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                          Text(
                            text = "Captura tus ideas y notas... 📝", 
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(secondaryTextColor),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        )
                    }
                } else {
                    Column(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        notes.take(4).forEach { note ->
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .cornerRadius(10.dp)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Bullet point
                                Box(
                                    modifier = GlanceModifier
                                        .size(6.dp)
                                        .background(Color.White.copy(alpha = 0.6f))
                                        .cornerRadius(3.dp)
                                ) {}
                                Spacer(modifier = GlanceModifier.width(10.dp))
                                // Note Text
                                Text(
                                    text = if (note.title.isNotBlank()) note.title else note.content,
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(inkColor),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}
