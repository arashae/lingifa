package com.example.srs

import com.example.data.model.VocabularyItem
import org.junit.Assert.*
import org.junit.Test

class Fsrs6Test {
    @Test fun `matches 28 golden vectors executed by the pinned upstream scheduler`() {
        val stream = javaClass.getResourceAsStream("/fsrs6-reference.csv")!!
        val rows = stream.bufferedReader().use { it.readLines().drop(1) }
        assertEquals(28, rows.size)
        rows.forEach { row ->
            val v = row.split(',')
            val previous = if (v[0].toDouble() == 0.0) null else Fsrs6.Memory(v[0].toDouble(), v[1].toDouble())
            val rating = ReviewRating.values().single { it.value == v[3].toInt() }
            val memory = Fsrs6.update(previous, v[2].toDouble(), rating)
            assertEquals("stability: $row", v[4].toDouble(), memory.stability, 1e-9)
            assertEquals("difficulty: $row", v[5].toDouble(), memory.difficulty, 1e-9)
            assertEquals("interval: $row", v[6].toInt(), Fsrs6.interval(memory.stability))
        }
    }

    @Test fun `higher retention gives shorter bounded intervals`() {
        assertTrue(Fsrs6.interval(10.0, .95) < Fsrs6.interval(10.0, .9))
        assertTrue(Fsrs6.interval(10.0, .9) < Fsrs6.interval(10.0, .85))
        assertEquals(1, Fsrs6.interval(.001))
        assertEquals(36500, Fsrs6.interval(1e8))
        assertEquals(.9, Fsrs6.retrievability(10.0, 10.0), 1e-12)
        assertEquals(1.0, Fsrs6.retrievability(10.0, 0.9), 0.0)
    }

    @Test fun `legacy heuristic stability is not treated as FSRS memory`() {
        val item = VocabularyItem(word = "retain", persianMeaning = "حفظ کردن", stability = 200f,
            difficulty = 9f, lastReview = 1_700_000_000_000, schedulerVersion = "legacy")
        val result = SpacedRepetitionSystem.calculateNextReview(item, ReviewRating.GOOD, item.lastReview + Fsrs6.DAY_MS)
        assertEquals(2.3065f, result.newStability, 1e-6f)
        assertEquals(2, result.intervalDays)
    }

    @Test fun `lapse reduces memory and short-term success cannot reduce stability`() {
        val previous = Fsrs6.Memory(20.0, 5.0)
        assertTrue(Fsrs6.update(previous, 30.0, ReviewRating.AGAIN).stability < previous.stability)
        assertTrue(Fsrs6.update(previous, 0.0, ReviewRating.GOOD).stability >= previous.stability)
    }
}
