package com.example.ui.screens.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.VocabularyItem
import com.example.data.repository.VocabularyRepository
import com.example.srs.ReviewRating
import kotlinx.coroutines.CancellationException
import com.example.vocab.LexicalBank
import com.example.vocab.ReviewEvidence
import com.example.vocab.ReviewEvidenceSummary
import com.example.data.model.VocabularyReviewSettings
import com.example.data.model.VocabularySense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ReviewSessionUiState(
    val queue: List<ReviewTask> = emptyList(),
    val answerText: String = "",
    val answerChecked: Boolean = false,
    val hintUsed: Boolean = false,
    val desiredRetention: Double = 0.9,
    val evidence: ReviewEvidenceSummary = ReviewEvidenceSummary(0, null, null),
    val typedAnswerCorrect: Boolean? = null,
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val completedCount: Int = 0,
    val sessionTotal: Int = 0,
    val isSessionFinished: Boolean = false,
    val isSubmitting: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val xpEarned: Int = 0
)

/**
 * Vocabulary-only review across the learner's complete vocabulary history.
 *
 * Important contract:
 * - Review never introduces a new vocabulary item.
 * - Only words that the learner has already judged at least once can enter the queue.
 * - Currently-due SRS words always come first.
 * - If fewer than a full session are due, Review is filled with weak/recent/rotating studied words
 *   from every pack so the learner can practice on demand instead of seeing an empty session.
 * - Early successful practice leaves the spaced schedule unchanged.
 * - AGAIN returns within 30 minutes and is not immediately
 *   appended to the current session.
 */
class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val vocabRepo = VocabularyRepository(
        db.vocabularyDao(),
        db.vocabularyPackDao(),
        db.vocabularyPackItemDao()
    )
    private val submissionStore = ReviewSubmissionStore(db)

    private val _uiState = MutableStateFlow(ReviewSessionUiState())
    val uiState: StateFlow<ReviewSessionUiState> = _uiState

    private var recordedMinutes = 0
    private var sessionStartMillis: Long = System.currentTimeMillis()

    init {
        startSession()
    }

    fun startSession() {
        if (_uiState.value.isLoading || _uiState.value.isSubmitting) return
        _uiState.value = ReviewSessionUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val dueItems = vocabRepo
                    .getDueVocabulariesForReview(limit = ReviewQueuePolicy.CANDIDATE_POOL_LIMIT, currentTime = now)
                    .first()
                val studiedItems = vocabRepo.getStudiedVocabulariesForReview(
                    limit = ReviewQueuePolicy.CANDIDATE_POOL_LIMIT
                )
                val queue = ReviewQueuePolicy.buildQueue(
                    dueItems = dueItems,
                    studiedItems = studiedItems,
                    now = now
                )

                val settings = db.reviewLearningDao().settings() ?: VocabularyReviewSettings()
                val evidence = ReviewEvidence.summarize(db.reviewLearningDao().recentEvents(),
                    db.reviewLearningDao().initializedSkills(),
                    db.reviewLearningDao().eventCountSince(now - 7 * com.example.srs.Fsrs6.DAY_MS))
                val skills = db.reviewLearningDao().skillsFor(queue.map { it.id }).groupBy { it.vocabularyId }
                val knownWords = db.vocabularyDao().getByNormalizedWords(LexicalBank.words.toList())
                    .filter { it.correctCount > 0 }.map { it.word.lowercase(java.util.Locale.US) }.toSet()
                val tasks = queue.map { item ->
                    val task = ReviewTask.forItem(item, skills[item.id].orEmpty(), knownWords, now)
                    val lexical = task.lexicalRelation?.sense(item.word)
                    if (lexical != null) {
                        val senses = db.vocabularySenseDao().getSensesForWordSync(item.id)
                        val id = senses.firstOrNull { it.senseIndex == lexical.index }?.id
                            ?: db.vocabularySenseDao().insert(VocabularySense(vocabularyId = item.id,
                                senseIndex = lexical.index, partOfSpeech = lexical.partOfSpeech,
                                cefrLevel = item.cefrLevel, englishDefinition = lexical.definition,
                                persianMeaning = lexical.meaningFa, exampleSentence = lexical.example, isPrimary = false))
                        task.copy(senseId = id)
                    } else task.copy(senseId = db.vocabularySenseDao().getSensesForWordSync(item.id)
                        .firstOrNull { it.isPrimary && it.englishDefinition == item.englishDefinition }?.id)
                }
                sessionStartMillis = now
                recordedMinutes = 0
                _uiState.value = if (queue.isEmpty()) {
                    ReviewSessionUiState(
                        desiredRetention = settings.desiredRetention,
                        evidence = evidence,
                        sessionTotal = 0,
                        isSessionFinished = true
                    )
                } else {
                    ReviewSessionUiState(
                        queue = tasks,
                        desiredRetention = settings.desiredRetention,
                        evidence = evidence,
                        currentIndex = 0,
                        sessionTotal = queue.size
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ReviewSessionUiState(errorMessage = "Could not load review. Please try again.")
            }
        }
    }

    fun updateAnswer(answer: String) {
        val state = _uiState.value
        if (state.isSubmitting || state.answerChecked) return
        _uiState.value = state.copy(answerText = answer)
    }

    fun showHint() {
        val state = _uiState.value
        if (!state.answerChecked && !state.isSubmitting) _uiState.value = state.copy(hintUsed = true)
    }

    fun setRetention(retention: Double) {
        require(retention in setOf(0.85, 0.9, 0.95))
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            try {
                db.reviewLearningDao().saveSettings(VocabularyReviewSettings(desiredRetention = retention))
                _uiState.value = _uiState.value.copy(desiredRetention = retention)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.value = _uiState.value.copy(errorMessage = "Could not save the review target. Please retry.") }
        }
    }

    fun checkAnswer() {
        val state = _uiState.value
        if (state.isSubmitting || state.answerChecked || state.answerText.isBlank()) return
        val task = state.queue.getOrNull(state.currentIndex) ?: return
        _uiState.value = state.copy(
            answerChecked = true,
            typedAnswerCorrect = task.isCorrect(state.answerText),
            isAnswerRevealed = true
        )
    }

    fun revealAnswer() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val task = state.queue.getOrNull(state.currentIndex) ?: return
        if (task.mode.requiresTypedAnswer && !state.answerChecked) return
        _uiState.value = state.copy(isAnswerRevealed = true)
    }

    fun dontKnow() {
        val state = _uiState.value
        if (state.isSubmitting) return
        _uiState.value = state.copy(answerChecked = true, typedAnswerCorrect = false, isAnswerRevealed = true)
    }

    fun submitRating(rating: ReviewRating) = submitResult(rating)

    // An alternative can be valid without proving recall of the intended headword.
    // Leave its grade and schedule unchanged; the learner still sees target feedback.
    fun submitAlternative() {
        val state = _uiState.value
        val task = state.queue.getOrNull(state.currentIndex) ?: return
        if (!state.answerChecked || state.typedAnswerCorrect != false || state.answerText.isBlank() ||
            !task.mode.requiresTypedAnswer) return
        submitResult(null)
    }

    private fun submitResult(rating: ReviewRating?) {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting || !snapshot.isAnswerRevealed) return
        val task = snapshot.queue.getOrNull(snapshot.currentIndex) ?: return
        if (task.mode.requiresTypedAnswer &&
            (!snapshot.answerChecked || (snapshot.typedAnswerCorrect != true && rating != null && rating != ReviewRating.AGAIN))
        ) return
        _uiState.value = snapshot.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val success = rating != null && rating != ReviewRating.AGAIN
                val xp = if (success) 10 else 2
                val elapsedMinutes = maxOf(1, ((now - sessionStartMillis) / 60_000L).toInt())
                submissionStore.save(
                    task, snapshot.answerText, rating, now,
                    minutesSpent = (elapsedMinutes - recordedMinutes).coerceAtLeast(0), xp = xp, hintUsed = snapshot.hintUsed
                )
                recordedMinutes = elapsedMinutes
                val nextIndex = snapshot.currentIndex + 1
                _uiState.value = snapshot.copy(
                    currentIndex = nextIndex,
                    isAnswerRevealed = false,
                    answerText = "",
                    answerChecked = false,
                    hintUsed = false,
                    typedAnswerCorrect = null,
                    completedCount = snapshot.completedCount + 1,
                    xpEarned = snapshot.xpEarned + xp,
                    isSubmitting = false,
                    isSessionFinished = nextIndex >= snapshot.queue.size,
                    errorMessage = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = snapshot.copy(
                    isSubmitting = false,
                    errorMessage = "Could not save this answer. Retry, or restart if the word was deleted."
                )
            }
        }
    }
}
