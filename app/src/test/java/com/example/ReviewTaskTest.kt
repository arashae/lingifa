package com.example

import com.example.data.model.VocabularyItem
import com.example.ui.screens.review.ReviewMode
import com.example.ui.screens.review.ReviewTask
import com.example.vocab.VocabularySkillAxis
import com.example.vocab.VocabularyStudyPolicy
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

    private fun item(
        word: String,
        meaning: String,
        example: String = "",
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
            example = example,
            tags = tags
        )
    }
}
