package com.example.ui.screens.review

import com.example.data.model.VocabularyItem
import com.example.vocab.VocabularySkillAxis
import com.example.vocab.VocabularyStudyPolicy
import java.util.Locale

enum class ReviewMode(
    val skill: VocabularySkillAxis,
    val instruction: String,
    val requiresTypedAnswer: Boolean
) {
    MEANING(VocabularySkillAxis.MEANING, "Recall the meaning", false),
    WORD_RECALL(VocabularySkillAxis.RETRIEVAL, "Write the English word", true),
    CONTEXT(VocabularySkillAxis.CONTEXT, "Complete the sentence", true),
    SYNONYM(VocabularySkillAxis.SYNONYM, "Choose the word that fits", true)
}

data class ReviewTask(
    val item: VocabularyItem,
    val mode: ReviewMode,
    val prompt: String,
    val expectedAnswer: String
) {
    fun isCorrect(answer: String): Boolean =
        normalize(answer) == normalize(expectedAnswer)

    companion object {
        private data class Contrast(
            val first: String,
            val second: String,
            val firstSentence: String,
            val secondSentence: String
        )

        // Use hand-written, sense-specific contrasts. Raw dictionary synonym fields are not
        // answer keys: they can include near-neighbours, different parts of speech, or errors.
        private val reviewedContrasts = listOf(
            Contrast(
                first = "avoid",
                second = "prevent",
                firstSentence = "I left early to ____ the rush-hour traffic.",
                secondSentence = "The new safety checks may ____ serious accidents."
            ),
            Contrast(
                first = "economic",
                second = "economical",
                firstSentence = "The report examines the country's ____ growth.",
                secondSentence = "This small car is cheap and ____ to run."
            ),
            Contrast(
                first = "say",
                second = "tell",
                firstSentence = "What did the witness ____ about the accident?",
                secondSentence = "Please ____ me the truth."
            )
        )

        fun forItem(item: VocabularyItem): ReviewTask {
            val candidates = buildList {
                add(ReviewMode.MEANING)
                if (item.persianMeaning.isNotBlank()) add(ReviewMode.WORD_RECALL)
                if (VocabularyStudyPolicy.clozeSentence(item) != null) add(ReviewMode.CONTEXT)
                if (reviewedContrasts.any {
                        item.word.equals(it.first, ignoreCase = true) ||
                            item.word.equals(it.second, ignoreCase = true)
                    }
                ) add(ReviewMode.SYNONYM)
            }
            val leastPracticed = candidates.minOf { VocabularyStudyPolicy.skillMastery(item, it.skill) }
            val tied = candidates.filter { VocabularyStudyPolicy.skillMastery(item, it.skill) == leastPracticed }
            // Spread first encounters across eligible modes; then prioritize the weakest skill.
            val mode = tied[Math.floorMod(item.id, tied.size.toLong()).toInt()]

            return when (mode) {
                ReviewMode.MEANING -> ReviewTask(item, mode, item.word, item.persianMeaning)
                ReviewMode.WORD_RECALL -> ReviewTask(item, mode, item.persianMeaning, item.word)
                ReviewMode.CONTEXT -> ReviewTask(
                    item, mode, VocabularyStudyPolicy.clozeSentence(item).orEmpty(), item.word
                )
                ReviewMode.SYNONYM -> {
                    val contrast = reviewedContrasts.first {
                        item.word.equals(it.first, ignoreCase = true) ||
                            item.word.equals(it.second, ignoreCase = true)
                    }
                    val firstIsTarget = item.word.equals(contrast.first, ignoreCase = true)
                    val sentence = if (firstIsTarget) contrast.firstSentence else contrast.secondSentence
                    ReviewTask(
                        item,
                        mode,
                        "${contrast.first.replaceFirstChar { it.uppercase() }} or ${contrast.second}?\n$sentence",
                        item.word
                    )
                }
            }
        }

        private fun normalize(value: String): String = value
            .trim()
            .lowercase(Locale.US)
            .replace(Regex("[^\\p{L}\\p{N}' -]"), "")
            .replace(Regex("\\s+"), " ")
    }
}
