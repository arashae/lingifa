package com.example.vocab

import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularySkillProgress
import com.example.ui.screens.review.ReviewMode
import com.example.ui.screens.review.ReviewTask
import org.junit.Assert.*
import org.junit.Test

class LexicalBankTest {
    @Test fun `entire bank has unique senses original teaching material and sourced prompts`() {
        assertEquals(36, LexicalBank.relations.size)
        assertEquals(72, LexicalBank.words.size)
        assertEquals(72, LexicalBank.relations.flatMap { listOf(it.first.index, it.second.index) }.toSet().size)
        assertEquals(36, LexicalBank.relations.map { it.id }.toSet().size)
        LexicalBank.relations.forEach { pair ->
            assertTrue(pair.noteFa.isNotBlank())
            assertEquals(2, pair.sources.size)
            assertTrue(pair.sources.all { it.startsWith("https://dictionary.cambridge.org/") })
            listOf(pair.first, pair.second).forEach { sense ->
                assertTrue(sense.definition.isNotBlank() && sense.meaningFa.isNotBlank())
                assertTrue(sense.example.contains(sense.word))
                assertTrue(pair.prompt(sense.word).contains("____"))
                assertTrue(sense.word in pair.answers(sense.word))
                assertTrue(pair.answers(sense.word).all { it in setOf(pair.first.word, pair.second.word) })
            }
        }
    }

    @Test fun `both alternatives are accepted where context does not uniquely identify target`() {
        for (word in listOf("buy", "purchase", "finish", "end", "help", "assist")) {
            val pair = LexicalBank.forWord(word)!!
            assertEquals(setOf(pair.first.word, pair.second.word), pair.answers(word))
        }
        assertEquals(setOf("tell"), LexicalBank.forWord("tell")!!.answers("tell"))
        assertEquals(setOf("start"), LexicalBank.forWord("start")!!.answers("start"))
    }

    @Test fun `unknown counterpart and incompatible part of speech cannot enter review`() {
        assertNull(LexicalBank.forWord("buy", setOf("buy")))
        assertNotNull(LexicalBank.forWord("buy", setOf("buy", "purchase")))
        val noun = VocabularyItem(id = 1, word = "work", persianMeaning = "کار", partOfSpeech = "verb", correctCount = 2)
        val due = VocabularySkillProgress(1, "lexical:job-work:work", "SYNONYM", stability = 2.0, lastReview = 1, nextReview = 1)
        assertNotEquals(ReviewMode.SYNONYM, ReviewTask.forItem(noun, listOf(due), setOf("job", "work"), 2).mode)
    }
}
