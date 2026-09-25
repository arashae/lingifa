package com.example

import com.example.data.model.VocabularyItem
import com.example.vocab.LearningLifecycle
import com.example.vocab.VocabularySkillAxis
import com.example.vocab.VocabularyStudyPolicy
import com.example.vocab.VocabularyTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyStudyPolicyTest {

    @Test
    fun `IELTS core and extended keep exact target sizes`() {
        val words = (1..3_000).map { index ->
            VocabularyItem(
                id = index.toLong(),
                word = "word$index",
                persianMeaning = "معنی $index",
                englishDefinition = "A useful definition for word $index.",
                example = "This is an example using word$index.",
                cefrLevel = "B2",
                ieltsRelevance = if (index <= 2_000) "High" else "Low",
                frequencyRank = index,
                examPriority = if (index <= 2_000) 4 else 0,
                learningOrder = index
            )
        }

        val core = VocabularyStudyPolicy.idsForTier(words, "pack_ielts_master", VocabularyTier.CORE)
        val extended = VocabularyStudyPolicy.idsForTier(words, "pack_ielts_master", VocabularyTier.EXTENDED)

        assertEquals(2_000, core?.size)
        assertEquals(1_000, extended?.size)
        assertTrue(core!!.intersect(extended!!).isEmpty())
    }

    @Test
    fun `priority favors exam relevant complete cards and downranks obvious C2 places`() {
        val useful = VocabularyItem(
            word = "establish",
            persianMeaning = "تأسیس کردن",
            englishDefinition = "To create or set something up on a firm basis.",
            example = "The researchers established a clear relationship between the variables.",
            cefrLevel = "B2",
            ieltsRelevance = "High",
            frequencyRank = 1_200,
            examPriority = 4
        )
        val place = VocabularyItem(
            word = "hampshire",
            persianMeaning = "همپشایر",
            englishDefinition = "County of southern England on the English Channel.",
            example = "They travelled through Hampshire.",
            cefrLevel = "C2",
            ieltsRelevance = "Low",
            frequencyRank = 7_000,
            examPriority = 0
        )

        assertTrue(
            VocabularyStudyPolicy.priorityScore(useful, "pack_ielts_master") >
                VocabularyStudyPolicy.priorityScore(place, "pack_ielts_master")
        )
    }

    @Test
    fun `unseen words never become due review items`() {
        val now = 1_000_000L
        val unseen = VocabularyItem(
            id = 1,
            word = "unseen",
            persianMeaning = "جدید",
            nextReview = 0L
        )
        val due = VocabularyItem(
            id = 2,
            word = "due",
            persianMeaning = "مرور",
            correctCount = 1,
            mastery = 20,
            nextReview = now - 1
        )
        val weak = VocabularyItem(
            id = 3,
            word = "weak",
            persianMeaning = "ضعیف",
            correctCount = 2,
            mastery = 35,
            nextReview = now + 100_000
        )

        val plan = VocabularyStudyPolicy.dailyPlan(listOf(unseen, due, weak), newWordLimit = 15, now = now)

        assertEquals(1, plan.dueReviews)
        assertEquals(1, plan.weakWords)
        assertEquals(1, plan.newWords)
        assertEquals(LearningLifecycle.UNSEEN, VocabularyStudyPolicy.lifecycle(unseen))
    }

    @Test
    fun `daily plan counts match per-session queue caps`() {
        val now = 2_000_000L
        val due = (1..45).map { index ->
            VocabularyItem(
                id = index.toLong(),
                word = "due$index",
                persianMeaning = "مرور $index",
                correctCount = 1,
                mastery = 20,
                nextReview = now - 1
            )
        }
        val weak = (46..70).map { index ->
            VocabularyItem(
                id = index.toLong(),
                word = "weak$index",
                persianMeaning = "ضعیف $index",
                correctCount = 2,
                mastery = 40,
                nextReview = now + 100_000
            )
        }
        val unseen = (71..100).map { index ->
            VocabularyItem(
                id = index.toLong(),
                word = "new$index",
                persianMeaning = "جدید $index"
            )
        }

        val plan = VocabularyStudyPolicy.dailyPlan(due + weak + unseen, newWordLimit = 15, now = now)

        assertEquals(VocabularyStudyPolicy.MAX_DUE_REVIEWS_PER_SESSION, plan.dueReviews)
        assertEquals(VocabularyStudyPolicy.MAX_WEAK_WORDS_PER_SESSION, plan.weakWords)
        assertEquals(15, plan.newWords)
        assertEquals(55, plan.total)
    }

    @Test
    fun `spelling and context mastery stay independent from semantic mastery`() {
        val original = VocabularyItem(
            word = "environment",
            persianMeaning = "محیط",
            mastery = 76,
            tags = emptyList()
        )

        val spellingUpdated = VocabularyStudyPolicy.withSkillResult(
            original,
            VocabularySkillAxis.SPELLING,
            success = false
        )
        val contextUpdated = VocabularyStudyPolicy.withSkillResult(
            spellingUpdated,
            VocabularySkillAxis.CONTEXT,
            success = true
        )

        assertEquals(76, contextUpdated.mastery)
        assertEquals(0, VocabularyStudyPolicy.skillMastery(contextUpdated, VocabularySkillAxis.SPELLING))
        assertEquals(12, VocabularyStudyPolicy.skillMastery(contextUpdated, VocabularySkillAxis.CONTEXT))
    }

    @Test
    fun `cloze uses the real stored example`() {
        val item = VocabularyItem(
            word = "allocate",
            persianMeaning = "تخصیص دادن",
            example = "The committee will allocate additional funds to the project."
        )

        val cloze = VocabularyStudyPolicy.clozeSentence(item)
        assertNotNull(cloze)
        assertEquals("The committee will ____ additional funds to the project.", cloze)
    }
}
