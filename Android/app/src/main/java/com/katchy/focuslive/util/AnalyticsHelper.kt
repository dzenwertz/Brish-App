package com.katchy.focuslive.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun logScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logMascotAction(mascotId: String, action: String) {
        val bundle = Bundle().apply {
            putString("mascot_id", mascotId)
            putString("action", action)
        }
        firebaseAnalytics.logEvent("mascot_interaction", bundle)
    }

    fun logTaskAction(action: String) {
        val bundle = Bundle().apply {
            putString("action", action)
        }
        firebaseAnalytics.logEvent("task_action", bundle)
    }

    fun logHabitAction(action: String) {
        val bundle = Bundle().apply {
            putString("action", action)
        }
        firebaseAnalytics.logEvent("habit_action", bundle)
    }

    fun logNoteAction(action: String) {
        val bundle = Bundle().apply {
            putString("action", action)
        }
        firebaseAnalytics.logEvent("note_action", bundle)
    }

    fun logPremiumAction(action: String) {
        val bundle = Bundle().apply {
            putString("action", action)
        }
        firebaseAnalytics.logEvent("premium_action", bundle)
    }

    fun logAiAction(action: String) {
        val bundle = Bundle().apply {
            putString("action", action)
        }
        firebaseAnalytics.logEvent("ai_action", bundle)
    }
}
