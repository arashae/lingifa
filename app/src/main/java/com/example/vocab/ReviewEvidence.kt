package com.example.vocab

import com.example.data.model.VocabularyReviewEvent
import com.example.data.model.VocabularySkillProgress
import com.example.srs.Fsrs6

data class ReviewEvidenceSummary(val samples: Int, val recallRate: Double?, val brierScore: Double?,
    val reviewsLastSevenDays: Int = 0, val dailyWorkloadByRetention: Map<Double, Double> = emptyMap(),
    val lexicalAttempts: Int = 0, val counterpartConfusions: Int = 0)

object ReviewEvidence {
    /** Pre-review predictions are scored only against delayed, unhinted, exact-target responses. */
    fun summarize(events: List<VocabularyReviewEvent>, states: List<VocabularySkillProgress> = emptyList(),
        reviewsLastSevenDays: Int = 0): ReviewEvidenceSummary {
        // A steady-state estimate from current stability, not a promise about tomorrow's queue.
        val initialized = states.filter { it.lastReview > 0 && it.stability.isFinite() && it.stability > 0 }
        val workload = if (initialized.isEmpty()) emptyMap() else listOf(.85, .9, .95).associateWith { target ->
            initialized.sumOf { 1.0 / Fsrs6.interval(it.stability, target) }
        }
        val lexical = events.filter { it.axis == VocabularySkillAxis.SYNONYM.name &&
            it.category == "DUE" && it.elapsedDays >= 1 && !it.hintUsed && it.rating != null && it.source == "REVIEW" }
        val confusions = lexical.count { event ->
            val pair = LexicalBank.forWord(event.expectedAnswer)
            pair != null && event.rating == 1 && event.answer.trim().lowercase(java.util.Locale.US) == pair.partner(event.expectedAnswer).word
        }
        val eligible = events.filter { it.category == "DUE" && it.elapsedDays >= 1 && !it.hintUsed && it.prediction != null &&
            it.axis in setOf(VocabularySkillAxis.RETRIEVAL.name, VocabularySkillAxis.ENGLISH_DEFINITION.name, VocabularySkillAxis.CONTEXT.name) && it.rating != null && it.source == "REVIEW" }
        if (eligible.isEmpty()) return ReviewEvidenceSummary(0, null, null, reviewsLastSevenDays, workload, lexical.size, confusions)
        val successes = eligible.count { it.exactTarget && it.rating != 1 }
        val brier = eligible.sumOf { val y = if (it.exactTarget && it.rating != 1) 1.0 else 0.0; val p = it.prediction!!; (p-y)*(p-y) } / eligible.size
        return ReviewEvidenceSummary(eligible.size, successes.toDouble() / eligible.size, brier, reviewsLastSevenDays, workload, lexical.size, confusions)
    }
}
