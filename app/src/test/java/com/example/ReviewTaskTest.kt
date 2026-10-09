package com.example

import com.example.data.model.VocabularyItem
import com.example.ui.screens.review.ReviewMode
import com.example.ui.screens.review.ReviewTask
import com.example.vocab.VocabularySkillAxis
import com.example.vocab.VocabularyStudyPolicy
import com.example.vocab.ReviewPersistencePolicy
import com.example.srs.ReviewRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewTaskTest {

    @Test
    fun `retrieval task hides the target and accepts case and punctuation differences`() {
        val task = ReviewTask.forItem(
            item(word = "allocate", meaning = "تخصیص دادن", lowest = VocabularySkillAxis.RETRIEVAL)
        )

        assertEquals(ReviewMode.WORD_RECALL, task.mode)
        assertEquals("تخصیص دادن", task.prompt)
        assertFalse(task.prompt.contains("allocate", ignoreCase = true))
        assertTrue(task.isCorrect("  ALLOCATE! "))
        assertFalse(task.isCorrect("assign"))
    }

    @Test
    fun `english definition task prompts without leaking the headword`() {
        val task = ReviewTask.forItem(
            item(
                word = "allocate",
                meaning = "تخصیص دادن",
                definition = "To distribute money or resources for a particular purpose.",
                lowest = VocabularySkillAxis.ENGLISH_DEFINITION
            )
        )

        assertEquals(ReviewMode.ENGLISH_DEFINITION, task.mode)
        assertEquals("To distribute money or resources for a particular purpose.", task.prompt)
        assertFalse(task.prompt.contains("allocate", ignoreCase = true))
        assertTrue(task.isCorrect("ALLOCATE"))
    }

    @Test
    fun `circular definitions are excluded as answer cues`() {
        val task = ReviewTask.forItem(
            item(
                word = "allocate",
                meaning = "تخصیص دادن",
                definition = "To allocate money or resources.",
                lowest = VocabularySkillAxis.ENGLISH_DEFINITION
            )
        )

        assertFalse(task.mode == ReviewMode.ENGLISH_DEFINITION)
        assertFalse(task.prompt.contains("allocate", ignoreCase = true))
    }

    @Test
    fun `context task requires a real example containing the target`() {
        val withExample = item(
            word = "allocate",
            meaning = "تخصیص دادن",
            example = "The committee will allocate more funds.",
            lowest = VocabularySkillAxis.CONTEXT
        )
        val task = ReviewTask.forItem(withExample)

        assertEquals(ReviewMode.CONTEXT, task.mode)
        assertEquals("The committee will ____ more funds.", task.prompt)

        val withoutTarget = ReviewTask.forItem(
            item(
                word = "allocate",
                meaning = "تخصیص دادن",
                example = "The committee approved additional funds.",
                lowest = VocabularySkillAxis.RETRIEVAL
            )
        )
        assertFalse(withoutTarget.prompt.contains("____"))
    }

    @Test
    fun `all reviewed contrast pairs reveal a Persian usage distinction`() {
        val reviewedWords = listOf(
            "avoid", "prevent", "economic", "economical", "say", "tell",
            "borrow", "lend", "rise", "raise", "remember", "remind",
            "affect", "effect", "advice", "advise", "accept", "except",
            "lose", "loose", "do", "make", "discover", "invent"
        )

        reviewedWords.forEach { word ->
            val task = ReviewTask.forItem(
                item(word = word, meaning = "معنی", lowest = VocabularySkillAxis.SYNONYM)
            )
            assertEquals(ReviewMode.SYNONYM, task.mode)
            assertTrue(task.isCorrect(word))
            assertTrue(task.answerNoteFa.isNotBlank())
            assertTrue(task.prompt.contains("____"))
        }
    }

    @Test
    fun `synonym contrast tasks only use reviewed sense-specific pairs`() {
        val pair = ReviewTask.forItem(
            item(word = "avoid", meaning = "اجتناب کردن", lowest = VocabularySkillAxis.SYNONYM)
        )
        val unrelated = ReviewTask.forItem(
            item(word = "radius", meaning = "شعاع", lowest = VocabularySkillAxis.SYNONYM)
        )

        assertEquals(ReviewMode.SYNONYM, pair.mode)
        assertTrue(pair.isCorrect("avoid"))
        assertFalse(pair.isCorrect("prevent"))
        assertFalse(unrelated.prompt.contains("or"))
        assertEquals(ReviewMode.WORD_RECALL, unrelated.mode)
    }

    @Test
    fun `repeated target occurrences are all hidden in context`() {
        val task = ReviewTask.forItem(item("allocate", "تخصیص دادن",
            example = "We allocate funds now and allocate staff later.", lowest = VocabularySkillAxis.CONTEXT))
        assertEquals("We ____ funds now and ____ staff later.", task.prompt)
    }

    @Test
    fun `internal punctuation errors are not erased to award a correct answer`() {
        val task = ReviewTask.forItem(item("allocate", "تخصیص دادن", lowest = VocabularySkillAxis.RETRIEVAL))
        assertFalse(task.isCorrect("al!locate"))
        assertFalse(task.isCorrect("allo\ncate"))
        assertTrue(task.isCorrect("  ALLOCATE! "))
    }

    @Test
    fun `typographic apostrophes are accepted`() {
        val task = ReviewTask.forItem(item("don't", "نکن", lowest = VocabularySkillAxis.RETRIEVAL))
        assertTrue(task.isCorrect("don’t"))
    }

    @Test
    fun `fully practised skills keep rotating instead of freezing on one mode`() {
        val now = 1_700_000_000_000L
        var current = item("allocate", "تخصیص دادن",
            example = "We allocate resources carefully.",
            definition = "To distribute resources for a particular purpose.",
            lowest = VocabularySkillAxis.SPELLING).copy(correctCount = 5, nextReview = now + 86_400_000L)
        val modes = mutableSetOf<ReviewMode>()
        repeat(4) {
            val task = ReviewTask.forItem(current)
            modes += task.mode
            current = ReviewPersistencePolicy.apply(current, task.mode.skill, ReviewRating.GOOD, now)
        }
        assertEquals(setOf(ReviewMode.MEANING, ReviewMode.WORD_RECALL, ReviewMode.ENGLISH_DEFINITION, ReviewMode.CONTEXT), modes)
    }

    private fun item(
        word: String,
        meaning: String,
        example: String = "",
        definition: String = "",
        lowest: VocabularySkillAxis
    ): VocabularyItem {
        val axes = VocabularySkillAxis.values().associateWith { axis ->
            if (axis == lowest) 0 else 100
        }
        val tags = axes.map { (axis, score) -> "${axis.tagPrefix}$score" }
        return VocabularyItem(
            id = 21,
            word = word,
            persianMeaning = meaning,
            englishDefinition = definition,
            example = example,
            tags = tags
        )
    }
}
