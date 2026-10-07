package com.example.ui.screens.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.VocabularyItem
import com.example.data.repository.DailyStreakRepository
import com.example.data.repository.MistakeRepository
import com.example.data.repository.VocabularyRepository
import com.example.srs.ReviewRating
import com.example.vocab.VocabularyStudyPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ReviewSessionUiState(
    val queue: List<ReviewTask> = emptyList(),
    val answerText: String = "",
    val answerChecked: Boolean = false,
    val typedAnswerCorrect: Boolean? = null,
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val completedCount: Int = 0,
    val sessionTotal: Int = 0,
    val isSessionFinished: Boolean = false,
    val isSubmitting: Boolean = false,
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
 * - AGAIN is persisted with the SRS intra-day delay (30 minutes) and is not immediately
 *   appended to the current session.
 */
class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val vocabRepo = VocabularyRepository(
        db.vocabularyDao(),
        db.vocabularyPackDao(),
        db.vocabularyPackItemDao()
    )
    private val mistakeRepo = MistakeRepository(db.mistakeDao())
    private val streakRepo = DailyStreakRepository(db.dailyStreakDao(), db.userProfileDao())

    private val _uiState = MutableStateFlow(ReviewSessionUiState())
    val uiState: StateFlow<ReviewSessionUiState> = _uiState

    private var sessionStartMillis: Long = System.currentTimeMillis()

    init {
        startSession()
    }

    fun startSession() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dueItems = vocabRepo
                .getDueVocabulariesForReview(limit = ReviewQueuePolicy.DEFAULT_SESSION_LIMIT, currentTime = now)
                .first()
            val studiedItems = vocabRepo.getStudiedVocabulariesForReview(
                limit = ReviewQueuePolicy.CANDIDATE_POOL_LIMIT
            )
            val queue = ReviewQueuePolicy.buildQueue(
                dueItems = dueItems,
                studiedItems = studiedItems,
                now = now
            )

            sessionStartMillis = now
            _uiState.value = if (queue.isEmpty()) {
                ReviewSessionUiState(
                    sessionTotal = 0,
                    isSessionFinished = true
                )
            } else {
                ReviewSessionUiState(
                    queue = queue.map(ReviewTask::forItem),
                    currentIndex = 0,
                    sessionTotal = queue.size
                )
            }
        }
    }

    fun updateAnswer(answer: String) {
        val state = _uiState.value
        if (state.isSubmitting || state.answerChecked) return
        _uiState.value = state.copy(answerText = answer)
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

    fun submitRating(rating: ReviewRating) {
        if (_uiState.value.isSubmitting) return

        val snapshot = _uiState.value
        val task = snapshot.queue.getOrNull(snapshot.currentIndex) ?: return
        if (task.mode.requiresTypedAnswer &&
            (!snapshot.answerChecked || (snapshot.typedAnswerCorrect != true && rating != ReviewRating.AGAIN))
        ) return
        val queuedItem = task.item
        _uiState.value = snapshot.copy(isSubmitting = true)

        viewModelScope.launch {
            val currentItem = vocabRepo.getByIdSync(queuedItem.id) ?: queuedItem
            val skillUpdatedItem = VocabularyStudyPolicy.withSkillResult(
                currentItem,
                task.mode.skill,
                success = if (task.mode.requiresTypedAnswer) snapshot.typedAnswerCorrect == true else rating != ReviewRating.AGAIN
            )
            vocabRepo.recordReview(skillUpdatedItem, rating)

            val isSuccess = rating != ReviewRating.AGAIN
            if (!isSuccess) {
                mistakeRepo.addMistake(
                    question = task.prompt,
                    myAnswer = snapshot.answerText.ifBlank { "I could not recall it / needs review" },
                    correctAnswer = task.expectedAnswer + if (currentItem.persianMeaning.isNotBlank()) {
                        " — ${currentItem.persianMeaning}"
                    } else {
                        ""
                    },
                    explanationFa = currentItem.examplePersian.ifEmpty { currentItem.example },
                    whyWrongFa = if (snapshot.typedAnswerCorrect == false) {
                        "پاسخ ثبت‌شده با جواب مورد انتظار یکی نبود؛ این واژه ۳۰ دقیقه دیگر دوباره مرور می‌شود."
                    } else {
                        "Could not recall it during vocabulary review; this word is due again in 30 minutes."
                    },
                    concept = currentItem.word,
                    skillType = "VOCABULARY"
                )
            }

            advanceAfterRating(isSuccess)
        }
    }

    private suspend fun advanceAfterRating(wasSuccess: Boolean) {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        val newCompleted = state.completedCount + 1
        val newXp = state.xpEarned + if (wasSuccess) 10 else 2

        if (nextIndex < state.queue.size) {
            _uiState.value = state.copy(
                currentIndex = nextIndex,
                isAnswerRevealed = false,
                answerText = "",
                answerChecked = false,
                typedAnswerCorrect = null,
                completedCount = newCompleted,
                xpEarned = newXp,
                isSubmitting = false
            )
            return
        }

        val elapsedMinutes = maxOf(
            1,
            ((System.currentTimeMillis() - sessionStartMillis) / 60_000L).toInt()
        )
        streakRepo.recordPracticeActivity(
            itemsCount = newCompleted,
            minutesSpent = elapsedMinutes,
            xpEarned = newXp,
            activityType = "VOCABULARY_SRS_REVIEW"
        )

        _uiState.value = state.copy(
            completedCount = newCompleted,
            xpEarned = newXp,
            isSubmitting = false,
            isSessionFinished = true
        )
    }
}
