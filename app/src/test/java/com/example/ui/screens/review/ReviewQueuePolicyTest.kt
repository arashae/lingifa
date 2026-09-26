package com.example.ui.screens.review

import com.example.data.model.VocabularyItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewQueuePolicyTest {

    @Test
    fun `due words come first and scarce due queue is filled from studied vocabulary`() {
        val now = 1_700_000_000_000L
        val due = listOf(
            studiedItem(id = 1L, now = now, nextReview = now - 10_000L, mastery = 20),
            studiedItem(id = 2L, now = now, nextReview = now - 5_000L, mastery = 30)
        )
        val studied = (1L..100L).map { id ->
            due.firstOrNull { it.id == id }
                ?: studiedItem(id = id, now = now, nextReview = now + 86_400_000L, mastery = (id % 100).toInt())
        }

        val queue = ReviewQueuePolicy.buildQueue(
            dueItems = due,
            studiedItems = studied,
            now = now,
            sessionLimit = 50
        )

        assertEquals(50, queue.size)
        assertEquals(listOf(1L, 2L), queue.take(2).map { it.id })
        assertEquals(queue.size, queue.map { it.id }.distinct().size)
        assertTrue(queue.all { it.correctCount > 0 || it.incorrectCount > 0 })
    }

    @Test
    fun `reinforcement review never introduces unseen vocabulary`() {
        val now = 1_700_000_000_000L
        val studied = (1L..20L).map { id -> studiedItem(id, now, now + 86_400_000L, mastery = 50) }
        val unseen = VocabularyItem(
            id = 999L,
            word = "unseen",
            persianMeaning = "دیده نشده",
            correctCount = 0,
            incorrectCount = 0,
            nextReview = 0L
        )

        val queue = ReviewQueuePolicy.buildQueue(
            dueItems = emptyList(),
            studiedItems = studied + unseen,
            now = now,
            sessionLimit = 10
        )

        assertEquals(10, queue.size)
        assertFalse(queue.any { it.id == unseen.id })
    }

    @Test
    fun `optional reinforcement gives the majority of early slots to hard words`() {
        val now = 1_700_000_000_000L
        val hardWords = (1L..12L).map { id ->
            studiedItem(
                id = id,
                now = now,
                nextReview = now + 86_400_000L,
                mastery = 10,
                correctCount = 2,
                incorrectCount = 4,
                difficulty = 8.5f,
                stability = 0.5f
            )
        }
        val easyWords = (13L..20L).map { id ->
            studiedItem(
                id = id,
                now = now,
                nextReview = now + 7L * 86_400_000L,
                mastery = 90,
                correctCount = 8,
                incorrectCount = 0,
                difficulty = 2f,
                stability = 12f
            )
        }

        val queue = ReviewQueuePolicy.buildQueue(
            dueItems = emptyList(),
            studiedItems = hardWords + easyWords,
            now = now,
            sessionLimit = 20
        )

        assertEquals(20, queue.size)
        assertTrue(queue.take(12).all { it.id in 1L..12L })
        assertEquals(12, queue.take(12).count { it.id in 1L..12L })
    }

    private fun studiedItem(
        id: Long,
        now: Long,
        nextReview: Long,
        mastery: Int,
        correctCount: Int = 1,
        incorrectCount: Int = if (id % 5L == 0L) 1 else 0,
        difficulty: Float = 2.5f,
        stability: Float = 1.0f
    ): VocabularyItem = VocabularyItem(
        id = id,
        word = "word$id",
        persianMeaning = "معنی $id",
        correctCount = correctCount,
        incorrectCount = incorrectCount,
        mastery = mastery,
        difficulty = difficulty,
        stability = stability,
        lastReview = now - 86_400_000L,
        nextReview = nextReview
    )
}
