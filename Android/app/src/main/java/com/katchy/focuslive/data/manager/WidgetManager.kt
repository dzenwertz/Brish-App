package com.katchy.focuslive.data.manager

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.katchy.focuslive.widget.TasksWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface WidgetManager {
    suspend fun updateTasksWidget()
}

@Singleton
class WidgetManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetManager {
    override suspend fun updateTasksWidget() {
        try {
           TasksWidget().updateAll(context)
        } catch (e: Exception) {
            android.util.Log.e("WidgetManager", "Exception caught", e)
        }
    }
}
