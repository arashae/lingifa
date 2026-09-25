package com.example.ui.screens.vocab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TtsManager
import com.example.data.ai.AiVocabCardGenerator
import com.example.data.local.AppDatabase
import com.example.data.model.AiVocabCardData
import com.example.data.model.IeltsFlashcard
import com.example.data.model.StreakInfo
import com.example.data.model.StreakUpdateResult
import com.example.data.model.VocabularyItem
import com.example.data.repository.DailyStreakRepository
import com.example.data.repository.IeltsFlashcardRepository
import com.example.data.repository.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiVocabCardUiState(
    val query: String = "Resilience",
    val isLoading: Boolean = false,
    val currentCard: AiVocabCardData? = null,
    val isUkAccent: Boolean = false,
    val speechRate: Float = 0.9f,
    val isPlayingAudio: Boolean = false,
    val isSaved: Boolean = false,
    val statusMessage: String? = null,
    val streakResult: StreakUpdateResult? = null
)

class AiVocabCardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val vocabRepository = VocabularyRepository(
        database.vocabularyDao(),
        database.vocabularyPackDao(),
        database.vocabularyPackItemDao()
    )
    private val ieltsRepository = IeltsFlashcardRepository(database.ieltsFlashcardDao())
    val streakRepository = DailyStreakRepository(
        database.dailyStreakDao(),
        database.userProfileDao()
    )

    private val _uiState = MutableStateFlow(AiVocabCardUiState())
    val uiState: StateFlow<AiVocabCardUiState> = _uiState.asStateFlow()

    val streakInfo: StateFlow<StreakInfo> = streakRepository.streakInfo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StreakInfo()
        )

    init {
        // Load initial card
        generateCard("Resilience")
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, isSaved = false) }
    }

    fun generateCard(word: String = _uiState.value.query) {
        if (word.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isSaved = false, statusMessage = null) }
            val card = AiVocabCardGenerator.generateVocabularyCard(word.trim())
            _uiState.update {
                it.copy(
                    query = word.trim(),
                    currentCard = card,
                    isLoading = false,
                    isSaved = false
                )
            }
        }
    }

    fun toggleAccent() {
        _uiState.update { it.copy(isUkAccent = !it.isUkAccent) }
    }

    fun setSpeechRate(rate: Float) {
        _uiState.update { it.copy(speechRate = rate) }
    }

    fun speakWord(tts: TtsManager) {
        val word = _uiState.value.currentCard?.word ?: return
        _uiState.update { it.copy(isPlayingAudio = true) }
        tts.speak(word, isUk = _uiState.value.isUkAccent, speechRate = _uiState.value.speechRate)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            _uiState.update { it.copy(isPlayingAudio = false) }
        }
    }

    fun speakExample(tts: TtsManager) {
        val example = _uiState.value.currentCard?.exampleSentenceEn ?: return
        _uiState.update { it.copy(isPlayingAudio = true) }
        tts.speak(example, isUk = _uiState.value.isUkAccent, speechRate = _uiState.value.speechRate)
        viewModelScope.launch {
            kotlinx.coroutines.delay(2800)
            _uiState.update { it.copy(isPlayingAudio = false) }
        }
    }

    fun saveCardToLibrary() {
        val card = _uiState.value.currentCard ?: return
        if (!card.isValid) return
        viewModelScope.launch {
            // 1. Save to main vocabulary repository
            val vocabItem = VocabularyItem(
                word = card.word,
                normalizedWord = card.word.lowercase().trim(),
                ipa = card.phonetic,
                persianMeaning = card.persianTranslation,
                englishDefinition = card.englishDefinition,
                partOfSpeech = card.partOfSpeech,
                example = card.exampleSentenceEn,
                examplePersian = card.exampleSentenceFa,
                cefrLevel = card.cefrLevel,
                collocations = card.collocations,
                synonyms = card.synonyms,
                antonyms = card.antonyms,
                commonMistakes = card.persianCommonMistake,
                ieltsRelevance = card.ieltsTipFa,
                source = "AI Generator"
            )
            val insertedId = vocabRepository.insert(vocabItem)

            // Attach membership to the corresponding CEFR pack
            val cefrPackId = "pack_cefr_${card.cefrLevel.lowercase(java.util.Locale.US)}"
            database.vocabularyPackItemDao().insert(
                com.example.data.model.VocabularyPackItem(
                    packId = cefrPackId,
                    vocabularyId = insertedId
                )
            )

            // 2. Also insert as IELTS Flashcard into deck 1 if available
            val ieltsCard = IeltsFlashcard(
                deckId = 1L,
                word = card.word,
                normalizedWord = card.word.lowercase().trim(),
                phonetic = card.phonetic,
                partOfSpeech = card.partOfSpeech,
                persianTranslation = card.persianTranslation,
                englishDefinition = card.englishDefinition,
                exampleSentenceEn = card.exampleSentenceEn,
                exampleSentenceFa = card.exampleSentenceFa,
                collocations = card.collocations,
                synonyms = card.synonyms,
                antonyms = card.antonyms,
                targetBand = if (card.cefrLevel == "C1" || card.cefrLevel == "C2") "8.0" else "7.5",
                usageNoteFa = card.ieltsTipFa,
                commonMistakesFa = card.persianCommonMistake
            )
            ieltsRepository.insertCard(ieltsCard)

            // 3. Record gamified streak activity in Room
            val streakResult = streakRepository.recordPracticeActivity(
                itemsCount = 1,
                minutesSpent = 3,
                xpEarned = 20,
                activityType = "AI_CARD"
            )

            _uiState.update {
                it.copy(
                    isSaved = true,
                    statusMessage = "'${card.word}' saved to your vocabulary and flashcards! (+20 XP)",
                    streakResult = streakResult
                )
            }
        }
    }

    fun dismissStreakModal() {
        _uiState.update { it.copy(streakResult = null) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
