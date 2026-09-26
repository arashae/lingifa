package com.example.ui.screens.review

import com.example.data.model.VocabularyItem
import kotlin.random.Random

/**
 * Builds a useful review session without weakening the spaced-repetition schedule.
 *
 * Scheduled/due cards always come first. If fewer than a full session are due, the remaining
 * slots are filled only from vocabulary the learner has already studied. Reinforcement favors
 * weak and recently learned words, then rotates through the rest so repeatedly opening Review
 * does not keep showing the same handful of cards.
 */
internal object ReviewQueuePolicy {
    const val DEFAULT_SESSION_LIMIT = 50
    const val CANDIDATE_POOL_LIMIT = 2_000

    private const val RECENT_WINDOW_MS = 3L * 24L * 60L * 60L * 1_000L
    private const val REINFORCEMENT_COOLDOWN_MS = 30L * 60L * 1_000L

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
                    .thenBy { it.mastery }
                    .thenByDescending { failurePressure(it) }
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

        val weak = preferredPool.sortedWith(
            compareBy<VocabularyItem> { it.mastery }
                .thenByDescending { failurePressure(it) }
                .thenBy { if (it.nextReview > 0L) it.nextReview else Long.MAX_VALUE }
                .thenBy { it.lastReview }
        )
        val recentCutoff = now - RECENT_WINDOW_MS
        val recent = preferredPool
            .filter { it.lastReview >= recentCutoff }
            .sortedByDescending { it.lastReview }
        val randomSeed = (now xor (now ushr 32) xor preferredPool.size.toLong()).toInt()
        val rotating = preferredPool.shuffled(Random(randomSeed))

        val weakQuota = (remaining + 1) / 2
        val recentQuota = (remaining - weakQuota + 1) / 2

        appendUnique(result, usedIds, weak, weakQuota, sessionLimit)
        appendUnique(result, usedIds, recent, recentQuota, sessionLimit)
        appendUnique(result, usedIds, rotating, sessionLimit - result.size, sessionLimit)

        // If the 30-minute cooldown excluded too many cards, reuse older studied cards only as a
        // final fallback. This guarantees Review remains available without ever introducing new words.
        if (result.size < sessionLimit) {
            val fallback = candidates.sortedWith(
                compareBy<VocabularyItem> { it.mastery }
                    .thenByDescending { failurePressure(it) }
                    .thenBy { it.lastReview }
            )
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

    private fun failurePressure(item: VocabularyItem): Float {
        val attempts = item.correctCount + item.incorrectCount
        if (attempts <= 0) return 0f
        return item.incorrectCount.toFloat() / attempts.toFloat()
    }
}
