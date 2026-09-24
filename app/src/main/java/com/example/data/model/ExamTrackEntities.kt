package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ExamTrackType(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val descriptionFa: String,
    val colorHex: Long,
    val iconName: String,
    val defaultDailyGoal: Int = 10
) {
    IELTS(
        id = "IELTS",
        titleEn = "IELTS Academic & General",
        titleFa = "آزمون آیلتس (IELTS)",
        descriptionFa = "مسیر مرحله‌به‌مرحله واژگان آکادمیک، کالوکیشن‌های طلایی و کلمات نمره ۷.۵ تا ۹ آیلتس",
        colorHex = 0xFF2563EB, // Blue
        iconName = "school",
        defaultDailyGoal = 10
    ),
    TOEFL(
        id = "TOEFL",
        titleEn = "TOEFL iBT Academic",
        titleFa = "آزمون تافل (TOEFL iBT)",
        descriptionFa = "مسیر تخصصی لغات سخنرانی‌های دانشگاهی، متون علمی و ریدینگ‌های تخصصی تافل",
        colorHex = 0xFF0D9488, // Teal
        iconName = "menu_book",
        defaultDailyGoal = 10
    ),
    GRE(
        id = "GRE",
        titleEn = "GRE General Verbal Reasoning",
        titleFa = "آزمون جی‌آر‌ای (GRE)",
        descriptionFa = "مسیر جامع واژگان پیچیده، مترادف‌های ظریف، لغات ۳۳۳ و ۸۰۰ پرتکرار وربال جی‌آر‌ای",
        colorHex = 0xFF7C3AED, // Purple
        iconName = "psychology",
        defaultDailyGoal = 10
    )
}

/**
 * Persists individual word mastery status for each exam track.
 */
@Entity(tableName = "exam_word_progress", primaryKeys = ["examTrack", "wordId"])
data class ExamWordProgressRecord(
    val examTrack: String, // "IELTS", "TOEFL", "GRE"
    val wordId: String,
    val stageNumber: Int,
    val isMastered: Boolean = false,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long = 0L
)

/**
 * Tracks stage and daily goal configuration for each exam track.
 */
@Entity(tableName = "exam_track_settings")
data class ExamTrackSettingsRecord(
    @PrimaryKey
    val examTrack: String, // "IELTS", "TOEFL", "GRE"
    val dailyGoalWords: Int = 10,
    val currentStageNumber: Int = 1,
    val lastStudyDate: String = "", // "YYYY-MM-DD"
    val wordsStudiedToday: Int = 0,
    val totalWordsLearnedCount: Int = 0
)

/**
 * In-memory representation of an exam word.
 */
data class ExamWordItem(
    val id: String,
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val persianMeaning: String,
    val englishDefinition: String,
    val exampleEn: String,
    val exampleFa: String,
    val collocations: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val examTipFa: String = "",
    val iranianMistakeFa: String = "",
    val stageNumber: Int,
    val examTrack: String,
    val isMastered: Boolean = false
)

/**
 * In-memory representation of a stage in an exam track.
 */
data class ExamTrackStage(
    val stageNumber: Int,
    val examTrack: String,
    val titleEn: String,
    val titleFa: String,
    val subtitleFa: String,
    val targetScoreFa: String,
    val words: List<ExamWordItem>,
    val isUnlocked: Boolean = true,
    val isCurrent: Boolean = false,
    val masteredCount: Int = 0,
    val totalCount: Int = words.size
) {
    val progressPercentage: Int
        get() = if (totalCount > 0) (masteredCount * 100) / totalCount else 0

    val isCompleted: Boolean
        get() = totalCount > 0 && masteredCount >= totalCount
}

/**
 * Overall status of an exam track.
 */
data class ExamTrackState(
    val trackType: ExamTrackType,
    val dailyGoalWords: Int = 10,
    val wordsStudiedToday: Int = 0,
    val totalWordsLearned: Int = 0,
    val totalWordsInTrack: Int = 0,
    val overallPercentage: Int = 0,
    val currentStageNumber: Int = 1,
    val stages: List<ExamTrackStage> = emptyList()
) {
    val dailyProgressPercentage: Int
        get() = if (dailyGoalWords > 0) ((wordsStudiedToday * 100) / dailyGoalWords).coerceAtMost(100) else 0

    val isDailyGoalMet: Boolean
        get() = wordsStudiedToday >= dailyGoalWords
}
