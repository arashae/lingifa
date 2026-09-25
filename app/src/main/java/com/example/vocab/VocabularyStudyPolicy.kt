package com.example.vocab

import com.example.data.model.VocabularyItem
import java.util.Locale

enum class VocabularyTier {
    ALL,
    CORE,
    EXTENDED
}

enum class LearningLifecycle {
    UNSEEN,
    LEARNING,
    REVIEW,
    MASTERED
}

enum class VocabularySkillAxis(val tagPrefix: String) {
    SPELLING("linguafa:skill:spelling:"),
    CONTEXT("linguafa:skill:context:")
}

data class VocabularyMasteryStats(
    val unseen: Int = 0,
    val learning: Int = 0,
    val review: Int = 0,
    val mastered: Int = 0
)

data class VocabularyDailyPlan(
    val dueReviews: Int = 0,
    val weakWords: Int = 0,
    val newWords: Int = 0,
    val newAvailable: Int = 0,
    val newWordLimit: Int = 15
) {
    val total: Int get() = dueReviews + weakWords + newWords
}

/**
 * Central vocabulary-study policy.
 *
 * The source datasets deliberately remain broad. This policy turns that catalog into a useful
 * learning queue by ranking words for the selected exam, splitting large exam banks into Core and
 * Extended tiers, and keeping meaning/context/spelling progress independent.
 *
 * priorityScore is calculated rather than stored so it never becomes stale after a dataset update.
 */
object VocabularyStudyPolicy {
    const val PREFS_NAME = "vocabulary_study_preferences"
    const val KEY_DAILY_NEW_LIMIT = "daily_new_word_limit"
    val DAILY_NEW_LIMIT_OPTIONS = listOf(10, 15, 20, 25)

    // Keep the dashboard counts identical to what ReviewViewModel actually schedules per session.
    const val MAX_DUE_REVIEWS_PER_SESSION = 30
    const val MAX_WEAK_WORDS_PER_SESSION = 10

    const val IELTS_CORE_LIMIT = 2_000
    const val TOEFL_CORE_LIMIT = 2_500
    const val GRE_CORE_LIMIT = 2_000

    fun coreLimit(packId: String?): Int? = when (packId) {
        "pack_ielts_master" -> IELTS_CORE_LIMIT
        "pack_toefl_master" -> TOEFL_CORE_LIMIT
        "pack_gre_master" -> GRE_CORE_LIMIT
        else -> null
    }

    fun supportsTiers(packId: String?): Boolean = coreLimit(packId) != null

    fun priorityScore(item: VocabularyItem, packId: String? = null): Int {
        var score = 0

        score += when {
            item.frequencyRank in 1..500 -> 28
            item.frequencyRank in 501..1_500 -> 24
            item.frequencyRank in 1_501..3_000 -> 19
            item.frequencyRank in 3_001..6_000 -> 13
            item.frequencyRank > 0 -> 7
            else -> 4
        }

        score += (item.examPriority.coerceIn(0, 4) * 8)

        val relevance = when (packId) {
            "pack_ielts_master" -> item.ieltsRelevance
            "pack_toefl_master" -> item.toeflRelevance
            "pack_gre_master" -> item.greRelevance
            else -> strongestRelevance(item)
        }
        score += when (relevance.trim().lowercase(Locale.US)) {
            "high" -> 24
            "medium" -> 12
            else -> 0
        }

        if (item.englishDefinition.isNotBlank()) score += 5
        if (item.example.isNotBlank()) score += 5
        if (item.ipa.isNotBlank()) score += 2
        if (item.collocations.isNotEmpty()) score += 3

        score += when (item.cefrLevel.trim().uppercase(Locale.US)) {
            "B1" -> 4
            "B2" -> 6
            "C1" -> 5
            "A2" -> 2
            else -> 0
        }

        if (looksLikeLowValueProperNoun(item)) score -= 38
        if (item.tags.any { it == "needs-definition" || it == "needs-example" }) score -= 8

        return score.coerceIn(0, 100)
    }

    fun sortForStudy(items: List<VocabularyItem>, packId: String? = null): List<VocabularyItem> {
        return items.sortedWith(
            compareByDescending<VocabularyItem> { priorityScore(it, packId) }
                .thenBy { if (it.learningOrder > 0) it.learningOrder else Int.MAX_VALUE }
                .thenBy { if (it.frequencyRank > 0) it.frequencyRank else Int.MAX_VALUE }
                .thenBy { it.word.lowercase(Locale.US) }
        )
    }

    fun idsForTier(
        items: List<VocabularyItem>,
        packId: String?,
        tier: VocabularyTier
    ): Set<Long>? {
        if (tier == VocabularyTier.ALL) return null
        val limit = coreLimit(packId) ?: return null
        val ranked = sortForStudy(items, packId)
        return when (tier) {
            VocabularyTier.CORE -> ranked.take(limit).mapTo(linkedSetOf()) { it.id }
            VocabularyTier.EXTENDED -> ranked.drop(limit).mapTo(linkedSetOf()) { it.id }
            VocabularyTier.ALL -> null
        }
    }

    fun lifecycle(item: VocabularyItem): LearningLifecycle {
        val attempts = item.correctCount + item.incorrectCount
        return when {
            attempts == 0 -> LearningLifecycle.UNSEEN
            item.mastery >= 70 && item.correctCount >= 4 -> LearningLifecycle.MASTERED
            attempts <= 1 || item.mastery < 30 -> LearningLifecycle.LEARNING
            else -> LearningLifecycle.REVIEW
        }
    }

    fun masteryStats(items: List<VocabularyItem>): VocabularyMasteryStats {
        var unseen = 0
        var learning = 0
        var review = 0
        var mastered = 0
        items.forEach { item ->
            when (lifecycle(item)) {
                LearningLifecycle.UNSEEN -> unseen++
                LearningLifecycle.LEARNING -> learning++
                LearningLifecycle.REVIEW -> review++
                LearningLifecycle.MASTERED -> mastered++
            }
        }
        return VocabularyMasteryStats(unseen, learning, review, mastered)
    }

    fun dailyPlan(
        items: List<VocabularyItem>,
        newWordLimit: Int,
        now: Long = System.currentTimeMillis()
    ): VocabularyDailyPlan {
        val safeLimit = newWordLimit.coerceIn(1, 50)
        val dueIds = items.asSequence()
            .filter { it.correctCount + it.incorrectCount > 0 && it.nextReview <= now }
            .map { it.id }
            .toSet()
        val weakAvailable = items.count {
            it.correctCount + it.incorrectCount > 0 &&
                it.id !in dueIds &&
                it.mastery < 50 &&
                lifecycle(it) != LearningLifecycle.MASTERED
        }
        val unseen = items.count { lifecycle(it) == LearningLifecycle.UNSEEN }
        return VocabularyDailyPlan(
            dueReviews = minOf(MAX_DUE_REVIEWS_PER_SESSION, dueIds.size),
            weakWords = minOf(MAX_WEAK_WORDS_PER_SESSION, weakAvailable),
            newWords = minOf(safeLimit, unseen),
            newAvailable = unseen,
            newWordLimit = safeLimit
        )
    }

    fun skillMastery(item: VocabularyItem, axis: VocabularySkillAxis): Int {
        return item.tags.firstNotNullOfOrNull { tag ->
            if (tag.startsWith(axis.tagPrefix)) {
                tag.removePrefix(axis.tagPrefix).toIntOrNull()?.coerceIn(0, 100)
            } else {
                null
            }
        } ?: 0
    }

    fun withSkillResult(
        item: VocabularyItem,
        axis: VocabularySkillAxis,
        success: Boolean
    ): VocabularyItem {
        val current = skillMastery(item, axis)
        val delta = when (axis) {
            VocabularySkillAxis.SPELLING -> if (success) 15 else -10
            VocabularySkillAxis.CONTEXT -> if (success) 12 else -12
        }
        val next = (current + delta).coerceIn(0, 100)
        val cleanTags = item.tags.filterNot { it.startsWith(axis.tagPrefix) }
        return item.copy(
            tags = cleanTags + "${axis.tagPrefix}$next",
            updatedAt = System.currentTimeMillis()
        )
    }

    fun clozeSentence(item: VocabularyItem): String? {
        val example = item.example.trim()
        if (example.isBlank() || item.word.isBlank()) return null
        val regex = Regex("(?i)(?<![A-Za-z])${Regex.escape(item.word.trim())}(?![A-Za-z])")
        if (!regex.containsMatchIn(example)) return null
        return regex.replaceFirst(example, "____")
    }

    private fun strongestRelevance(item: VocabularyItem): String {
        val values = listOf(item.ieltsRelevance, item.toeflRelevance, item.greRelevance)
        return when {
            values.any { it.equals("High", ignoreCase = true) } -> "High"
            values.any { it.equals("Medium", ignoreCase = true) } -> "Medium"
            else -> "Low"
        }
    }

    /**
     * C2 assets intentionally behave as "C2 + Advanced". Obvious names/places are not deleted
     * because they may still be useful in a dictionary, but they are strongly pushed down in the
     * learning order.
     */
    private fun looksLikeLowValueProperNoun(item: VocabularyItem): Boolean {
        if (!item.cefrLevel.equals("C2", ignoreCase = true)) return false
        if (item.tags.any {
                it.contains("proper", ignoreCase = true) ||
                    it.contains("name", ignoreCase = true) ||
                    it.contains("place", ignoreCase = true)
            }
        ) return true

        val definition = item.englishDefinition.lowercase(Locale.US)
        val patterns = listOf(
            "county of ",
            "city in ",
            "town in ",
            "island in ",
            "island of ",
            "capital of ",
            "province of ",
            "state in ",
            "surname",
            "given name",
            "family name"
        )
        return patterns.any(definition::contains)
    }
}
