package com.example.ui.screens.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.GrammarTopic
import com.example.data.model.ListeningExercise
import com.example.data.model.ReadingPassage
import com.example.data.model.VocabularyItem
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.VocabularyRepository
import com.example.data.seed.GrammarSeed
import com.example.data.seed.ReadingListeningSeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LearnUiState(
    val grammarTopics: List<GrammarTopic> = emptyList(),
    val readingPassages: List<ReadingPassage> = emptyList(),
    val listeningExercises: List<ListeningExercise> = emptyList(),
    val selectedCategory: String = "گرامر", // "گرامر", "Reading", "Listening"
    val popupWordDetail: VocabularyItem? = null,
    val isWordSavedStatus: String? = null
)

class LearnViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val vocabRepo = VocabularyRepository(db.vocabularyDao(), db.vocabularyPackDao())
    private val profileRepo = UserProfileRepository(db.userProfileDao())

    private val _uiState = MutableStateFlow(
        LearnUiState(
            grammarTopics = GrammarSeed.getTopics(),
            readingPassages = ReadingListeningSeed.getReadingPassages(),
            listeningExercises = ReadingListeningSeed.getListeningExercises()
        )
    )
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun inspectWord(word: String) {
        viewModelScope.launch {
            val clean = word.lowercase().trim().replace(Regex("[^a-zA-Z]"), "")
            val found = vocabRepo.checkDuplicate(clean)
            if (found != null) {
                _uiState.value = _uiState.value.copy(popupWordDetail = found, isWordSavedStatus = "این واژه در لغات شما قرار دارد.")
            } else {
                val dummy = VocabularyItem(
                    word = clean,
                    persianMeaning = "در حال بارگذاری معنی یا افزودن به لغات...",
                    cefrLevel = "B2",
                    source = "Reading Tap"
                )
                _uiState.value = _uiState.value.copy(popupWordDetail = dummy, isWordSavedStatus = null)
            }
        }
    }

    fun dismissWordPopup() {
        _uiState.value = _uiState.value.copy(popupWordDetail = null, isWordSavedStatus = null)
    }

    fun saveInspectedWord(item: VocabularyItem) {
        viewModelScope.launch {
            vocabRepo.insert(item.copy(id = 0L, source = "Reading Tap"))
            _uiState.value = _uiState.value.copy(isWordSavedStatus = "واژه به لغات شما افزوده شد!")
        }
    }

    fun completeExercise(xp: Int) {
        viewModelScope.launch {
            profileRepo.addXp(xp)
        }
    }
}
