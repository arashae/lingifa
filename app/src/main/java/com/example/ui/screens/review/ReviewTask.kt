package com.example.ui.screens.review

import com.example.data.model.VocabularyItem
import com.example.vocab.VocabularySkillAxis
import com.example.vocab.VocabularyStudyPolicy
import com.example.vocab.ReviewPersistencePolicy
import java.util.Locale
import com.example.vocab.LexicalBank
import com.example.vocab.LexicalRelation
import com.example.data.model.VocabularySkillProgress

enum class ReviewMode(
    val skill: VocabularySkillAxis,
    val instruction: String,
    val requiresTypedAnswer: Boolean
) {
    MEANING(VocabularySkillAxis.MEANING, "Recall the meaning", false),
    WORD_RECALL(VocabularySkillAxis.RETRIEVAL, "Write the English word", true),
    ENGLISH_DEFINITION(
        VocabularySkillAxis.ENGLISH_DEFINITION,
        "Write the word from its English definition",
        true
    ),
    CONTEXT(VocabularySkillAxis.CONTEXT, "Complete the sentence", true),
    SYNONYM(VocabularySkillAxis.SYNONYM, "Choose the word that fits", true)
}

data class ReviewTask(
    val item: VocabularyItem,
    val mode: ReviewMode,
    val prompt: String,
    val expectedAnswer: String,
    val answerNoteFa: String = "",
    val acceptedAnswers: Set<String> = setOf(expectedAnswer),
    val senseKey: String = "primary",
    val senseId: Long? = null,
    val skillProgress: VocabularySkillProgress? = null,
    val lexicalRelation: LexicalRelation? = null
) {
    fun isCorrect(answer: String): Boolean =
        acceptedAnswers.any { normalize(answer) == normalize(it) }

    fun isExactTarget(answer: String): Boolean = normalize(answer) == normalize(expectedAnswer)

    /** Interval previews use this sense/skill's memory, never another skill's schedule. */
    val predictionItem: VocabularyItem get() = skillProgress?.takeIf { it.lastReview > 0 }?.let {
        item.copy(stability = it.stability.toFloat(), difficulty = it.difficulty.toFloat(),
            lastReview = it.lastReview, nextReview = it.nextReview, correctCount = it.correctCount,
            incorrectCount = it.incorrectCount, schedulerVersion = it.schedulerVersion)
    } ?: item.copy(lastReview = 0, schedulerVersion = "legacy")

    val isSkillDue: Boolean get() = skillProgress?.let { it.lastReview == 0L || it.nextReview <= System.currentTimeMillis() } ?: true

    val allowsAlternative: Boolean get() = mode.requiresTypedAnswer

    val targetHint: String get() = "Target: ${expectedAnswer.firstOrNull() ?: '?'}… (${expectedAnswer.length} characters)"

    companion object {
        fun forItem(item: VocabularyItem, progress: List<VocabularySkillProgress> = emptyList(),
            knownWords: Set<String>? = null, now: Long = System.currentTimeMillis()): ReviewTask {
            val relation = LexicalBank.forWord(item.word, knownWords)?.takeIf {
                item.partOfSpeech.lowercase() in setOf("", "word", it.sense(item.word).partOfSpeech) &&
                    (knownWords == null || item.correctCount > 0)
            }
            fun key(mode: ReviewMode) = if (mode == ReviewMode.SYNONYM && relation != null)
                "lexical:${relation.id}:${item.word.lowercase()}" else "primary"
            fun state(mode: ReviewMode) = progress.firstOrNull { it.axis == mode.skill.name && it.senseKey == key(mode) }

            val candidates = buildList {
                add(ReviewMode.MEANING)
                if (item.persianMeaning.isNotBlank()) add(ReviewMode.WORD_RECALL)
                if (VocabularyStudyPolicy.definitionCue(item) != null) add(ReviewMode.ENGLISH_DEFINITION)
                if (VocabularyStudyPolicy.clozeSentence(item) != null) add(ReviewMode.CONTEXT)
                if (relation != null) add(ReviewMode.SYNONYM)
            }
            val dueModes = candidates.filter { mode -> state(mode)?.let { it.lastReview > 0 && it.nextReview <= now } == true }
            val eligible = dueModes.ifEmpty { candidates }
            val leastPracticed = eligible.minOf { VocabularyStudyPolicy.skillMastery(item, it.skill) }
            val tied = eligible.filter { VocabularyStudyPolicy.skillMastery(item, it.skill) == leastPracticed }
            // Spread first encounters across eligible modes; then prioritize the weakest skill.
            val mode = tied[Math.floorMod(item.id + ReviewPersistencePolicy.sequence(item), tied.size.toLong()).toInt()]

            return when (mode) {
                ReviewMode.MEANING -> ReviewTask(item, mode, item.word, item.persianMeaning)
                ReviewMode.WORD_RECALL -> ReviewTask(item, mode, item.persianMeaning, item.word)
                ReviewMode.ENGLISH_DEFINITION -> ReviewTask(
                    item, mode, VocabularyStudyPolicy.definitionCue(item).orEmpty(), item.word
                )
                ReviewMode.CONTEXT -> ReviewTask(
                    item, mode, VocabularyStudyPolicy.clozeSentence(item).orEmpty(), item.word
                )
                ReviewMode.SYNONYM -> {
                    val pair = requireNotNull(relation)
                    ReviewTask(item, mode, "${pair.first.word} or ${pair.second.word}?\n${pair.prompt(item.word)}",
                        item.word, answerNoteFa = pair.noteFa, acceptedAnswers = pair.answers(item.word),
                        senseKey = key(mode), lexicalRelation = pair)
                }
            }.let { it.copy(skillProgress = state(mode)) }
        }

        private fun normalize(value: String): String = value
            .trim()
            .lowercase(Locale.US)
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace('–', '-')
            .replace('—', '-')
            .trimEnd('.', ',', '!', '?', ';', ':')
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
