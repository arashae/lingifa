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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

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
    val lastWasSuccess: Boolean? = null,
    val completedCount: Int = 0,
    val sessionTotal: Int = 0,
    val isSessionFinished: Boolean = false,
    val xpEarned: Int = 0
)

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val vocabRepo = VocabularyRepository(db.vocabularyDao(), db.vocabularyPackDao(), db.vocabularyPackItemDao())
    private val mistakeRepo = MistakeRepository(db.mistakeDao())
    private val profileRepo = UserProfileRepository(db.userProfileDao())
    private val streakRepo = DailyStreakRepository(db.dailyStreakDao(), db.userProfileDao())

    private val _uiState = MutableStateFlow(ReviewSessionUiState())
    val uiState: StateFlow<ReviewSessionUiState> = _uiState

    init {
        startSession()
    }

    fun startSession(forceNewWordsOnly: Boolean = false) {
        viewModelScope.launch {
            val profile = profileRepo.getProfileSync()
            val userLevel = profile.currentLevel.ifEmpty { "B2" }

            val sessionItems = if (forceNewWordsOnly) {
                vocabRepo.getNewVocabulariesForLearning(userLevel, limit = 15)
            } else {
                val due = vocabRepo.getDueVocabulariesForReview(limit = 30).first()
                if (due.isNotEmpty()) {
                    due.take(15)
                } else {
                    val newWords = vocabRepo.getNewVocabulariesForLearning(userLevel, limit = 10)
                    if (newWords.isNotEmpty()) newWords
                    else vocabRepo.getStudiedVocabulariesForReview(limit = 10)
                }
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

    /**
     * Retrieval-first review policy.
     *
     * The main learning signal is silent/mental recall followed by reveal and self-rating.
     * Recognition, listening and spelling are useful secondary probes, but they should not
     * dominate scheduling. In particular, exact typing is kept deliberately rare so a learner
     * is not marked as having forgotten a concept merely because a synonym came to mind or the
     * spelling was imperfect.
     */
    private suspend fun setupCurrentExercise(item: VocabularyItem) {
        val roll = Random.nextInt(100)
        val chosenType = when {
            item.correctCount == 0 && item.incorrectCount == 0 -> ReviewExerciseType.FLASHCARD
            item.correctCount <= 2 -> when {
                roll < 72 -> ReviewExerciseType.FLASHCARD
                roll < 84 -> ReviewExerciseType.LISTENING_CHOOSE
                roll < 94 -> ReviewExerciseType.MULTIPLE_CHOICE_EN_FA
                else -> ReviewExerciseType.MULTIPLE_CHOICE_FA_EN
            }
            else -> when {
                roll < 68 -> ReviewExerciseType.FLASHCARD
                roll < 80 -> ReviewExerciseType.LISTENING_CHOOSE
                roll < 90 -> ReviewExerciseType.MULTIPLE_CHOICE_EN_FA
                roll < 96 -> ReviewExerciseType.MULTIPLE_CHOICE_FA_EN
                else -> ReviewExerciseType.TYPE_WORD
            }
        }

        val options = when (chosenType) {
            ReviewExerciseType.MULTIPLE_CHOICE_EN_FA,
            ReviewExerciseType.LISTENING_CHOOSE -> {
                val distractors = vocabRepo.getDistractors(
                    level = item.cefrLevel,
                    partOfSpeech = item.partOfSpeech,
                    excludeWord = item.word,
                    limit = 3
                ).map { it.persianMeaning }
                (distractors + item.persianMeaning).distinct().shuffled()
            }
            ReviewExerciseType.MULTIPLE_CHOICE_FA_EN -> {
                val distractors = vocabRepo.getDistractors(
                    level = item.cefrLevel,
                    partOfSpeech = item.partOfSpeech,
                    excludeWord = item.word,
                    limit = 3
                ).map { it.word }
                (distractors + item.word).distinct().shuffled()
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
            isTypedCorrect = null,
            lastWasSuccess = null
        )
    }

    fun revealAnswer() {
        _uiState.value = _uiState.value.copy(isAnswerRevealed = true)
    }

    fun submitRating(rating: ReviewRating) {
        viewModelScope.launch {
            val currentItem = _uiState.value.queue.getOrNull(_uiState.value.currentIndex) ?: return@launch
            vocabRepo.recordReview(currentItem, rating)

            val isSuccess = rating != ReviewRating.AGAIN
            if (!isSuccess) {
                mistakeRepo.addMistake(
                    question = "معنی یا کاربرد واژه '${currentItem.word}' چیست؟",
                    myAnswer = "یادم نبود / نیاز به مرور",
                    correctAnswer = buildString {
                        append(currentItem.englishDefinition.ifBlank { currentItem.persianMeaning })
                        if (currentItem.persianMeaning.isNotBlank()) append(" — ${currentItem.persianMeaning}")
                    },
                    explanationFa = currentItem.examplePersian.ifEmpty { currentItem.example },
                    whyWrongFa = "در بازیابی آزادِ جلسه مرور به یاد نیامد",
                    concept = currentItem.word,
                    skillType = "VOCABULARY"
                )
                requeueFailedCard(currentItem)
            }

            advanceQueue(isSuccess)
        }
    }

    fun selectMultipleChoiceOption(index: Int) {
        if (_uiState.value.isOptionAnswerChecked) return
        val currentItem = _uiState.value.queue.getOrNull(_uiState.value.currentIndex) ?: return
        val selectedText = _uiState.value.multipleChoiceOptions.getOrNull(index) ?: return

        val isCorrect = when (_uiState.value.currentExerciseType) {
            ReviewExerciseType.MULTIPLE_CHOICE_EN_FA,
            ReviewExerciseType.LISTENING_CHOOSE -> selectedText == currentItem.persianMeaning
            ReviewExerciseType.MULTIPLE_CHOICE_FA_EN -> selectedText == currentItem.word
            else -> false
        }

        _uiState.value = _uiState.value.copy(
            selectedOptionIndex = index,
            isOptionAnswerChecked = true,
            isAnswerRevealed = true,
            lastWasSuccess = isCorrect
        )

        val rating = if (isCorrect) ReviewRating.GOOD else ReviewRating.AGAIN
        viewModelScope.launch {
            vocabRepo.recordReview(currentItem, rating)
            if (!isCorrect) {
                mistakeRepo.addMistake(
                    question = "معنی یا معادل '${currentItem.word}'",
                    myAnswer = selectedText,
                    correctAnswer = currentItem.persianMeaning,
                    explanationFa = currentItem.englishDefinition.ifBlank { currentItem.examplePersian },
                    concept = currentItem.word,
                    skillType = "VOCABULARY"
                )
                requeueFailedCard(currentItem)
            }
        }
    }

    fun onTypedInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(typedInput = input)
    }

    /**
     * Spelling/production is diagnostic only. Checking the exact spelling does not change the
     * SRS schedule by itself; after feedback the learner self-rates the underlying memory.
     */
    fun checkTypedAnswer() {
        val currentItem = _uiState.value.queue.getOrNull(_uiState.value.currentIndex) ?: return
        val answer = _uiState.value.typedInput.trim()
        val isCorrect = answer.equals(currentItem.word.trim(), ignoreCase = true)
        _uiState.value = _uiState.value.copy(
            isTypedCorrect = isCorrect,
            isAnswerRevealed = true,
            lastWasSuccess = isCorrect
        )

        if (!isCorrect) {
            viewModelScope.launch {
                mistakeRepo.addMistake(
                    question = "تمرین املا/تولید برای «${currentItem.persianMeaning}»",
                    myAnswer = answer,
                    correctAnswer = currentItem.word,
                    explanationFa = currentItem.example.ifEmpty { "واژه صحیح: ${currentItem.word}" },
                    whyWrongFa = "این خطا جدا از دانستن مفهوم ثبت شده و به‌تنهایی حافظه معنایی را صفر نمی‌کند.",
                    concept = currentItem.word,
                    skillType = "VOCABULARY_SPELLING"
                )
            }
        }
    }

    private fun requeueFailedCard(item: VocabularyItem) {
        val alreadyRequeued = _uiState.value.queue
            .drop(_uiState.value.currentIndex + 1)
            .any { it.id == item.id }
        if (alreadyRequeued) return

        val updatedQueue = _uiState.value.queue.toMutableList().apply { add(item) }
        _uiState.value = _uiState.value.copy(
            queue = updatedQueue,
            sessionTotal = updatedQueue.size
        )
    }

    fun advanceQueue(explicitSuccess: Boolean? = null) {
        viewModelScope.launch {
            val wasSuccess = explicitSuccess ?: _uiState.value.lastWasSuccess ?: true
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
