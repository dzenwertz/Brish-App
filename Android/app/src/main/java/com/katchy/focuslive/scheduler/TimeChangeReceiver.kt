package com.katchy.focuslive.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimeChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIME_CHANGED || 
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            
            // Reschedule all notifications on time change to keep them accurate
            notificationScheduler.rescheduleAllAlarms()
            
            // Note: For active UI components (like PlannerScreen), they usually
            // react to configuration changes or resume events. 
            // In PlannerViewModel, we rely on System.currentTimeMillis() which updates automatically.
        }
    }
}
