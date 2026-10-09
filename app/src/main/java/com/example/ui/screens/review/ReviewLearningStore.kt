package com.example.ui.screens.review

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularySkillProgress
import com.example.data.model.VocabularyReviewEvent
import com.example.srs.Fsrs6
import com.example.srs.ReviewRating
import com.example.srs.SpacedRepetitionSystem
import com.example.vocab.LexicalBank
import com.example.vocab.ReviewPersistencePolicy
import com.example.vocab.VocabularySkillAxis
import com.example.vocab.VocabularyStudyPolicy

class ReviewLearningStore(private val db: AppDatabase) {
    suspend fun recordBase(item: VocabularyItem, rating: ReviewRating, now: Long = System.currentTimeMillis(), source: String = "LEARNING") {
        record(ReviewTask(item, ReviewMode.MEANING, item.word, item.persianMeaning), "", rating, now, false, source)
    }

    suspend fun record(task: ReviewTask, answer: String, rating: ReviewRating?, now: Long,
        hintUsed: Boolean = false, source: String = "REVIEW") = db.withTransaction {
        val item = db.vocabularyDao().getByIdSync(task.item.id) ?: error("Word deleted")
        require(item.word == task.item.word && item.persianMeaning == task.item.persianMeaning &&
            item.example == task.item.example && item.englishDefinition == task.item.englishDefinition) { "Card changed" }
        val dao = db.reviewLearningDao()
        val states = dao.skills(item.id)
        val before = states.firstOrNull { it.senseKey == task.senseKey && it.axis == task.mode.skill.name }
        val initialized = before != null && before.stability > 0 && before.lastReview > 0 && before.schedulerVersion == Fsrs6.VERSION
        val due = initialized && before!!.nextReview <= now
        val elapsed = if (initialized) ((now - before!!.lastReview).coerceAtLeast(0) / Fsrs6.DAY_MS).toDouble() else 0.0
        val exact = task.mode.requiresTypedAnswer && task.isExactTarget(answer)
        val verifiedAlternative = rating != null && task.mode.requiresTypedAnswer && !exact && task.isCorrect(answer)
        val effective = if (hintUsed && rating != null && rating != ReviewRating.AGAIN) ReviewRating.HARD else rating
        val retention = dao.settings()?.desiredRetention ?: 0.9
        val prediction = if (initialized) Fsrs6.retrievability(before!!.stability, elapsed) else null
        val category = when {
            rating == null -> "UNGRADED"
            verifiedAlternative -> "VERIFIED_ALTERNATIVE"
            !initialized -> "INITIAL"
            due -> "DUE"
            else -> "EARLY"
        }
        var progress = before ?: VocabularySkillProgress(item.id, task.senseKey, task.mode.skill.name, nextReview = item.nextReview)
        if (effective != null && !verifiedAlternative && (!initialized || due || effective == ReviewRating.AGAIN)) {
            val memory = Fsrs6.update(if (initialized) Fsrs6.Memory(progress.stability, progress.difficulty) else null, elapsed, effective)
            val days = if (effective == ReviewRating.AGAIN) 0 else Fsrs6.interval(memory.stability, retention)
            val scheduled = now + if (days == 0) ReviewPersistencePolicy.COOLDOWN_MS else days * Fsrs6.DAY_MS
            val next = if (initialized && !due && effective == ReviewRating.AGAIN) minOf(progress.nextReview, scheduled) else scheduled
            progress = progress.copy(stability = memory.stability, difficulty = memory.difficulty,
                firstReview = if (initialized) progress.firstReview.takeIf { it > 0 } ?: now else now, lastReview = now,
                nextReview = next,
                correctCount = progress.correctCount + if (effective == ReviewRating.AGAIN) 0 else 1,
                incorrectCount = progress.incorrectCount + if (effective == ReviewRating.AGAIN) 1 else 0,
                independentSuccesses = (if (initialized) progress.independentSuccesses else 0) + if (!hintUsed && !verifiedAlternative && effective != ReviewRating.AGAIN) 1 else 0,
                schedulerVersion = Fsrs6.VERSION)
        }
        progress = progress.copy(lastPractice = now)
        dao.saveSkill(progress)
        var updated = if (rating == null || verifiedAlternative) item else VocabularyStudyPolicy.withSkillResult(item, task.mode.skill, effective != ReviewRating.AGAIN)
        updated = ReviewPersistencePolicy.markPracticed(updated, now)
        // Canonical counts remain compatible with older screens; semantic alternatives never
        // certify recall of the intended headword, and one axis cannot postpone another.
        if (effective != null && !verifiedAlternative &&
            (effective == ReviewRating.AGAIN || (item.nextReview <= now && (!initialized || due)))) {
            val interval = if (effective == ReviewRating.AGAIN) 0 else Fsrs6.interval(progress.stability, retention)
            updated = updated.copy(intervalDays = interval, stability = progress.stability.toFloat(),
                difficulty = progress.difficulty.toFloat(),
                correctCount = item.correctCount + if (effective == ReviewRating.AGAIN) 0 else 1,
                incorrectCount = item.incorrectCount + if (effective == ReviewRating.AGAIN) 1 else 0,
                lastReview = now, schedulerVersion = Fsrs6.VERSION)
            updated = updated.copy(mastery = SpacedRepetitionSystem.calculateMastery(updated))
        }
        val all = states.filterNot { it.senseKey == progress.senseKey && it.axis == progress.axis } + progress
        val known = if (all.any { it.axis == VocabularySkillAxis.SYNONYM.name })
            db.vocabularyDao().getByNormalizedWords(LexicalBank.words.toList())
                .filter { it.correctCount > 0 }.map { it.word.lowercase(java.util.Locale.US) }.toSet() else emptySet()
        val relation = LexicalBank.forWord(item.word, known)?.takeIf {
            item.partOfSpeech.lowercase(java.util.Locale.US) in setOf("", "word", it.sense(item.word).partOfSpeech)
        }
        // A removed definition/example or unknown/deleted counterpart must not trap the
        // vocabulary due date behind a skill that can no longer produce a valid task.
        val active = all.filter { state -> state.lastReview > 0 && when (state.axis) {
            VocabularySkillAxis.MEANING.name -> true
            VocabularySkillAxis.RETRIEVAL.name -> item.persianMeaning.isNotBlank()
            VocabularySkillAxis.ENGLISH_DEFINITION.name -> VocabularyStudyPolicy.definitionCue(item) != null
            VocabularySkillAxis.CONTEXT.name -> VocabularyStudyPolicy.clozeSentence(item) != null
            VocabularySkillAxis.SYNONYM.name -> relation != null && state.senseKey == "lexical:${relation.id}:${item.word.lowercase(java.util.Locale.US)}"
            else -> false
        } }
        if (rating != null && !verifiedAlternative && active.isNotEmpty()) {
            val next = active.minOf { it.nextReview }
            val routed = if (updated.schedulerVersion == "legacy") minOf(item.nextReview, next) else next
            updated = updated.copy(nextReview = if (effective == ReviewRating.AGAIN && item.nextReview > now) minOf(item.nextReview, routed) else routed)
            val core = active.filter { it.senseKey == "primary" && it.axis in setOf(VocabularySkillAxis.MEANING.name, VocabularySkillAxis.RETRIEVAL.name) }
            val mature = core.size == 2 && core.all { it.independentSuccesses >= 4 && now - it.firstReview >= 7 * Fsrs6.DAY_MS && Fsrs6.interval(it.stability, retention) >= 7 }
            if (!mature) updated = updated.copy(mastery = updated.mastery.coerceAtMost(69))
        }
        db.vocabularyDao().update(updated)
        dao.appendEvent(VocabularyReviewEvent(vocabularyId = item.id, senseKey = task.senseKey, senseId = task.senseId,
            word = item.word, axis = task.mode.skill.name, reviewedAt = now, category = category,
            rating = effective?.value, hintUsed = hintUsed, exactTarget = exact, answer = answer,
            prompt = task.prompt, expectedAnswer = task.expectedAnswer, prediction = prediction,
            desiredRetention = retention, source = source, elapsedDays = elapsed))
    }
}
