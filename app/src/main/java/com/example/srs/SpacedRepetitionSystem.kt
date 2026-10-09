package com.example.srs

import com.example.data.model.VocabularyItem
import kotlin.math.max
import kotlin.math.min
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

/** FSRS-6 memory scheduling with a separate progress score and 30-minute relearning step. */
object SpacedRepetitionSystem {
    fun calculateNextReview(item: VocabularyItem, rating: ReviewRating,
        now: Long = System.currentTimeMillis(), desiredRetention: Double = 0.9): SrsCalculationResult {
        // Legacy states used a different forgetting curve. Keep their due dates during migration;
        // initialise FSRS from the next real grade rather than calling old stability calibrated.
        val previous = if (item.schedulerVersion == Fsrs6.VERSION && item.lastReview > 0 &&
            item.stability.isFinite() && item.stability > 0) Fsrs6.Memory(item.stability.toDouble(), item.difficulty.toDouble()) else null
        val elapsed = ((now - item.lastReview).coerceAtLeast(0) / Fsrs6.DAY_MS).toDouble()
        val memory = Fsrs6.update(previous, elapsed, rating)
        val days = if (rating == ReviewRating.AGAIN) 0 else Fsrs6.interval(memory.stability, desiredRetention)
        val correct = item.correctCount + if (rating == ReviewRating.AGAIN) 0 else 1
        val incorrect = item.incorrectCount + if (rating == ReviewRating.AGAIN) 1 else 0
        val score = calculateMastery(item.copy(correctCount = correct, incorrectCount = incorrect, intervalDays = days))
        return SrsCalculationResult(days, now + if (days == 0) 30 * 60_000L else days * Fsrs6.DAY_MS,
            memory.difficulty.toFloat(), memory.stability.toFloat(), score, correct, incorrect)
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
    fun getIntervalLabel(item: VocabularyItem, rating: ReviewRating, desiredRetention: Double = 0.9): String {
        val result = calculateNextReview(item, rating, desiredRetention = desiredRetention)
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
