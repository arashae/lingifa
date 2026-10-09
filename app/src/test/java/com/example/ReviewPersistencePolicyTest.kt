package com.example

import com.example.data.model.VocabularyItem
import com.example.srs.ReviewRating
import com.example.vocab.ReviewPersistencePolicy
import com.example.vocab.VocabularySkillAxis
import com.example.vocab.VocabularyStudyPolicy
import org.junit.Assert.*
import org.junit.Test

class ReviewPersistencePolicyTest {
    private val now = 1_700_000_000_000L
    private fun studied() = VocabularyItem(
        id = 1, word = "retain", persianMeaning = "حفظ کردن", correctCount = 5,
        mastery = 75, stability = 12f, difficulty = 3f, intervalDays = 10,
        lastReview = now - 86_400_000L, nextReview = now + 9 * 86_400_000L
    )

    @Test fun `early success trains retrieval without inflating spaced mastery`() {
        val before = studied()
        val after = ReviewPersistencePolicy.apply(before, VocabularySkillAxis.RETRIEVAL, ReviewRating.EASY, now)
        assertEquals(before.nextReview, after.nextReview)
        assertEquals(before.lastReview, after.lastReview)
        assertEquals(before.stability, after.stability)
        assertEquals(before.difficulty, after.difficulty)
        assertEquals(before.intervalDays, after.intervalDays)
        assertEquals(before.correctCount, after.correctCount)
        assertEquals(before.mastery, after.mastery)
        assertTrue(VocabularyStudyPolicy.skillMastery(after, VocabularySkillAxis.RETRIEVAL) > 0)
        assertEquals(now, ReviewPersistencePolicy.lastPractice(after))
    }

    @Test fun `early lapse brings review forward without rewarding repetitions`() {
        val before = studied()
        val after = ReviewPersistencePolicy.apply(before, VocabularySkillAxis.RETRIEVAL, ReviewRating.AGAIN, now)
        assertEquals(now + ReviewPersistencePolicy.COOLDOWN_MS, after.nextReview)
        assertEquals(before.correctCount, after.correctCount)
        assertEquals(before.lastReview, after.lastReview)
        assertEquals(before.stability, after.stability)
    }

    @Test fun `due success advances the spaced schedule once`() {
        val before = studied().copy(nextReview = now)
        val after = ReviewPersistencePolicy.apply(before, VocabularySkillAxis.RETRIEVAL, ReviewRating.GOOD, now)
        assertTrue(after.nextReview > now)
        assertEquals(before.correctCount + 1, after.correctCount)
        assertEquals(now, after.lastReview)
    }

    @Test fun `early lapse cannot postpone an already nearer review`() {
        val before = studied().copy(nextReview = now + 10_000)
        val after = ReviewPersistencePolicy.apply(before, VocabularySkillAxis.RETRIEVAL, ReviewRating.AGAIN, now)
        assertEquals(before.nextReview, after.nextReview)
    }
}
