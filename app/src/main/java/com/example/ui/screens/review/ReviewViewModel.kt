package com.example.ui.screens.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.VocabularyItem
import com.example.data.repository.DailyStreakRepository
import com.example.data.repository.MistakeRepository
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.VocabularyRepository
import com.example.srs.ReviewRating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReviewExerciseType {
    FLASHCARD,
    MULTIPLE_CHOICE_EN_FA,
    MULTIPLE_CHOICE_FA_EN,
    TYPE_WORD,
    LISTENING_CHOOSE
}

data class ReviewSessionUiState(
    val queue: List<VocabularyItem> = emptyList(),
    val currentIndex: Int = 0,
    val currentExerciseType: ReviewExerciseType = ReviewExerciseType.FLASHCARD,
    val isAnswerRevealed: Boolean = false,
    val multipleChoiceOptions: List<String> = emptyList(),
    val selectedOptionIndex: Int? = null,
    val isOptionAnswerChecked: Boolean = false,
    val typedInput: String = "",
    val isTypedCorrect: Boolean? = null,
    val completedCount: Int = 0,
    val sessionTotal: Int = 0,
    val isSessionFinished: Boolean = false,
    val xpEarned: Int = 0
)

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val vocabRepo = VocabularyRepository(db.vocabularyDao(), db.vocabularyPackDao())
    private val mistakeRepo = MistakeRepository(db.mistakeDao())
    private val profileRepo = UserProfileRepository(db.userProfileDao())
    private val streakRepo = DailyStreakRepository(db.dailyStreakDao(), db.userProfileDao())

    private val _uiState = MutableStateFlow(ReviewSessionUiState())
    val uiState: StateFlow<ReviewSessionUiState> = _uiState

    init {
        startSession()
    }

    fun startSession() {
        viewModelScope.launch {
            val due = vocabRepo.getDueVocabulariesForReview(limit = 30).first()
            val sessionItems = if (due.isNotEmpty()) {
                due.take(15)
            } else {
                vocabRepo.getRandomVocabularies(10)
            }

            if (sessionItems.isNotEmpty()) {
                _uiState.value = ReviewSessionUiState(
                    queue = sessionItems,
                    currentIndex = 0,
                    sessionTotal = sessionItems.size,
                    currentExerciseType = ReviewExerciseType.FLASHCARD
                )
                setupCurrentExercise(sessionItems[0])
            } else {
                _uiState.value = ReviewSessionUiState(isSessionFinished = true)
            }
        }
    }

    private suspend fun setupCurrentExercise(item: VocabularyItem) {
        val exerciseTypes = listOf(
            ReviewExerciseType.FLASHCARD,
            ReviewExerciseType.MULTIPLE_CHOICE_EN_FA,
            ReviewExerciseType.MULTIPLE_CHOICE_FA_EN,
            ReviewExerciseType.LISTENING_CHOOSE
        )
        val chosenType = exerciseTypes.random()

        val options = when (chosenType) {
            ReviewExerciseType.MULTIPLE_CHOICE_EN_FA, ReviewExerciseType.LISTENING_CHOICE -> {
                val distractors = vocabRepo.getRandomVocabularies(8)
                    .filter { it.word != item.word }
                    .take(3)
                    .map { it.persianMeaning }
                (distractors + item.persianMeaning).shuffled()
            }
            ReviewExerciseType.MULTIPLE_CHOICE_FA_EN -> {
                val distractors = vocabRepo.getRandomVocabularies(8)
                    .filter { it.word != item.word }
                    .take(3)
                    .map { it.word }
                (distractors + item.word).shuffled()
            }
            else -> emptyList()
        }

        _uiState.value = _uiState.value.copy(
            currentExerciseType = chosenType,
            isAnswerRevealed = false,
            multipleChoiceOptions = options,
            selectedOptionIndex = null,
            isOptionAnswerChecked = false,
            typedInput = "",
            isTypedCorrect = null
        )
    }

    fun revealAnswer() {
        _uiState.value = _uiState.value.copy(isAnswerRevealed = true)
    }

    fun submitRating(rating: ReviewRating) {
        viewModelScope.launch {
            val currentItem = _uiState.value.queue.getOrNull(_uiState.value.currentIndex) ?: return@launch
            vocabRepo.recordReview(currentItem, rating)

            // If AGAIN, add to mistake notebook
            if (rating == ReviewRating.AGAIN) {
                mistakeRepo.addMistake(
                    question = "معنی واژه '${currentItem.word}' چیست؟",
                    myAnswer = "پاسخ نادرست / نیاز به مرور",
                    correctAnswer = "${currentItem.persianMeaning} (${currentItem.ipa})",
                    explanationFa = currentItem.examplePersian.ifEmpty { "معنی: ${currentItem.persianMeaning}" },
                    whyWrongFa = "فراموشی در جلسه مرور روزانه",
                    concept = currentItem.word,
                    skillType = "VOCABULARY"
                )
            }

            advanceQueue(rating != ReviewRating.AGAIN)
        }
    }

    fun selectMultipleChoiceOption(index: Int) {
        if (_uiState.value.isOptionAnswerChecked) return
        val currentItem = _uiState.value.queue.getOrNull(_uiState.value.currentIndex) ?: return
        val selectedText = _uiState.value.multipleChoiceOptions.getOrNull(index) ?: return

        val isCorrect = when (_uiState.value.currentExerciseType) {
            ReviewExerciseType.MULTIPLE_CHOICE_EN_FA, ReviewExerciseType.LISTENING_CHOOSE ->
                selectedText == currentItem.persianMeaning
            ReviewExerciseType.MULTIPLE_CHOICE_FA_EN ->
                selectedText == currentItem.word
            else -> false
        }

        _uiState.value = _uiState.value.copy(
            selectedOptionIndex = index,
            isOptionAnswerChecked = true,
            isAnswerRevealed = true
        )

        val rating = if (isCorrect) ReviewRating.GOOD else ReviewRating.AGAIN
        viewModelScope.launch {
            vocabRepo.recordReview(currentItem, rating)
            if (!isCorrect) {
                mistakeRepo.addMistake(
                    question = "معنی '${currentItem.word}'",
                    myAnswer = selectedText,
                    correctAnswer = currentItem.persianMeaning,
                    explanationFa = currentItem.examplePersian,
                    skillType = "VOCABULARY"
                )
            }
        }
    }

    fun onTypedInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(typedInput = input)
    }

    fun checkTypedAnswer() {
        val currentItem = _uiState.value.queue.getOrNull(_uiState.value.currentIndex) ?: return
        val isCorrect = _uiState.value.typedInput.trim().equals(currentItem.word.trim(), ignoreCase = true)
        _uiState.value = _uiState.value.copy(
            isTypedCorrect = isCorrect,
            isAnswerRevealed = true
        )

        val rating = if (isCorrect) ReviewRating.GOOD else ReviewRating.AGAIN
        viewModelScope.launch {
            vocabRepo.recordReview(currentItem, rating)
        }
    }

    fun advanceQueue(wasSuccess: Boolean = true) {
        viewModelScope.launch {
            val nextIndex = _uiState.value.currentIndex + 1
            val newCompleted = _uiState.value.completedCount + 1
            val newXp = _uiState.value.xpEarned + (if (wasSuccess) 10 else 2)

            if (nextIndex < _uiState.value.queue.size) {
                _uiState.value = _uiState.value.copy(
                    currentIndex = nextIndex,
                    completedCount = newCompleted,
                    xpEarned = newXp
                )
                setupCurrentExercise(_uiState.value.queue[nextIndex])
            } else {
                streakRepo.recordPracticeActivity(
                    itemsCount = newCompleted,
                    minutesSpent = 8,
                    xpEarned = newXp,
                    activityType = "SRS_REVIEW"
                )
                _uiState.value = _uiState.value.copy(
                    completedCount = newCompleted,
                    xpEarned = newXp,
                    isSessionFinished = true
                )
            }
        }
    }
}
