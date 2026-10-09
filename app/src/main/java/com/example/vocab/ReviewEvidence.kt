package com.example.vocab

import com.example.data.model.VocabularyReviewEvent

data class ReviewEvidenceSummary(val samples: Int, val recallRate: Double?, val brierScore: Double?)

object ReviewEvidence {
    /** Pre-review predictions are scored only against delayed, unhinted, exact-target responses. */
    fun summarize(events: List<VocabularyReviewEvent>): ReviewEvidenceSummary {
        val eligible = events.filter { it.category == "DUE" && it.elapsedDays >= 1 && !it.hintUsed && it.prediction != null &&
            it.axis in setOf(VocabularySkillAxis.RETRIEVAL.name, VocabularySkillAxis.ENGLISH_DEFINITION.name, VocabularySkillAxis.CONTEXT.name) && it.rating != null && it.source == "REVIEW" }
        if (eligible.isEmpty()) return ReviewEvidenceSummary(0, null, null)
        val successes = eligible.count { it.exactTarget && it.rating != 1 }
        val brier = eligible.sumOf { val y = if (it.exactTarget && it.rating != 1) 1.0 else 0.0; val p = it.prediction!!; (p-y)*(p-y) } / eligible.size
        return ReviewEvidenceSummary(eligible.size, successes.toDouble() / eligible.size, brier)
    }
}
