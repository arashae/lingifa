package com.example.ui.screens.vocab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TtsManager
import com.example.data.local.AppDatabase
import com.example.data.model.ExamTrackStage
import com.example.data.model.ExamTrackState
import com.example.data.model.ExamTrackType
import com.example.data.model.StreakInfo
import com.example.data.model.StreakUpdateResult
import com.example.data.repository.DailyStreakRepository
import com.example.data.repository.ExamTrackRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExamTrackUiState(
    val selectedTrack: ExamTrackType = ExamTrackType.IELTS,
    val activeStudyStage: ExamTrackStage? = null,
    val studyWordIndex: Int = 0,
    val isCardFlipped: Boolean = false,
    val isUkAccent: Boolean = false,
    val isPlayingAudio: Boolean = false,
    val statusMessage: String? = null,
    val streakResult: StreakUpdateResult? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExamTrackViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val trackRepository = ExamTrackRepository(
        db.examTrackDao(),
        db.userProfileDao(),
        db.dailyStreakDao(),
        db.vocabularyDao()
    )
    val streakRepository = DailyStreakRepository(
        db.dailyStreakDao(),
        db.userProfileDao()
    )

    private val _uiState = MutableStateFlow(ExamTrackUiState())
    val uiState: StateFlow<ExamTrackUiState> = _uiState.asStateFlow()

    private val _selectedTrackFlow = MutableStateFlow(ExamTrackType.IELTS)

    val currentTrackState: StateFlow<ExamTrackState> = _selectedTrackFlow
        .flatMapLatest { trackType -> trackRepository.getTrackState(trackType) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExamTrackState(ExamTrackType.IELTS)
        )

    val streakInfo: StateFlow<StreakInfo> = streakRepository.streakInfo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StreakInfo()
        )

    fun selectTrack(track: ExamTrackType) {
        _selectedTrackFlow.value = track
        _uiState.update {
            it.copy(
                selectedTrack = track,
                activeStudyStage = null,
                studyWordIndex = 0,
                isCardFlipped = false
            )
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch {
            trackRepository.updateDailyGoal(_uiState.value.selectedTrack, goal)
            _uiState.update { it.copy(statusMessage = "هدف روزانه روی $goal واژه تنظیم شد.") }
        }
    }

    /**
     * A master stage may contain thousands of words. A study session intentionally opens only
     * the user's daily goal worth of not-yet-mastered cards, keeping Compose/SRS responsive.
     */
    fun startStudyingStage(stage: ExamTrackStage) {
        viewModelScope.launch {
            trackRepository.selectCurrentStage(_uiState.value.selectedTrack, stage.stageNumber)

            val dailyGoal = currentTrackState.value.dailyGoalWords.coerceAtLeast(1)
            val sessionWords = stage.words
                .asSequence()
                .filterNot { it.isMastered }
                .take(dailyGoal)
                .toList()

            if (sessionWords.isEmpty()) {
                _uiState.update {
                    it.copy(
                        activeStudyStage = null,
                        studyWordIndex = 0,
                        isCardFlipped = false,
                        statusMessage = "همه واژه‌های این مرحله را یاد گرفته‌اید."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    activeStudyStage = stage.copy(
                        words = sessionWords,
                        masteredCount = 0,
                        totalCount = sessionWords.size
                    ),
                    studyWordIndex = 0,
                    isCardFlipped = false,
                    statusMessage = "جلسه امروز: ${sessionWords.size} واژه از مرحله ${stage.stageNumber}"
                )
            }
        }
    }

    fun closeStudySession() {
        _uiState.update {
            it.copy(
                activeStudyStage = null,
                studyWordIndex = 0,
                isCardFlipped = false
            )
        }
    }

    fun flipCard() {
        _uiState.update { it.copy(isCardFlipped = !it.isCardFlipped) }
    }

    fun toggleAccent() {
        _uiState.update { it.copy(isUkAccent = !it.isUkAccent) }
    }

    fun playAudio(tts: TtsManager, text: String) {
        _uiState.update { it.copy(isPlayingAudio = true) }
        tts.speak(text, isUk = _uiState.value.isUkAccent, speechRate = 0.9f)
        viewModelScope.launch {
            delay(1200)
            _uiState.update { it.copy(isPlayingAudio = false) }
        }
    }

    fun recordWordResult(isMastered: Boolean) {
        val stage = _uiState.value.activeStudyStage ?: return
        val currentWord = stage.words.getOrNull(_uiState.value.studyWordIndex) ?: return

        viewModelScope.launch {
            val streakResult = trackRepository.updateWordMastery(
                trackType = _uiState.value.selectedTrack,
                wordId = currentWord.id,
                stageNumber = stage.stageNumber,
                isMastered = isMastered
            )

            val nextIndex = _uiState.value.studyWordIndex + 1
            if (nextIndex < stage.words.size) {
                _uiState.update {
                    it.copy(
                        studyWordIndex = nextIndex,
                        isCardFlipped = false,
                        streakResult = if (streakResult.isNewMilestoneReached) streakResult else null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        activeStudyStage = null,
                        studyWordIndex = 0,
                        isCardFlipped = false,
                        statusMessage = "جلسه امروز تمام شد؛ پیشرفت شما ذخیره شد (+۱۵ XP برای هر واژه).",
                        streakResult = streakResult
                    )
                }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun dismissStreakModal() {
        _uiState.update { it.copy(streakResult = null) }
    }
}
