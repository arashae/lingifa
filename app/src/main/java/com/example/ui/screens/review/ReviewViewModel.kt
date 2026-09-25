package com.example.ui.screens.review

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.VocabularyItem
import com.example.data.repository.DailyStreakRepository
import com.example.data.repository.MistakeRepository
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.VocabularyRepository
import com.example.srs.ReviewRating
import com.example.vocab.VocabularySkillAxis
import com.example.vocab.VocabularyStudyPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ReviewExerciseType {
    FLASHCARD,
    MULTIPLE_CHOICE_EN_FA,
    MULTIPLE_CHOICE_FA_EN,
    CONTEXT_CLOZE,
    TYPE_WORD,
    LISTENING_CHOOSE
}

data class ReviewSessionUiState(
    val queue: List<VocabularyItem> = emptyList(),
    val currentIndex: Int = 0,
    val currentExerciseType: ReviewExerciseType = ReviewExerciseType.FLASHCARD,
    val contextPrompt: String = "",
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
    private val studyPreferences = application.getSharedPreferences(
        VocabularyStudyPolicy.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _uiState = MutableStateFlow(ReviewSessionUiState())
    val uiState: StateFlow<ReviewSessionUiState> = _uiState
    private var sessionStartMillis: Long = System.currentTimeMillis()

    init {
        startSession()
    }

    fun startSession(forceNewWordsOnly: Boolean = false) {
        viewModelScope.launch {
            val profile = profileRepo.getProfileSync()
            val userLevel = profile.currentLevel.ifEmpty { "B2" }
            val newWordLimit = studyPreferences
                .getInt(VocabularyStudyPolicy.KEY_DAILY_NEW_LIMIT, 15)
                .takeIf { it in VocabularyStudyPolicy.DAILY_NEW_LIMIT_OPTIONS }
                ?: 15

            val sessionItems = if (forceNewWordsOnly) {
                vocabRepo.getNewVocabulariesForLearning(userLevel, limit = newWordLimit)
            } else {
                val due = vocabRepo.getDueVocabulariesForReview(limit = 40).first().take(30)
                val dueIds = due.mapTo(hashSetOf()) { it.id }

                val studied = vocabRepo.getStudiedVocabulariesForReview(limit = 120)
                val weak = studied
                    .asSequence()
                    .filter { it.id !in dueIds && it.mastery < 50 }
                    .sortedWith(compareBy<VocabularyItem> { it.mastery }.thenBy { it.lastReview })
                    .take(10)
                    .toList()
                val usedIds = (due + weak).mapTo(hashSetOf()) { it.id }

                val newWords = vocabRepo.getNewVocabulariesForLearning(userLevel, limit = newWordLimit)
                    .filterNot { it.id in usedIds }

                val planned = (due + weak + newWords).distinctBy { it.id }
                if (planned.isNotEmpty()) planned
                else studied.take(10)
            }

            if (sessionItems.isNotEmpty()) {
                sessionStartMillis = System.currentTimeMillis()
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
     * Learn/Recall remains the main semantic signal. Once a word has been seen, contextual cloze
     * becomes a meaningful secondary probe. Exact spelling stays diagnostic and has its own
     * persisted mastery axis, so a typo never wipes semantic memory.
     */
    private suspend fun setupCurrentExercise(item: VocabularyItem) {
        val roll = Random.nextInt(100)
        val isUnseen = item.correctCount == 0 && item.incorrectCount == 0
        val cloze = if (isUnseen) null else VocabularyStudyPolicy.clozeSentence(item)

        val chosenType = when {
            isUnseen -> ReviewExerciseType.FLASHCARD
            item.correctCount <= 2 -> when {
                roll < 62 -> ReviewExerciseType.FLASHCARD
                cloze != null && roll < 76 -> ReviewExerciseType.CONTEXT_CLOZE
                roll < 86 -> ReviewExerciseType.LISTENING_CHOOSE
                roll < 95 -> ReviewExerciseType.MULTIPLE_CHOICE_EN_FA
                else -> ReviewExerciseType.MULTIPLE_CHOICE_FA_EN
            }
            else -> when {
                roll < 52 -> ReviewExerciseType.FLASHCARD
                cloze != null && roll < 70 -> ReviewExerciseType.CONTEXT_CLOZE
                roll < 80 -> ReviewExerciseType.LISTENING_CHOOSE
                roll < 89 -> ReviewExerciseType.MULTIPLE_CHOICE_EN_FA
                roll < 95 -> ReviewExerciseType.MULTIPLE_CHOICE_FA_EN
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
            ReviewExerciseType.MULTIPLE_CHOICE_FA_EN,
            ReviewExerciseType.CONTEXT_CLOZE -> {
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
            contextPrompt = if (chosenType == ReviewExerciseType.CONTEXT_CLOZE) cloze.orEmpty() else "",
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
            val queuedItem = _uiState.value.queue.getOrNull(_uiState.value.currentIndex) ?: return@launch
            val currentItem = vocabRepo.getByIdSync(queuedItem.id) ?: queuedItem
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
        val exerciseType = _uiState.value.currentExerciseType

        val isCorrect = when (exerciseType) {
            ReviewExerciseType.MULTIPLE_CHOICE_EN_FA,
            ReviewExerciseType.LISTENING_CHOOSE -> selectedText == currentItem.persianMeaning
            ReviewExerciseType.MULTIPLE_CHOICE_FA_EN,
            ReviewExerciseType.CONTEXT_CLOZE -> selectedText == currentItem.word
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
            val fresh = vocabRepo.getByIdSync(currentItem.id) ?: currentItem
            val itemForSemanticReview = if (exerciseType == ReviewExerciseType.CONTEXT_CLOZE) {
                val skillUpdated = VocabularyStudyPolicy.withSkillResult(
                    fresh,
                    VocabularySkillAxis.CONTEXT,
                    isCorrect
                )
                vocabRepo.update(skillUpdated)
                replaceQueuedItem(skillUpdated)
                skillUpdated
            } else {
                fresh
            }
            vocabRepo.recordReview(itemForSemanticReview, rating)

            if (!isCorrect) {
                val isContext = exerciseType == ReviewExerciseType.CONTEXT_CLOZE
                mistakeRepo.addMistake(
                    question = if (isContext) {
                        "کدام واژه جمله را کامل می‌کند؟ ${_uiState.value.contextPrompt}"
                    } else {
                        "معنی یا معادل '${currentItem.word}'"
                    },
                    myAnswer = selectedText,
                    correctAnswer = if (isContext) currentItem.word else currentItem.persianMeaning,
                    explanationFa = currentItem.englishDefinition.ifBlank { currentItem.examplePersian },
                    whyWrongFa = if (isContext) "واژه در بافت جمله درست بازیابی نشد" else "گزینه نادرست انتخاب شد",
                    concept = currentItem.word,
                    skillType = if (isContext) "VOCABULARY_CONTEXT" else "VOCABULARY"
                )
                requeueFailedCard(currentItem)
            }
        }
    }

    fun onTypedInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(typedInput = input)
    }

    /**
     * Spelling/production is diagnostic and persisted independently from semantic mastery.
     * After this feedback the learner still self-rates meaning recall, which controls SRS timing.
     */
    fun checkTypedAnswer() {
        val currentItem = _uiState.value.queue.getOrNull(_uiState.value.currentIndex) ?: return
        val answer = _uiState.value.typedInput.trim()
        val isCorrect = answer.equals(currentItem.word.trim(), ignoreCase = true)
        val skillUpdated = VocabularyStudyPolicy.withSkillResult(
            currentItem,
            VocabularySkillAxis.SPELLING,
            isCorrect
        )
        replaceQueuedItem(skillUpdated)
        _uiState.value = _uiState.value.copy(
            isTypedCorrect = isCorrect,
            isAnswerRevealed = true,
            lastWasSuccess = isCorrect
        )

        viewModelScope.launch {
            vocabRepo.update(skillUpdated)
            if (!isCorrect) {
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

    private fun replaceQueuedItem(updated: VocabularyItem) {
        val index = _uiState.value.currentIndex
        if (index !in _uiState.value.queue.indices) return
        val queue = _uiState.value.queue.toMutableList()
        queue[index] = updated
        _uiState.value = _uiState.value.copy(queue = queue)
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
                val elapsedMinutes = maxOf(1, ((System.currentTimeMillis() - sessionStartMillis) / 60000L).toInt())
                streakRepo.recordPracticeActivity(
                    itemsCount = newCompleted,
                    minutesSpent = elapsedMinutes,
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
