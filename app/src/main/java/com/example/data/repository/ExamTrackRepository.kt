package com.example.data.repository

import com.example.data.local.DailyStreakDao
import com.example.data.local.ExamTrackDao
import com.example.data.local.UserProfileDao
import com.example.data.local.VocabularyDao
import com.example.data.model.ExamTrackSettingsRecord
import com.example.data.model.ExamTrackStage
import com.example.data.model.ExamTrackState
import com.example.data.model.ExamTrackType
import com.example.data.model.ExamWordItem
import com.example.data.model.ExamWordProgressRecord
import com.example.data.model.StreakUpdateResult
import com.example.data.model.VocabularyItem
import com.example.data.seed.ExamTrackDataSeed
import com.example.data.seed.InitialDataSeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExamTrackRepository(
    private val examTrackDao: ExamTrackDao,
    private val userProfileDao: UserProfileDao,
    private val streakDao: DailyStreakDao,
    private val vocabularyDao: VocabularyDao
) {
    private val streakRepository = DailyStreakRepository(streakDao, userProfileDao)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun getTodayDateString(): String = dateFormat.format(Date())

    /**
     * Reactive exam-track state backed by the real many-to-many master vocabulary pack.
     * The original hand-curated seed cards remain as rich high-yield cards at the beginning
     * of the track; downloaded Room vocabulary fills the current catalog target for each exam.
     * A successful study of a canonical vocabulary row is shared across every exam pack that
     * contains that same word, while pack membership and stage organization remain independent.
     */
    fun getTrackState(trackType: ExamTrackType): Flow<ExamTrackState> {
        val seedWords = ExamTrackDataSeed.getWordsForTrack(trackType.id)
        val packId = masterPackId(trackType)

        return combine(
            vocabularyDao.getByPack(packId),
            examTrackDao.getProgressForTrack(trackType.id),
            examTrackDao.getSettingsForTrack(trackType.id)
        ) { downloadedVocabulary, progressList, settings ->
            val allWords = buildTrackWords(trackType, seedWords, downloadedVocabulary)
            val progressMap = progressList.associateBy { it.wordId }
            val vocabularyByNormalizedWord = downloadedVocabulary.associateBy { normalize(it.word) }
            val today = getTodayDateString()

            val dailyGoal = settings?.dailyGoalWords ?: trackType.defaultDailyGoal
            val currentStageNum = settings?.currentStageNumber ?: 1
            val wordsToday = if (settings?.lastStudyDate == today) settings.wordsStudiedToday else 0

            val decoratedWords = allWords.map { word ->
                val progress = progressMap[word.id]
                val canonical = vocabularyByNormalizedWord[normalize(word.word)]
                val learnedAnywhere = (canonical?.correctCount ?: 0) > 0
                word.copy(isMastered = progress?.isMastered == true || learnedAnywhere)
            }

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
        }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    }

    private val cachedTrackWords = java.util.concurrent.ConcurrentHashMap<ExamTrackType, List<ExamWordItem>>()
    private val cachedDownloadedSize = java.util.concurrent.ConcurrentHashMap<ExamTrackType, Int>()

    private fun masterTarget(trackType: ExamTrackType): Int = when (trackType) {
        ExamTrackType.IELTS -> 5040
        ExamTrackType.TOEFL -> 6974
        ExamTrackType.GRE -> 7504
    }

    private fun buildTrackWords(
        trackType: ExamTrackType,
        seedWords: List<ExamWordItem>,
        downloadedVocabulary: List<VocabularyItem>
    ): List<ExamWordItem> {
        val cached = cachedTrackWords[trackType]
        if (cached != null && cachedDownloadedSize[trackType] == downloadedVocabulary.size) {
            return cached
        }

        val target = masterTarget(trackType)
        val seedNormalized = seedWords.map { normalize(it.word) }.toHashSet()

        val rankedDownloaded = downloadedVocabulary
            .asSequence()
            .filter { it.word.isNotBlank() && it.persianMeaning.isNotBlank() }
            .filterNot { normalize(it.word) in seedNormalized }
            .distinctBy { normalize(it.word) }
            .sortedWith(
                compareBy<VocabularyItem> { if (it.learningOrder > 0) it.learningOrder else Int.MAX_VALUE }
                    .thenByDescending { it.examPriority }
                    .thenBy { if (it.frequencyRank > 0) it.frequencyRank else Int.MAX_VALUE }
                    .thenBy { it.word.lowercase(Locale.US) }
            )
            .toList()

        val dynamicSlots = (target - seedWords.size).coerceAtLeast(0)
        val selectedDynamic = rankedDownloaded.take(dynamicSlots)
        val dynamicCount = selectedDynamic.size.coerceAtLeast(1)

        val dynamicExamWords = selectedDynamic.mapIndexed { index, item ->
            val stage = (1 + (index * 4 / dynamicCount)).coerceIn(1, 4)
            item.toExamWordItem(trackType, stage)
        }

        val result = (seedWords + dynamicExamWords).take(target)
        cachedTrackWords[trackType] = result
        cachedDownloadedSize[trackType] = downloadedVocabulary.size
        return result
    }

    private fun VocabularyItem.toExamWordItem(
        trackType: ExamTrackType,
        stageNumber: Int
    ): ExamWordItem {
        val priorityText = if (examPriority > 0) {
            "اولویت این واژه در بانک ${trackType.id}: $examPriority از ۱۰۰."
        } else {
            "این واژه از بانک جامع ${trackType.id} انتخاب شده است."
        }

        return ExamWordItem(
            id = "master_${trackType.id.lowercase(Locale.US)}_$id",
            word = word,
            phonetic = ipa,
            partOfSpeech = partOfSpeech,
            persianMeaning = persianMeaning,
            englishDefinition = englishDefinition,
            exampleEn = example,
            exampleFa = examplePersian,
            collocations = collocations,
            synonyms = synonyms,
            examTipFa = priorityText,
            iranianMistakeFa = commonMistakes,
            stageNumber = stageNumber,
            examTrack = trackType.id
        )
    }

    private fun masterPackId(trackType: ExamTrackType): String = when (trackType) {
        ExamTrackType.IELTS -> InitialDataSeed.IELTS_MASTER_PACK_ID
        ExamTrackType.TOEFL -> InitialDataSeed.TOEFL_MASTER_PACK_ID
        ExamTrackType.GRE -> InitialDataSeed.GRE_MASTER_PACK_ID
    }

    private fun normalize(word: String): String = word.trim().lowercase(Locale.US)

    private fun buildStages(
        trackType: ExamTrackType,
        words: List<ExamWordItem>,
        currentStageNumber: Int
    ): List<ExamTrackStage> {
        val stageMeta = when (trackType) {
            ExamTrackType.IELTS -> listOf(
                StageMeta(1, "Foundation Academic Core", "مرحله ۱: پایه آکادمیک و ضروریات", "واژگان با بالاترین بسامد تکرار در مقالات دانشگاهی و نمودارها", "Band 6.0 - 6.5"),
                StageMeta(2, "Advanced Lexical Resource", "مرحله ۲: واژگان پیشرفته و هم‌آیندها", "کلمات امتیازآور برای راه‌حل‌ها، استدلال و تحلیل عمیق", "Band 7.0 - 7.5"),
                StageMeta(3, "Academic Mastery", "مرحله ۳: تسلط علمی و کلمات نمره بالا", "واژگان دقیق برای متون دشوار ریدینگ و رایتینگ", "Band 8.0 - 8.5"),
                StageMeta(4, "Idiomatic & Collocation Power", "مرحله ۴: اصطلاحات و تسلط کامل", "کالوکیشن‌های طبیعی و واژگان سطح بالا", "Band 9.0")
            )
            ExamTrackType.TOEFL -> listOf(
                StageMeta(1, "Campus & Academic Foundation", "مرحله ۱: مکالمات و فضای دانشگاهی", "لغات پرکاربرد سرفصل‌های درسی، پروژه‌ها و ارتباط با اساتید", "TOEFL 80+"),
                StageMeta(2, "Scientific & Empirical Lectures", "مرحله ۲: سخنرانی‌های علمی و آزمایشگاهی", "واژگان متون زیست‌شناسی، زمین‌شناسی و علوم", "TOEFL 95+"),
                StageMeta(3, "Advanced Academic Discourse", "مرحله ۳: تحلیل انتقادی و مباحثه دانشگاهی", "کلمات کلیدی برای Academic Discussion و ریدینگ‌های سنگین", "TOEFL 105+"),
                StageMeta(4, "High-Yield Distinction", "مرحله ۴: لغات متمایزکننده نمره بالا", "واژگان سطح بالا برای عملکرد ممتاز", "TOEFL 115+")
            )
            ExamTrackType.GRE -> listOf(
                StageMeta(1, "Verbal High-Frequency Core", "مرحله ۱: هسته پرتکرار جی‌آر‌ای", "کلمات اساسی بخش وربال و تکمیل جملات", "GRE 150-155"),
                StageMeta(2, "Sentence Equivalence & Contrasts", "مرحله ۲: مترادف‌ها و تضادهای ظریف", "واژگان کلیدی برای Sentence Equivalence و تحلیل متن", "GRE 155-160"),
                StageMeta(3, "Advanced Verbal Vocabulary", "مرحله ۳: لغات سخت و استدلال عمیق", "کلمات سطح بالا برای تحلیل‌های انتقادی", "GRE 160-165"),
                StageMeta(4, "Ultra-Advanced Verbal Mastery", "مرحله ۴: تسلط پیشرفته وربال", "واژگان دشوار برای بالاترین بازه‌های وربال", "GRE 165+")
            )
        }

        var previousStageCompleted = true

        val stageMap = words.groupBy { it.stageNumber }

        return stageMeta.map { meta ->
            val stageWords = stageMap[meta.stageNumber] ?: emptyList()
            val masteredCount = stageWords.count { it.isMastered }
            val totalCount = stageWords.size
            // Learners can freely start at any band / stage based on their current proficiency
            val isUnlocked = true

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

    suspend fun updateWordMastery(
        trackType: ExamTrackType,
        wordId: String,
        stageNumber: Int,
        isMastered: Boolean
    ): StreakUpdateResult {
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

        val today = getTodayDateString()
        val currentSettings = examTrackDao.getSettingsForTrackSync(trackType.id)
        val wordsToday = if (currentSettings?.lastStudyDate == today) {
            currentSettings.wordsStudiedToday + 1
        } else {
            1
        }
        val totalLearnedCount = examTrackDao.getMasteredCountForTrackSync(trackType.id)

        // Synchronize with core Spaced Repetition System (VocabularyItem) so words studied here enter the SRS queue
        try {
            val vocabItem = if (wordId.startsWith("master_")) {
                val dbId = wordId.substringAfterLast("_").toIntOrNull()
                if (dbId != null) vocabularyDao.getByIdSync(dbId) else null
            } else {
                val wordName = ExamTrackDataSeed.getWordsForTrack(trackType.id).firstOrNull { it.id == wordId }?.word
                if (wordName != null) vocabularyDao.getByExactWord(wordName) else null
            }

            if (vocabItem != null) {
                val rating = if (isMastered) com.example.srs.ReviewRating.GOOD else com.example.srs.ReviewRating.AGAIN
                val srsResult = com.example.srs.SpacedRepetitionSystem.calculateNextReview(vocabItem, rating)
                val updatedItem = vocabItem.copy(
                    nextReview = srsResult.nextReviewTimestamp,
                    intervalDays = srsResult.intervalDays,
                    difficulty = srsResult.newDifficulty,
                    stability = srsResult.newStability,
                    mastery = srsResult.newMastery,
                    correctCount = srsResult.correctCount,
                    incorrectCount = srsResult.incorrectCount,
                    lastReview = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                vocabularyDao.update(updatedItem)
            }
        } catch (_: Exception) {
            // Best effort sync to prevent breaking track progress
        }

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
