package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.srs.Fsrs6

@Entity(tableName = "vocabulary_skill_progress", primaryKeys = ["vocabularyId", "senseKey", "axis"],
    foreignKeys = [ForeignKey(entity = VocabularyItem::class, parentColumns = ["id"], childColumns = ["vocabularyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["vocabularyId"]), Index(value = ["nextReview"])])
data class VocabularySkillProgress(
    val vocabularyId: Long,
    val senseKey: String = "primary",
    val axis: String,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val firstReview: Long = 0,
    val lastReview: Long = 0,
    val lastPractice: Long = 0,
    val nextReview: Long = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val independentSuccesses: Int = 0,
    val schedulerVersion: String = Fsrs6.VERSION
)

/** Append-only evidence; keep snapshots even after a vocabulary item is deleted. */
@Entity(tableName = "vocabulary_review_events", indices = [Index(value = ["vocabularyId"]), Index(value = ["reviewedAt"])])
data class VocabularyReviewEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vocabularyId: Long,
    val senseKey: String,
    val senseId: Long?,
    val word: String,
    val axis: String,
    val reviewedAt: Long,
    val category: String,
    val rating: Int?,
    val hintUsed: Boolean,
    val exactTarget: Boolean,
    val answer: String,
    val prompt: String,
    val expectedAnswer: String,
    val prediction: Double?,
    val elapsedDays: Double,
    val desiredRetention: Double,
    val schedulerVersion: String = Fsrs6.VERSION,
    val source: String = "REVIEW"
)

@Entity(tableName = "vocabulary_review_settings")
data class VocabularyReviewSettings(@PrimaryKey val id: Int = 1, val desiredRetention: Double = 0.9)
