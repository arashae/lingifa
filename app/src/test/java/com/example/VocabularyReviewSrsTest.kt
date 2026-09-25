package com.example

import com.example.data.model.VocabularyItem
import com.example.srs.ReviewRating
import com.example.srs.SpacedRepetitionSystem
import org.junit.Assert.assertEquals
import org.junit.Test

class VocabularyReviewSrsTest {

    @Test
    fun `again schedules a studied word exactly 30 minutes later`() {
        val now = 1_700_000_000_000L
        val item = VocabularyItem(
            id = 42L,
            word = "retain",
            persianMeaning = "حفظ کردن",
            correctCount = 2,
            incorrectCount = 0,
            lastReview = now - 86_400_000L,
            intervalDays = 1,
            stability = 2.5f,
            difficulty = 5f,
            nextReview = now
        )

        val result = SpacedRepetitionSystem.calculateNextReview(
            item = item,
            rating = ReviewRating.AGAIN,
            now = now
        )

        assertEquals(0, result.intervalDays)
        assertEquals(now + 30L * 60L * 1_000L, result.nextReviewTimestamp)
        assertEquals(item.incorrectCount + 1, result.incorrectCount)
    }
}
