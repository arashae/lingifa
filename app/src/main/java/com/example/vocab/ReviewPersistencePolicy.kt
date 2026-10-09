package com.example.vocab

import com.example.data.model.VocabularyItem
import com.example.srs.ReviewRating
import com.example.srs.SpacedRepetitionSystem

/** Early practice trains a skill; only a due review advances the spaced schedule. */
object ReviewPersistencePolicy {
    const val PRACTICE_TIME_PREFIX = "linguafa:last-practice:"
    const val COOLDOWN_MS = 30L * 60L * 1_000L

    fun lastPractice(item: VocabularyItem): Long = maxOf(
        item.lastReview,
        item.tags.firstOrNull { it.startsWith(PRACTICE_TIME_PREFIX) }
            ?.removePrefix(PRACTICE_TIME_PREFIX)?.toLongOrNull() ?: 0L
    )

    fun apply(item: VocabularyItem, axis: VocabularySkillAxis, rating: ReviewRating, now: Long): VocabularyItem {
        val practiced = VocabularyStudyPolicy.withSkillResult(item, axis, rating != ReviewRating.AGAIN)
        val tagged = practiced.copy(
            tags = practiced.tags.filterNot { it.startsWith(PRACTICE_TIME_PREFIX) } + "$PRACTICE_TIME_PREFIX$now",
            updatedAt = now
        )
        if (item.nextReview > now && item.correctCount + item.incorrectCount > 0) {
            return tagged.copy(nextReview = if (rating == ReviewRating.AGAIN) {
                minOf(item.nextReview, now + COOLDOWN_MS)
            } else item.nextReview)
        }
        val result = SpacedRepetitionSystem.calculateNextReview(item, rating, now)
        return tagged.copy(
            intervalDays = result.intervalDays, nextReview = result.nextReviewTimestamp,
            difficulty = result.newDifficulty, stability = result.newStability,
            mastery = result.newMastery, correctCount = result.correctCount,
            incorrectCount = result.incorrectCount, lastReview = now
        )
    }
}
