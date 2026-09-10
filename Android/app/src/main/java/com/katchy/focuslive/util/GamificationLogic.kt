package com.katchy.focuslive.util

object GamificationLogic {
    
    // Badge IDs
    const val BADGE_EARLY_BIRD = "EARLY_BIRD" // Task before 7 AM
    const val BADGE_FINANCIER = "MASTER_FINANCIER" // 30 transactions
    const val BADGE_STREAK_3 = "STREAK_3"
    const val BADGE_STREAK_7 = "STREAK_7"
    const val BADGE_STREAK_30 = "STREAK_30"
    
    const val BADGE_POMODORO_1 = "POMODORO_1"
    const val BADGE_POMODORO_5 = "POMODORO_5"
    const val BADGE_HABIT_MASTER = "HABIT_MASTER"
    const val BADGE_PLANNER_PRO = "PLANNER_PRO"
    const val BADGE_NOTE_TAKER = "NOTE_TAKER"
    const val BADGE_PREMIUM_MEMBER = "PREMIUM_MEMBER"

    fun getBadgeIcon(badgeId: String): String {
        return when(badgeId) {
            BADGE_EARLY_BIRD -> "🌅"
            BADGE_FINANCIER -> "💰"
            BADGE_STREAK_3 -> "🔥"
            BADGE_STREAK_7 -> "⚡"
            BADGE_STREAK_30 -> "👺"
            BADGE_POMODORO_1 -> "⏱️"
            BADGE_POMODORO_5 -> "🧠"
            BADGE_HABIT_MASTER -> "📅"
            BADGE_PLANNER_PRO -> "🎯"
            BADGE_NOTE_TAKER -> "✍️"
            BADGE_PREMIUM_MEMBER -> "👑"
            else -> "🎖️"
        }
    }
    
    fun getBadgeName(badgeId: String): String {
        return when(badgeId) {
            BADGE_EARLY_BIRD -> "El Madrugador"
            BADGE_FINANCIER -> "Financiero Maestro"
            BADGE_STREAK_3 -> "En Racha (3)"
            BADGE_STREAK_7 -> "Imparable (7)"
            BADGE_STREAK_30 -> "Leyenda (30)"
            BADGE_POMODORO_1 -> "Primer Enfoque"
            BADGE_POMODORO_5 -> "Enfoque Profundo"
            BADGE_HABIT_MASTER -> "Creador de Hábitos"
            BADGE_PLANNER_PRO -> "Planificador Pro"
            BADGE_NOTE_TAKER -> "Escribano Real"
            BADGE_PREMIUM_MEMBER -> "Miembro VIP"
            else -> "Insignia"
        }
    }
}
