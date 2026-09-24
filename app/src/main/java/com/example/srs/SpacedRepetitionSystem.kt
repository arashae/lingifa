package com.example.srs

import com.example.data.model.VocabularyItem
import kotlin.math.max
import kotlin.math.min

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

object SpacedRepetitionSystem {

    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    fun calculateNextReview(
        item: VocabularyItem,
        rating: ReviewRating,
        now: Long = System.currentTimeMillis()
    ): SrsCalculationResult {
        var difficulty = item.difficulty
        var stability = item.stability
        var correct = item.correctCount
        var incorrect = item.incorrectCount
        var intervalDays = item.intervalDays

        when (rating) {
            ReviewRating.AGAIN -> {
                // Reset interval, increase difficulty, record mistake
                incorrect += 1
                difficulty = min(5.0f, difficulty + 0.3f)
                stability = max(0.5f, stability * 0.7f)
                intervalDays = 0 // review again today / in 10 mins or next day
            }
            ReviewRating.HARD -> {
                // Small interval progression, slightly increase difficulty
                correct += 1
                difficulty = min(5.0f, difficulty + 0.15f)
                intervalDays = if (intervalDays <= 1) 1 else (intervalDays * 1.2f).toInt()
                stability = max(1.0f, stability * 1.1f)
            }
            ReviewRating.GOOD -> {
                // Standard SM-2 interval expansion
                correct += 1
                difficulty = max(1.0f, difficulty - 0.05f)
                intervalDays = when (intervalDays) {
                    0 -> 1
                    1 -> 3
                    else -> (intervalDays * (stability + 0.5f)).toInt()
                }
                stability = min(10.0f, stability * 1.25f)
            }
            ReviewRating.EASY -> {
                // Accelerated interval expansion, reduce difficulty
                correct += 1
                difficulty = max(1.0f, difficulty - 0.2f)
                intervalDays = when (intervalDays) {
                    0 -> 2
                    1 -> 4
                    else -> (intervalDays * (stability + 1.0f)).toInt()
                }
                stability = min(15.0f, stability * 1.4f)
            }
        }

        // Calculate mastery percentage (0 to 100)
        val totalAttempts = correct + incorrect
        val successRatio = if (totalAttempts > 0) correct.toFloat() / totalAttempts else 0f
        val intervalFactor = min(1.0f, intervalDays / 30f)
        val rawMastery = ((successRatio * 60f) + (intervalFactor * 40f)).toInt()
        val mastery = min(100, max(0, rawMastery))

        val reviewDelayMs = if (intervalDays == 0) {
            // Due in 1 hour if rated AGAIN
            60 * 60 * 1000L
        } else {
            intervalDays * ONE_DAY_MS
        }

        val nextReview = now + reviewDelayMs

        return SrsCalculationResult(
            intervalDays = intervalDays,
            nextReviewTimestamp = nextReview,
            newDifficulty = difficulty,
            newStability = stability,
            newMastery = mastery,
            correctCount = correct,
            incorrectCount = incorrect
        )
    }
}
