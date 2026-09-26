package com.example.ui.screens.review

import com.example.data.model.VocabularyItem
import kotlin.random.Random

/**
 * Builds a useful review session without weakening the spaced-repetition schedule.
 *
 * Scheduled/due cards always come first. If fewer than a full session are due, the remaining
 * slots are filled only from vocabulary the learner has already studied. Reinforcement is
 * deliberately difficulty-weighted: most extra slots go to weak/hard words, while recent and
 * rotating words preserve coverage and prevent Review from becoming the same small loop.
 */
internal object ReviewQueuePolicy {
    const val DEFAULT_SESSION_LIMIT = 50
    const val CANDIDATE_POOL_LIMIT = 2_000

    private const val RECENT_WINDOW_MS = 3L * 24L * 60L * 60L * 1_000L
    private const val REINFORCEMENT_COOLDOWN_MS = 30L * 60L * 1_000L

    // After due cards, target roughly 60% hard/weak, 20% recently learned, 20% rotating mix.
    private const val WEAK_SHARE = 0.60f
    private const val RECENT_SHARE = 0.20f

    fun buildQueue(
        dueItems: List<VocabularyItem>,
        studiedItems: List<VocabularyItem>,
        now: Long = System.currentTimeMillis(),
        sessionLimit: Int = DEFAULT_SESSION_LIMIT
    ): List<VocabularyItem> {
        if (sessionLimit <= 0) return emptyList()

        val due = dueItems
            .asSequence()
            .filter(::isStudied)
            .distinctBy { it.id }
            .sortedWith(
                compareBy<VocabularyItem> { it.nextReview }
                    .thenByDescending { weaknessScore(it) }
            )
            .take(sessionLimit)
            .toMutableList()

        if (due.size >= sessionLimit) return due

        val usedIds = due.mapTo(mutableSetOf()) { it.id }
        val candidates = studiedItems
            .asSequence()
            .filter(::isStudied)
            .filterNot { it.id in usedIds }
            .distinctBy { it.id }
            .toList()

        if (candidates.isEmpty()) return due

        val result = due.toMutableList()
        val preferredCandidates = candidates.filter {
            it.lastReview <= 0L || now - it.lastReview >= REINFORCEMENT_COOLDOWN_MS
        }
        val preferredPool = preferredCandidates.ifEmpty { candidates }
        val remaining = sessionLimit - result.size

        val weak = preferredPool.sortedByDescending(::weaknessScore)
        val recentCutoff = now - RECENT_WINDOW_MS
        val recent = preferredPool
            .filter { it.lastReview >= recentCutoff }
            .sortedWith(
                compareByDescending<VocabularyItem> { weaknessScore(it) }
                    .thenByDescending { it.lastReview }
            )
        val randomSeed = (now xor (now ushr 32) xor preferredPool.size.toLong()).toInt()
        val rotating = preferredPool.shuffled(Random(randomSeed))

        val weakQuota = kotlin.math.ceil(remaining * WEAK_SHARE).toInt().coerceAtMost(remaining)
        val recentQuota = kotlin.math.ceil(remaining * RECENT_SHARE).toInt()
            .coerceAtMost((remaining - weakQuota).coerceAtLeast(0))

        appendUnique(result, usedIds, weak, weakQuota, sessionLimit)
        appendUnique(result, usedIds, recent, recentQuota, sessionLimit)
        appendUnique(result, usedIds, rotating, sessionLimit - result.size, sessionLimit)

        // If the 30-minute cooldown excluded too many cards, reuse studied cards only as a final
        // fallback. Difficulty still leads the fallback, and unseen words can never enter Review.
        if (result.size < sessionLimit) {
            val fallback = candidates.sortedByDescending(::weaknessScore)
            appendUnique(result, usedIds, fallback, sessionLimit - result.size, sessionLimit)
        }

        return result
    }

    private fun appendUnique(
        destination: MutableList<VocabularyItem>,
        usedIds: MutableSet<Long>,
        source: List<VocabularyItem>,
        requestedCount: Int,
        sessionLimit: Int
    ) {
        if (requestedCount <= 0 || destination.size >= sessionLimit) return

        var added = 0
        for (item in source) {
            if (destination.size >= sessionLimit || added >= requestedCount) break
            if (usedIds.add(item.id)) {
                destination += item
                added++
            }
        }
    }

    private fun isStudied(item: VocabularyItem): Boolean =
        item.correctCount > 0 || item.incorrectCount > 0

    /**
     * Composite difficulty signal for optional reinforcement.
     *
     * - low mastery is the strongest signal;
     * - repeated failures increase priority;
     * - SRS difficulty carries the effect of HARD/AGAIN ratings;
     * - low stability means the memory is still fragile.
     */
    private fun weaknessScore(item: VocabularyItem): Float {
        val masteryPressure = (100 - item.mastery.coerceIn(0, 100)) / 100f
        val failurePressure = failurePressure(item)
        val difficultyPressure = ((item.difficulty.coerceIn(1f, 10f) - 1f) / 9f)
        val stabilityPressure = 1f / (1f + item.stability.coerceAtLeast(0.1f))

        return masteryPressure * 0.45f +
            failurePressure * 0.30f +
            difficultyPressure * 0.15f +
            stabilityPressure * 0.10f
    }

    private fun failurePressure(item: VocabularyItem): Float {
        val attempts = item.correctCount + item.incorrectCount
        if (attempts <= 0) return 0f
        return item.incorrectCount.toFloat() / attempts.toFloat()
    }
}
