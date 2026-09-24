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
            milestoneReached != null -> "تبریک! مدال «${milestoneReached.titleFa}» را به دست آوردید! 🔥"
            currentStreak > 1 -> "فوق‌العاده است! زنجیره یادگیری شما به $currentStreak روز متوالی رسید."
            else -> "آفرین! اولین گام از زنجیره تمرین امروز با موفقیت ثبت شد."
        }

        return StreakUpdateResult(
            previousStreak = previousStreak,
            newStreak = currentStreak,
            xpEarned = xpEarned,
            isNewMilestoneReached = milestoneReached != null,
            milestoneTitleFa = milestoneReached?.titleFa,
            messageFa = message
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
            StreakMilestone(3, "جرقه یادگیری", "۳ روز تمرین مستمر زبان", "spark", currentStreak >= 3),
            StreakMilestone(7, "قهرمان یک‌هفته‌ای", "۷ روز تمرین بدون توقف", "shield", currentStreak >= 7),
            StreakMilestone(14, "عادت پایدار", "۱۴ روز تسلط و پیوستگی", "fire", currentStreak >= 14),
            StreakMilestone(30, "استاد واژگان", "۳۰ روز طلایی یادگیری مستمر", "crown", currentStreak >= 30),
            StreakMilestone(60, "ذهن دوزبانه", "۶۰ روز تعهد کامل به زبان", "diamond", currentStreak >= 60),
            StreakMilestone(100, "اسطوره زبان‌آموزی", "۱۰۰ روز تمرین حرفه‌ای آیلتس/تافل", "trophy", currentStreak >= 100)
        )

        val nextMilestone = allMilestones.firstOrNull { !it.isUnlocked }
        val unlockedMilestones = allMilestones.filter { it.isUnlocked }

        val levelFa = when {
            currentStreak >= 100 -> "اسطوره زبان‌آموزی 👑"
            currentStreak >= 30 -> "استاد واژگان 🏆"
            currentStreak >= 14 -> "زبان‌آموز کوشا 🥇"
            currentStreak >= 7 -> "قهرمان هفته 🥈"
            currentStreak >= 3 -> "یادگیرنده فعال 🥉"
            else -> "آغازگر مصمم 🌟"
        }

        val motivationalMessageFa = when {
            isTodayCompleted && currentStreak >= 7 -> "فوق‌العاده است! زنجیره امروز کامل شد و رکورد $currentStreak روزه شما در اوج است!"
            isTodayCompleted -> "آفرین! زنجیره امروز شما با موفقیت ثبت شد. فردا منتظرتان هستیم!"
            currentStreak > 0 -> "زنجیره $currentStreak روزه شما فعال است! برای حفظ آن، فقط یک تمرین کوتاه انجام دهید."
            else -> "امروز بهترین روز برای آغاز زنجیره یادگیری جدید است! با یک کارت یا مرور شروع کنید."
        }

        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            isTodayCompleted = isTodayCompleted,
            totalDaysPracticed = totalDays,
            totalXp = userXp,
            weeklyDays = weeklyDays,
            motivationalMessageFa = motivationalMessageFa,
            streakLevelFa = levelFa,
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

        val dayNamesFa = mapOf(
            Calendar.SATURDAY to "ش",
            Calendar.SUNDAY to "ی",
            Calendar.MONDAY to "د",
            Calendar.TUESDAY to "س",
            Calendar.WEDNESDAY to "چ",
            Calendar.THURSDAY to "پ",
            Calendar.FRIDAY to "ج"
        )

        for (i in 0..6) {
            val dateStr = dateFormat.format(cal.time)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val dayName = dayNamesFa[dayOfWeek] ?: "؟"
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString()

            val record = recordDates[dateStr]
            items.add(
                StreakDayItem(
                    date = dateStr,
                    dayNameFa = dayName,
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
