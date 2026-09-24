package com.example.srs

import com.example.data.model.VocabularyItem
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

enum class ReviewRating(val value: Int, val labelFa: String) {
    AGAIN(1, "دوباره"),
    HARD(2, "سخت بود"),
    GOOD(3, "خوب بود"),
    EASY(4, "آسان بود")
}

data class SrsCalculationResult(
    val intervalDays: Int,
    val nextReviewTimestamp: Long,
    val newDifficulty: Float,
    val newStability: Float,
    val newMastery: Int,
    val correctCount: Int,
    val incorrectCount: Int
)

/**
 * Enhanced Spaced Repetition System based on modern DSR (Difficulty, Stability, Retrievability)
 * memory modeling (FSRS / SuperMemo DSR dynamics) combined with cognitive psychology principles:
 *
 * 1. Desirable Difficulty: Retrieving a memory when its retrievability is fading produces
 *    the strongest synaptic consolidation boost.
 * 2. Non-Amnesic Lapses: When a mature word is forgotten (AGAIN), the stability is reduced
 *    proportionally rather than wiped out, allowing rapid relearning.
 * 3. Difficulty Mean Reversion: Difficulty adjusts dynamically based on ratings while
 *    gently regressing toward the center to avoid "ease hell".
 * 4. Multi-Dimensional Mastery: Mastery requires high accuracy, repetition depth, and interval longevity.
 */
object SpacedRepetitionSystem {

    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    fun calculateNextReview(
        item: VocabularyItem,
        rating: ReviewRating,
        now: Long = System.currentTimeMillis()
    ): SrsCalculationResult {
        val difficulty = if (item.difficulty <= 0f) 5.0f else item.difficulty
        val stability = if (item.stability <= 0f) 1.0f else item.stability
        var correct = item.correctCount
        var incorrect = item.incorrectCount

        val isFirstReview = (correct == 0 && incorrect == 0)

        // Elapsed time in days since last review
        val elapsedDays = if (item.lastReview > 0L) {
            max(0.1f, (now - item.lastReview).toFloat() / ONE_DAY_MS)
        } else {
            stability
        }

        // Current retrievability R(t) = 0.9^(t / S)
        val retrievability = 0.9.pow((elapsedDays / max(0.1f, stability)).toDouble()).toFloat()

        val newStability: Float
        val newDifficulty: Float
        val intervalDays: Int

        if (isFirstReview) {
            // Initial stability calibrated for first-time retention
            when (rating) {
                ReviewRating.AGAIN -> {
                    newStability = 0.4f
                    newDifficulty = min(10.0f, difficulty + 1.0f)
                    intervalDays = 0
                    incorrect += 1
                }
                ReviewRating.HARD -> {
                    newStability = 1.0f
                    newDifficulty = min(10.0f, difficulty + 0.3f)
                    intervalDays = 1
                    correct += 1
                }
                ReviewRating.GOOD -> {
                    newStability = 2.5f
                    newDifficulty = max(1.0f, difficulty - 0.1f)
                    intervalDays = 2
                    correct += 1
                }
                ReviewRating.EASY -> {
                    newStability = 4.5f
                    newDifficulty = max(1.0f, difficulty - 0.5f)
                    intervalDays = 4
                    correct += 1
                }
            }
        } else {
            when (rating) {
                ReviewRating.AGAIN -> {
                    // Lapse: Partial memory retention instead of total amnesia
                    incorrect += 1
                    newDifficulty = min(10.0f, difficulty + 0.8f)
                    newStability = max(0.4f, stability * 0.25f)
                    intervalDays = 0 // Immediate review required today
                }
                ReviewRating.HARD -> {
                    correct += 1
                    val dDelta = 0.3f
                    val rawD = min(10.0f, max(1.0f, difficulty + dDelta))
                    newDifficulty = 0.9f * rawD + 0.1f * 5.0f // mean reversion

                    // Desirable difficulty consolidation
                    val desirableDiff = exp(max(0.0, 1.0 - retrievability)).toFloat()
                    val factor = 1.0f + 1.2f * (newDifficulty.pow(-0.3f)) * (stability.pow(-0.15f)) * desirableDiff * 0.75f
                    newStability = max(stability + 0.5f, stability * factor)
                    intervalDays = max(1, newStability.roundToInt())
                }
                ReviewRating.GOOD -> {
                    correct += 1
                    val dDelta = -0.1f
                    val rawD = min(10.0f, max(1.0f, difficulty + dDelta))
                    newDifficulty = 0.9f * rawD + 0.1f * 5.0f

                    val desirableDiff = exp(max(0.0, 1.0 - retrievability)).toFloat()
                    val factor = 1.0f + 1.8f * (newDifficulty.pow(-0.3f)) * (stability.pow(-0.15f)) * desirableDiff * 1.0f
                    newStability = max(stability + 1.0f, stability * factor)
                    intervalDays = max(1, newStability.roundToInt())
                }
                ReviewRating.EASY -> {
                    correct += 1
                    val dDelta = -0.5f
                    val rawD = min(10.0f, max(1.0f, difficulty + dDelta))
                    newDifficulty = 0.9f * rawD + 0.1f * 5.0f

                    val desirableDiff = exp(max(0.0, 1.0 - retrievability)).toFloat()
                    val factor = 1.0f + 2.4f * (newDifficulty.pow(-0.3f)) * (stability.pow(-0.15f)) * desirableDiff * 1.4f
                    newStability = max(stability + 2.0f, stability * factor)
                    intervalDays = max(2, newStability.roundToInt())
                }
            }
        }

        // Mastery calculation: multi-dimensional index (accuracy 25%, repetition depth 35%, interval stability 40%)
        val totalAttempts = correct + incorrect
        val successRatio = if (totalAttempts > 0) correct.toFloat() / totalAttempts else 0f
        val repFactor = min(1.0f, correct / 5.0f)
        val intervalFactor = min(1.0f, intervalDays / 30.0f)
        val rawMastery = ((successRatio * 25f) + (repFactor * 35f) + (intervalFactor * 40f)).roundToInt()
        val mastery = min(100, max(0, rawMastery))

        val reviewDelayMs = if (intervalDays == 0) {
            30 * 60 * 1000L // Due in 30 minutes for intra-day relearning
        } else {
            intervalDays * ONE_DAY_MS
        }

        return SrsCalculationResult(
            intervalDays = intervalDays,
            nextReviewTimestamp = now + reviewDelayMs,
            newDifficulty = newDifficulty,
            newStability = newStability,
            newMastery = mastery,
            correctCount = correct,
            incorrectCount = incorrect
        )
    }

    /**
     * Calculates mastery level from existing item metrics.
     */
    fun calculateMastery(item: VocabularyItem): Int {
        val totalAttempts = item.correctCount + item.incorrectCount
        val successRatio = if (totalAttempts > 0) item.correctCount.toFloat() / totalAttempts else 0f
        val repFactor = min(1.0f, item.correctCount / 5.0f)
        val intervalFactor = min(1.0f, item.intervalDays / 30.0f)
        val rawMastery = ((successRatio * 25f) + (repFactor * 35f) + (intervalFactor * 40f)).roundToInt()
        return min(100, max(0, rawMastery))
    }

    /**
     * Returns a human-friendly Persian prediction of when this item will be reviewed.
     */
    fun getIntervalLabel(item: VocabularyItem, rating: ReviewRating): String {
        val result = calculateNextReview(item, rating)
        return when {
            result.intervalDays == 0 -> "< ۳۰ دقیقه"
            result.intervalDays == 1 -> "۱ روز"
            result.intervalDays in 2..6 -> "${result.intervalDays} روز"
            result.intervalDays in 7..13 -> "۱ هفته"
            result.intervalDays in 14..27 -> "${result.intervalDays / 7} هفته"
            result.intervalDays in 28..59 -> "۱ ماه"
            result.intervalDays in 60..180 -> "${result.intervalDays / 30} ماه"
            else -> "۶ ماه+"
        }
    }
}
