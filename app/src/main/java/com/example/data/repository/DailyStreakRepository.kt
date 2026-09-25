package com.example.data.repository

import com.example.data.local.DailyStreakDao
import com.example.data.local.UserProfileDao
import com.example.data.model.DailyStreakRecord
import com.example.data.model.StreakDayItem
import com.example.data.model.StreakInfo
import com.example.data.model.StreakMilestone
import com.example.data.model.StreakUpdateResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

class DailyStreakRepository(
    private val streakDao: DailyStreakDao,
    private val userProfileDao: UserProfileDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val allStreakRecords: Flow<List<DailyStreakRecord>> = streakDao.getAllRecords()
    val totalPracticeDays: Flow<Int> = streakDao.getTotalDaysCount()

    fun getTodayDateString(): String = dateFormat.format(Date())

    fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }

    /**
     * Flow emitting the real-time calculated streak state and gamification milestones.
     */
    val streakInfo: Flow<StreakInfo> = combine(
        streakDao.getAllRecords(),
        userProfileDao.getProfile()
    ) { records, profile ->
        calculateStreakInfo(records, profile?.xp ?: 0)
    }

    suspend fun getStreakInfoSync(): StreakInfo {
        val records = streakDao.getAllRecordsSync()
        val profile = userProfileDao.getProfileSync()
        return calculateStreakInfo(records, profile?.xp ?: 0)
    }

    /**
     * Record a practice activity (e.g. reviewing flashcards, creating an AI card, completing a quiz).
     * Automatically updates the daily record in Room, updates current streak, and adds XP.
     */
    suspend fun recordPracticeActivity(
        itemsCount: Int = 1,
        minutesSpent: Int = 5,
        xpEarned: Int = 15,
        activityType: String = "VOCABULARY"
    ): StreakUpdateResult {
        val today = getTodayDateString()
        val existingToday = streakDao.getRecordForDateSync(today)

        val updatedRecord = if (existingToday != null) {
            existingToday.copy(
                itemsPracticed = existingToday.itemsPracticed + itemsCount,
                minutesSpent = existingToday.minutesSpent + minutesSpent,
                xpEarned = existingToday.xpEarned + xpEarned,
                timestamp = System.currentTimeMillis(),
                isGoalMet = true
            )
        } else {
            DailyStreakRecord(
                date = today,
                timestamp = System.currentTimeMillis(),
                itemsPracticed = itemsCount,
                minutesSpent = minutesSpent,
                xpEarned = xpEarned,
                activityType = activityType,
                isGoalMet = true
            )
        }

        streakDao.insertOrUpdate(updatedRecord)

        // Re-calculate streak
        val allRecords = streakDao.getAllRecordsSync()
        val profile = userProfileDao.getProfileSync()
        val currentStreak = calculateCurrentStreak(allRecords)
        val previousStreak = profile?.streakDays ?: 0

        // Update profile streak and XP
        if (profile != null) {
            val newXp = profile.xp + xpEarned
            userProfileDao.insertOrUpdate(
                profile.copy(
                    streakDays = currentStreak,
                    xp = newXp
                )
            )
        }

        // Check if a new milestone is achieved
        val milestoneReached = getMilestoneForStreak(currentStreak, previousStreak)
        val message = when {
            milestoneReached != null -> "Congratulations! You unlocked the \"${milestoneReached.title}\" badge!"
            currentStreak > 1 -> "Excellent! Your learning streak has reached $currentStreak days in a row."
            else -> "Nice! Today's practice session has been logged."
        }

        return StreakUpdateResult(
            previousStreak = previousStreak,
            newStreak = currentStreak,
            xpEarned = xpEarned,
            isNewMilestoneReached = milestoneReached != null,
            milestoneTitle = milestoneReached?.title,
            message = message
        )
    }

    /**
     * Compute current consecutive streak in days.
     */
    fun calculateCurrentStreak(records: List<DailyStreakRecord>): Int {
        if (records.isEmpty()) return 0
        val recordDates = records.map { it.date }.toSet()

        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()

        val cal = Calendar.getInstance()
        var streak = 0

        // If today is practiced, start from today.
        // If today is not yet practiced, but yesterday was, streak is intact from yesterday.
        if (recordDates.contains(today)) {
            // Count backwards from today
            while (true) {
                val checkDate = dateFormat.format(cal.time)
                if (recordDates.contains(checkDate)) {
                    streak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        } else if (recordDates.contains(yesterday)) {
            // Today not yet practiced, but yesterday was
            cal.add(Calendar.DAY_OF_YEAR, -1)
            while (true) {
                val checkDate = dateFormat.format(cal.time)
                if (recordDates.contains(checkDate)) {
                    streak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        return streak
    }

    /**
     * Compute the longest streak in recorded history.
     */
    fun calculateLongestStreak(records: List<DailyStreakRecord>): Int {
        if (records.isEmpty()) return 0
        val sortedDates = records.map { it.date }.distinct().sorted()
        if (sortedDates.isEmpty()) return 0

        var maxStreak = 1
        var currentStreak = 1

        val cal = Calendar.getInstance()
        for (i in 1 until sortedDates.size) {
            try {
                val prevDate = dateFormat.parse(sortedDates[i - 1])
                val currDate = dateFormat.parse(sortedDates[i])
                if (prevDate != null && currDate != null) {
                    cal.time = prevDate
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    if (dateFormat.format(cal.time) == sortedDates[i]) {
                        currentStreak++
                        maxStreak = max(maxStreak, currentStreak)
                    } else {
                        currentStreak = 1
                    }
                }
            } catch (e: Exception) {
                currentStreak = 1
            }
        }
        return maxStreak
    }

    private fun calculateStreakInfo(records: List<DailyStreakRecord>, userXp: Int): StreakInfo {
        val today = getTodayDateString()
        val isTodayCompleted = records.any { it.date == today }
        val currentStreak = calculateCurrentStreak(records)
        val longestStreak = max(currentStreak, calculateLongestStreak(records))
        val totalDays = records.size

        // Build 7-day weekly schedule for Persian speakers (Saturday to Friday)
        val weeklyDays = buildWeeklyDays(records)

        // Milestones
        val allMilestones = listOf(
            StreakMilestone(3, "Learning Spark", "3 days of consistent practice", "spark", currentStreak >= 3),
            StreakMilestone(7, "Week Warrior", "7 days without a break", "shield", currentStreak >= 7),
            StreakMilestone(14, "Steady Habit", "14 days of mastery and consistency", "fire", currentStreak >= 14),
            StreakMilestone(30, "Vocabulary Master", "30 golden days of steady learning", "crown", currentStreak >= 30),
            StreakMilestone(60, "Bilingual Mind", "60 days of full commitment", "diamond", currentStreak >= 60),
            StreakMilestone(100, "Language Legend", "100 days of professional IELTS/TOEFL practice", "trophy", currentStreak >= 100)
        )

        val nextMilestone = allMilestones.firstOrNull { !it.isUnlocked }
        val unlockedMilestones = allMilestones.filter { it.isUnlocked }

        val level = when {
            currentStreak >= 100 -> "Language Legend"
            currentStreak >= 30 -> "Vocabulary Master"
            currentStreak >= 14 -> "Diligent Learner"
            currentStreak >= 7 -> "Week Warrior"
            currentStreak >= 3 -> "Active Learner"
            else -> "Determined Starter"
        }

        val motivationalMessage = when {
            isTodayCompleted && currentStreak >= 7 -> "Outstanding! Today's streak is locked in and your $currentStreak-day record is at its peak."
            isTodayCompleted -> "Well done! Today's streak is logged. See you tomorrow!"
            currentStreak > 0 -> "Your $currentStreak-day streak is alive! One short session keeps it going."
            else -> "Today is the best day to start a new streak. Begin with one card or a quick review."
        }

        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            isTodayCompleted = isTodayCompleted,
            totalDaysPracticed = totalDays,
            totalXp = userXp,
            weeklyDays = weeklyDays,
            motivationalMessage = motivationalMessage,
            streakLevel = level,
            nextMilestone = nextMilestone,
            unlockedMilestones = unlockedMilestones
        )
    }

    private fun buildWeeklyDays(records: List<DailyStreakRecord>): List<StreakDayItem> {
        val recordDates = records.associateBy { it.date }
        val today = getTodayDateString()

        // Generate past 7 days (including today)
        val items = mutableListOf<StreakDayItem>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6) // 6 days ago up to today

        val dayNames = mapOf(
            Calendar.SATURDAY to "S",
            Calendar.SUNDAY to "S",
            Calendar.MONDAY to "M",
            Calendar.TUESDAY to "T",
            Calendar.WEDNESDAY to "W",
            Calendar.THURSDAY to "T",
            Calendar.FRIDAY to "F"
        )

        for (i in 0..6) {
            val dateStr = dateFormat.format(cal.time)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val dayName = dayNames[dayOfWeek] ?: "?"
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString()

            val record = recordDates[dateStr]
            items.add(
                StreakDayItem(
                    date = dateStr,
                    dayName = dayName,
                    dayNumber = dayOfMonth,
                    isCompleted = record != null,
                    isToday = dateStr == today,
                    itemsCount = record?.itemsPracticed ?: 0
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return items
    }

    private fun getMilestoneForStreak(currentStreak: Int, previousStreak: Int): StreakMilestone? {
        val milestones = listOf(
            StreakMilestone(3, "جرقه یادگیری", "۳ روز تمرین مستمر زبان", "spark", true),
            StreakMilestone(7, "قهرمان یک‌هفته‌ای", "۷ روز تمرین بدون توقف", "shield", true),
            StreakMilestone(14, "عادت پایدار", "۱۴ روز تسلط و پیوستگی", "fire", true),
            StreakMilestone(30, "استاد واژگان", "۳۰ روز طلایی یادگیری مستمر", "crown", true),
            StreakMilestone(60, "ذهن دوزبانه", "۶۰ روز تعهد کامل به زبان", "diamond", true),
            StreakMilestone(100, "اسطوره زبان‌آموزی", "۱۰۰ روز تمرین حرفه‌ای آیلتس/تافل", "trophy", true)
        )
        return milestones.firstOrNull { currentStreak >= it.requiredDays && previousStreak < it.requiredDays }
    }
}
