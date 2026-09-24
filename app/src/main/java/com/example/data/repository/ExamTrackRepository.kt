package com.example.data.repository

import com.example.data.local.DailyStreakDao
import com.example.data.local.ExamTrackDao
import com.example.data.local.UserProfileDao
import com.example.data.model.ExamTrackSettingsRecord
import com.example.data.model.ExamTrackStage
import com.example.data.model.ExamTrackState
import com.example.data.model.ExamTrackType
import com.example.data.model.ExamWordItem
import com.example.data.model.ExamWordProgressRecord
import com.example.data.model.StreakUpdateResult
import com.example.data.seed.ExamTrackDataSeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExamTrackRepository(
    private val examTrackDao: ExamTrackDao,
    private val userProfileDao: UserProfileDao,
    private val streakDao: DailyStreakDao
) {
    private val streakRepository = DailyStreakRepository(streakDao, userProfileDao)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun getTodayDateString(): String = dateFormat.format(Date())

    /**
     * Reactive stream of progress, stages, and daily stats for an exam track.
     */
    fun getTrackState(trackType: ExamTrackType): Flow<ExamTrackState> {
        val allWords = ExamTrackDataSeed.getWordsForTrack(trackType.id)

        return combine(
            examTrackDao.getProgressForTrack(trackType.id),
            examTrackDao.getSettingsForTrack(trackType.id)
        ) { progressList, settings ->
            val progressMap = progressList.associateBy { it.wordId }
            val today = getTodayDateString()

            val dailyGoal = settings?.dailyGoalWords ?: trackType.defaultDailyGoal
            val currentStageNum = settings?.currentStageNumber ?: 1

            val wordsToday = if (settings?.lastStudyDate == today) settings.wordsStudiedToday else 0

            // Decorate words with saved Room mastery status
            val decoratedWords = allWords.map { word ->
                val p = progressMap[word.id]
                word.copy(isMastered = p?.isMastered ?: false)
            }

            // Group into 4 discrete stages
            val stages = buildStages(trackType, decoratedWords, currentStageNum)

            val totalLearned = decoratedWords.count { it.isMastered }
            val totalInTrack = decoratedWords.size
            val overallPercentage = if (totalInTrack > 0) (totalLearned * 100) / totalInTrack else 0

            ExamTrackState(
                trackType = trackType,
                dailyGoalWords = dailyGoal,
                wordsStudiedToday = wordsToday,
                totalWordsLearned = totalLearned,
                totalWordsInTrack = totalInTrack,
                overallPercentage = overallPercentage,
                currentStageNumber = currentStageNum,
                stages = stages
            )
        }
    }

    private fun buildStages(
        trackType: ExamTrackType,
        words: List<ExamWordItem>,
        currentStageNumber: Int
    ): List<ExamTrackStage> {
        val stageMeta = when (trackType) {
            ExamTrackType.IELTS -> listOf(
                StageMeta(1, "Foundation Academic Core", "مرحله ۱: پایه آکادمیک و ضروریات", "واژگان با بالاترین بسامد تکرار در مقالات دانشگاهی و نمودارها", "Band 6.0 - 6.5"),
                StageMeta(2, "Advanced Lexical Resource", "مرحله ۲: واژگان پیشرفته و هم‌آیندها", "کلمات امتیازآور برای راه‌حل‌ها، استدلال و تحلیل عمیق", "Band 7.0 - 7.5"),
                StageMeta(3, "Academic Mastery", "مرحله ۳: تسلط علمی و کلمات نمره بالا", "واژگان فاخر و دقیق برای متون دشوار ریدینگ و رایتینگ", "Band 8.0 - 8.5"),
                StageMeta(4, "Idiomatic & Collocation Power", "مرحله ۴: اصطلاحات طلایی و تسلط کامل", "کالوکیشن‌های طبیعی، استعاره‌های آکادمیک و نمره ۹", "Band 9.0")
            )
            ExamTrackType.TOEFL -> listOf(
                StageMeta(1, "Campus & Academic Foundation", "مرحله ۱: مکالمات و فضای دانشگاهی", "لغات پرکاربرد سرفصل‌های درسی، پروژه‌ها و ارتباط با اساتید", "TOEFL 80+"),
                StageMeta(2, "Scientific & Empirical Lectures", "مرحله ۲: سخنرانی‌های علمی و آزمایشگاهی", "واژگان تخصصی متون زیست‌شناسی، زمین‌شناسی و فیزیک", "TOEFL 95+"),
                StageMeta(3, "Advanced Academic Discourse", "مرحله ۳: تحلیل انتقادی و مباحثه دانشگاهی", "کلمات کلیدی برای بخش جدید Academic Discussion و ریدینگ‌های سنگین", "TOEFL 105+"),
                StageMeta(4, "High-Yield Distinction", "مرحله ۴: لغات متمایزکننده نمره کامل", "واژگان تراز اول برای رسیدن به نمره ۱۱۵ و بالاتر", "TOEFL 115+")
            )
            ExamTrackType.GRE -> listOf(
                StageMeta(1, "Verbal High-Frequency 333", "مرحله ۱: واژگان پرتکرار ۳۳۳ جی‌آر‌ای", "کلمات اساسی و زیربنایی بخش وربال و تکمیل جملات", "GRE 150-155"),
                StageMeta(2, "Sentence Equivalence & Contrasts", "مرحله ۲: مترادف‌ها و تضادهای ظریف", "واژگان کلیدی برای سوالات دوگزینه‌ای و تحلیل متنی", "GRE 155-160"),
                StageMeta(3, "Baron's & Manhattan Hard 800", "مرحله ۳: لغات سخت و استدلال عمیق", "کلمات سطح بالا برای تحلیل‌های فلسفی و انتقادی", "GRE 160-165"),
                StageMeta(4, "Ultra-Advanced Verbal Mastery", "مرحله ۴: تسلط فوق‌العاده و نمره ممتاز", "واژگان کمیاب و ادبی برای کسب بالاترین رتبه وربال", "GRE 165+")
            )
        }

        var previousStageCompleted = true

        return stageMeta.map { meta ->
            val stageWords = words.filter { it.stageNumber == meta.stageNumber }
            val masteredCount = stageWords.count { it.isMastered }
            val totalCount = stageWords.size
            val isUnlocked = meta.stageNumber == 1 || previousStageCompleted || meta.stageNumber <= currentStageNumber

            // Track if this stage qualifies to unlock next (>= 60% completion)
            previousStageCompleted = totalCount > 0 && (masteredCount * 100 / totalCount) >= 60

            ExamTrackStage(
                stageNumber = meta.stageNumber,
                examTrack = trackType.id,
                titleEn = meta.titleEn,
                titleFa = meta.titleFa,
                subtitleFa = meta.subtitleFa,
                targetScoreFa = meta.targetScoreFa,
                words = stageWords,
                isUnlocked = isUnlocked,
                isCurrent = meta.stageNumber == currentStageNumber,
                masteredCount = masteredCount,
                totalCount = totalCount
            )
        }
    }

    private data class StageMeta(
        val stageNumber: Int,
        val titleEn: String,
        val titleFa: String,
        val subtitleFa: String,
        val targetScoreFa: String
    )

    /**
     * Mark a word as mastered or needs review in Room.
     * Updates daily progress count and records Daily Streak activity (+15 XP).
     */
    suspend fun updateWordMastery(
        trackType: ExamTrackType,
        wordId: String,
        stageNumber: Int,
        isMastered: Boolean
    ): StreakUpdateResult {
        // 1. Update word progress in Room
        val existing = examTrackDao.getWordProgress(trackType.id, wordId)
        val updated = existing?.copy(
            isMastered = isMastered,
            reviewCount = existing.reviewCount + 1,
            lastReviewedAt = System.currentTimeMillis()
        ) ?: ExamWordProgressRecord(
            examTrack = trackType.id,
            wordId = wordId,
            stageNumber = stageNumber,
            isMastered = isMastered,
            reviewCount = 1,
            lastReviewedAt = System.currentTimeMillis()
        )
        examTrackDao.insertOrUpdateWordProgress(updated)

        // 2. Update track settings & today's study count in Room
        val today = getTodayDateString()
        val currentSettings = examTrackDao.getSettingsForTrackSync(trackType.id)
        val wordsToday = if (currentSettings?.lastStudyDate == today) {
            currentSettings.wordsStudiedToday + 1
        } else {
            1
        }
        val totalLearnedCount = examTrackDao.getMasteredCountForTrackSync(trackType.id)

        val newSettings = currentSettings?.copy(
            lastStudyDate = today,
            wordsStudiedToday = wordsToday,
            totalWordsLearnedCount = totalLearnedCount
        ) ?: ExamTrackSettingsRecord(
            examTrack = trackType.id,
            dailyGoalWords = trackType.defaultDailyGoal,
            currentStageNumber = stageNumber,
            lastStudyDate = today,
            wordsStudiedToday = wordsToday,
            totalWordsLearnedCount = totalLearnedCount
        )
        examTrackDao.insertOrUpdateSettings(newSettings)

        // 3. Record daily streak practice and XP
        return streakRepository.recordPracticeActivity(
            itemsCount = 1,
            minutesSpent = 2,
            xpEarned = 15,
            activityType = "EXAM_TRACK_${trackType.id}"
        )
    }

    suspend fun updateDailyGoal(trackType: ExamTrackType, newGoal: Int) {
        val current = examTrackDao.getSettingsForTrackSync(trackType.id)
        val updated = current?.copy(dailyGoalWords = newGoal) ?: ExamTrackSettingsRecord(
            examTrack = trackType.id,
            dailyGoalWords = newGoal,
            currentStageNumber = 1
        )
        examTrackDao.insertOrUpdateSettings(updated)
    }

    suspend fun selectCurrentStage(trackType: ExamTrackType, stageNumber: Int) {
        val current = examTrackDao.getSettingsForTrackSync(trackType.id)
        val updated = current?.copy(currentStageNumber = stageNumber) ?: ExamTrackSettingsRecord(
            examTrack = trackType.id,
            dailyGoalWords = trackType.defaultDailyGoal,
            currentStageNumber = stageNumber
        )
        examTrackDao.insertOrUpdateSettings(updated)
    }

    suspend fun resetTrack(trackType: ExamTrackType) {
        examTrackDao.resetTrackProgress(trackType.id)
    }
}
