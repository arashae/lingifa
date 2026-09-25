package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Daily practice record stored in Room to track consecutive daily activity,
 * study minutes, items reviewed/learned, and gamified XP.
 */
@Entity(tableName = "daily_streak_records")
data class DailyStreakRecord(
    @PrimaryKey
    val date: String, // Format: "YYYY-MM-DD", e.g., "2026-09-24"
    val timestamp: Long = System.currentTimeMillis(),
    val itemsPracticed: Int = 1,
    val minutesSpent: Int = 5,
    val xpEarned: Int = 15,
    val activityType: String = "VOCABULARY", // VOCABULARY, FLASHCARD, AI_WORD_CREATE, REVIEW, SPEAKING, GRAMMAR
    val isGoalMet: Boolean = true
)

/**
 * Aggregated streak and gamification state for the user.
 */
data class StreakInfo(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val isTodayCompleted: Boolean = false,
    val totalDaysPracticed: Int = 0,
    val totalXp: Int = 0,
    val weeklyDays: List<StreakDayItem> = emptyList(),
    val motivationalMessage: String = "",
    val streakLevel: String = "Starter",
    val nextMilestone: StreakMilestone? = null,
    val unlockedMilestones: List<StreakMilestone> = emptyList()
)

data class StreakDayItem(
    val date: String,
    val dayName: String, // "S", "M", "T", "W", "T", "F", "S"
    val dayNumber: String,
    val isCompleted: Boolean,
    val isToday: Boolean,
    val itemsCount: Int = 0
)

data class StreakMilestone(
    val requiredDays: Int,
    val title: String,
    val description: String,
    val badgeIcon: String,
    val isUnlocked: Boolean
)

data class StreakUpdateResult(
    val previousStreak: Int,
    val newStreak: Int,
    val xpEarned: Int,
    val isNewMilestoneReached: Boolean,
    val milestoneTitle: String? = null,
    val message: String
)
