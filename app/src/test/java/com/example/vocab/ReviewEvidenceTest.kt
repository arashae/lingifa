package com.example.vocab

import com.example.data.model.VocabularyReviewEvent
import com.example.data.model.VocabularySkillProgress
import org.junit.Assert.*
import org.junit.Test

class ReviewEvidenceTest {
    private val event = VocabularyReviewEvent(vocabularyId = 1, senseKey = "primary", senseId = null,
        word = "buy", axis = "RETRIEVAL", reviewedAt = 100, category = "DUE", rating = 3,
        hintUsed = false, exactTarget = true, answer = "buy", prompt = "خریدن", expectedAnswer = "buy",
        prediction = .9, elapsedDays = 2.0, desiredRetention = .9)

    @Test fun `evaluation excludes guided early initial alternative self-rated and short-term responses`() {
        val excluded = listOf(event.copy(hintUsed = true), event.copy(category = "EARLY"),
            event.copy(category = "INITIAL"), event.copy(category = "VERIFIED_ALTERNATIVE"),
            event.copy(category = "UNGRADED", rating = null), event.copy(axis = "MEANING"),
            event.copy(elapsedDays = 0.0), event.copy(source = "EXAM_TRACK"))
        assertEquals(0, ReviewEvidence.summarize(excluded).samples)
        val result = ReviewEvidence.summarize(excluded + event + event.copy(exactTarget = false, rating = 1))
        assertEquals(2, result.samples)
        assertEquals(.5, result.recallRate!!, 0.0)
        assertEquals(.41, result.brierScore!!, 1e-12)
    }

    @Test fun `workload compares retention targets without confusing estimates with recall measurements`() {
        val state = VocabularySkillProgress(1, axis = "RETRIEVAL", stability = 10.0, lastReview = 1)
        val result = ReviewEvidence.summarize(emptyList(), listOf(state), 12)
        assertNull(result.recallRate)
        assertEquals(12, result.reviewsLastSevenDays)
        assertTrue(result.dailyWorkloadByRetention[.95]!! > result.dailyWorkloadByRetention[.9]!!)
        assertTrue(result.dailyWorkloadByRetention[.9]!! > result.dailyWorkloadByRetention[.85]!!)
    }
}
