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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ReviewSessionUiState(
    val queue: List<VocabularyItem> = emptyList(),
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val completedCount: Int = 0,
    val sessionTotal: Int = 0,
    val isSessionFinished: Boolean = false,
    val isSubmitting: Boolean = false,
    val xpEarned: Int = 0
)

/**
 * Vocabulary-only SRS review.
 *
 * Important contract:
 * - Review never introduces a new vocabulary item.
 * - Only words that the learner has already judged at least once can enter the queue.
 * - Only currently-due words are shown; weak-but-not-due words are not pulled forward.
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
            val dueItems = vocabRepo
                .getDueVocabulariesForReview(limit = 50)
                .first()
                .filter { it.correctCount > 0 || it.incorrectCount > 0 }
                .distinctBy { it.id }

            sessionStartMillis = System.currentTimeMillis()
            _uiState.value = if (dueItems.isEmpty()) {
                ReviewSessionUiState(
                    sessionTotal = 0,
                    isSessionFinished = true
                )
            } else {
                ReviewSessionUiState(
                    queue = dueItems,
                    currentIndex = 0,
                    sessionTotal = dueItems.size
                )
            }
        }
    }

    fun revealAnswer() {
        if (_uiState.value.isSubmitting) return
        _uiState.value = _uiState.value.copy(isAnswerRevealed = true)
    }

    fun submitRating(rating: ReviewRating) {
        if (_uiState.value.isSubmitting) return

        val snapshot = _uiState.value
        val queuedItem = snapshot.queue.getOrNull(snapshot.currentIndex) ?: return
        _uiState.value = snapshot.copy(isSubmitting = true)

        viewModelScope.launch {
            val currentItem = vocabRepo.getByIdSync(queuedItem.id) ?: queuedItem
            vocabRepo.recordReview(currentItem, rating)

            val isSuccess = rating != ReviewRating.AGAIN
            if (!isSuccess) {
                mistakeRepo.addMistake(
                    question = "معنی یا کاربرد واژه '${currentItem.word}' چیست؟",
                    myAnswer = "یادم نبود / نیاز به مرور",
                    correctAnswer = buildString {
                        append(currentItem.englishDefinition.ifBlank { currentItem.persianMeaning })
                        if (currentItem.persianMeaning.isNotBlank()) {
                            append(" — ${currentItem.persianMeaning}")
                        }
                    },
                    explanationFa = currentItem.examplePersian.ifEmpty { currentItem.example },
                    whyWrongFa = "در مرور واژگان به یاد نیامد؛ این واژه ۳۰ دقیقه بعد دوباره موعد مرور می‌شود.",
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
