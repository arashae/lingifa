package com.example.ui.screens.exams

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TtsManager
import com.example.data.ai.IeltsSpeakingPromptGenerator
import com.example.data.local.AppDatabase
import com.example.data.model.IeltsSpeakingFeedback
import com.example.data.model.IeltsSpeakingPrompt
import com.example.data.model.IeltsSpeakingSessionRecord
import com.example.data.model.StreakInfo
import com.example.data.model.StreakUpdateResult
import com.example.data.repository.DailyStreakRepository
import com.example.data.repository.IeltsSpeakingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IeltsSpeakingUiState(
    val selectedPart: Int = 2, // 1, 2, 3
    val currentPrompt: IeltsSpeakingPrompt = IeltsSpeakingPromptGenerator.DEFAULT_PROMPTS[0],
    val candidateTranscript: String = "",
    val isEvaluating: Boolean = false,
    val feedback: IeltsSpeakingFeedback? = null,
    val isRecording: Boolean = false,
    val speakingTimerSeconds: Int = 0,
    val prepTimerSeconds: Int = 60,
    val isPrepTimerRunning: Boolean = false,
    val isUkAccent: Boolean = false,
    val isExaminerSpeaking: Boolean = false,
    val isModelSpeaking: Boolean = false,
    val statusMessage: String? = null,
    val streakResult: StreakUpdateResult? = null
)

class IeltsSpeakingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val speakingRepository = IeltsSpeakingRepository(
        db.ieltsSpeakingDao(),
        db.userProfileDao(),
        db.dailyStreakDao()
    )
    val streakRepository = DailyStreakRepository(db.dailyStreakDao(), db.userProfileDao())

    private val _uiState = MutableStateFlow(IeltsSpeakingUiState())
    val uiState: StateFlow<IeltsSpeakingUiState> = _uiState.asStateFlow()

    val pastSessions: StateFlow<List<IeltsSpeakingSessionRecord>> = speakingRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val averageBandScore: StateFlow<Float?> = speakingRepository.averageBandScore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val streakInfo: StateFlow<StreakInfo> = streakRepository.streakInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StreakInfo())

    private var prepTimerJob: Job? = null
    private var speakingTimerJob: Job? = null

    init {
        selectPart(2)
    }

    fun selectPart(part: Int) {
        val prompts = speakingRepository.getAvailablePrompts().filter { it.part == part }
        val prompt = prompts.firstOrNull() ?: IeltsSpeakingPromptGenerator.DEFAULT_PROMPTS.first()
        stopAllTimers()
        _uiState.update {
            it.copy(
                selectedPart = part,
                currentPrompt = prompt,
                candidateTranscript = "",
                feedback = null,
                isRecording = false,
                speakingTimerSeconds = 0,
                prepTimerSeconds = if (part == 2) 60 else 0,
                isPrepTimerRunning = false
            )
        }
    }

    fun selectPrompt(prompt: IeltsSpeakingPrompt) {
        stopAllTimers()
        _uiState.update {
            it.copy(
                selectedPart = prompt.part,
                currentPrompt = prompt,
                candidateTranscript = "",
                feedback = null,
                speakingTimerSeconds = 0,
                prepTimerSeconds = if (prompt.part == 2) 60 else 0
            )
        }
    }

    fun generateAiPrompt(topic: String = "Artificial Intelligence") {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "در حال تولید سوال و Cue Card جدید با هوش مصنوعی...") }
            val newPrompt = speakingRepository.generatePrompt(_uiState.value.selectedPart, topic)
            stopAllTimers()
            _uiState.update {
                it.copy(
                    currentPrompt = newPrompt,
                    candidateTranscript = "",
                    feedback = null,
                    speakingTimerSeconds = 0,
                    prepTimerSeconds = if (newPrompt.part == 2) 60 else 0,
                    statusMessage = "سوال جدید آیلتس آماده شد!"
                )
            }
        }
    }

    fun onTranscriptChanged(text: String) {
        _uiState.update { it.copy(candidateTranscript = text) }
    }

    fun togglePrepTimer() {
        if (_uiState.value.isPrepTimerRunning) {
            prepTimerJob?.cancel()
            _uiState.update { it.copy(isPrepTimerRunning = false) }
        } else {
            _uiState.update { it.copy(isPrepTimerRunning = true) }
            prepTimerJob = viewModelScope.launch {
                while (_uiState.value.prepTimerSeconds > 0 && _uiState.value.isPrepTimerRunning) {
                    delay(1000)
                    _uiState.update { it.copy(prepTimerSeconds = it.prepTimerSeconds - 1) }
                }
                _uiState.update { it.copy(isPrepTimerRunning = false) }
            }
        }
    }

    fun resetPrepTimer() {
        prepTimerJob?.cancel()
        _uiState.update { it.copy(prepTimerSeconds = 60, isPrepTimerRunning = false) }
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            stopSpeakingTimer()
        } else {
            startSpeakingTimer()
        }
    }

    private fun startSpeakingTimer() {
        stopAllTimers()
        _uiState.update { it.copy(isRecording = true, speakingTimerSeconds = 0) }
        speakingTimerJob = viewModelScope.launch {
            while (_uiState.value.isRecording) {
                delay(1000)
                _uiState.update { it.copy(speakingTimerSeconds = it.speakingTimerSeconds + 1) }
            }
        }
    }

    private fun stopSpeakingTimer() {
        speakingTimerJob?.cancel()
        _uiState.update { it.copy(isRecording = false) }
    }

    private fun stopAllTimers() {
        prepTimerJob?.cancel()
        speakingTimerJob?.cancel()
        _uiState.update { it.copy(isPrepTimerRunning = false, isRecording = false) }
    }

    fun toggleAccent() {
        _uiState.update { it.copy(isUkAccent = !it.isUkAccent) }
    }

    fun playExaminerPrompt(tts: TtsManager) {
        val promptText = buildExaminerAudioScript(_uiState.value.currentPrompt)
        _uiState.update { it.copy(isExaminerSpeaking = true) }
        tts.speak(promptText, isUk = _uiState.value.isUkAccent, speechRate = 0.9f)
        viewModelScope.launch {
            delay(3500)
            _uiState.update { it.copy(isExaminerSpeaking = false) }
        }
    }

    fun playModelAnswer(tts: TtsManager) {
        val modelAnswer = _uiState.value.feedback?.band8ModelResponseEn ?: return
        _uiState.update { it.copy(isModelSpeaking = true) }
        tts.speak(modelAnswer, isUk = _uiState.value.isUkAccent, speechRate = 0.9f)
        viewModelScope.launch {
            delay(5000)
            _uiState.update { it.copy(isModelSpeaking = false) }
        }
    }

    private fun buildExaminerAudioScript(prompt: IeltsSpeakingPrompt): String {
        return if (prompt.part == 2) {
            "In this part, I'd like you to speak for one to two minutes on the following topic. You will have one minute to prepare. Here is your question: ${prompt.questionEn}"
        } else {
            prompt.questionEn
        }
    }

    fun evaluateSpeakingResponse() {
        val transcript = _uiState.value.candidateTranscript.trim()
        if (transcript.isBlank()) {
            _uiState.update { it.copy(statusMessage = "لطفاً ابتدا صحبت کرده یا متن پاسخ انگلیسی خود را بنویسید.") }
            return
        }

        stopAllTimers()
        _uiState.update { it.copy(isEvaluating = true, statusMessage = null) }

        viewModelScope.launch {
            val (feedback, streakResult) = speakingRepository.evaluateAndSaveSession(
                prompt = _uiState.value.currentPrompt,
                userTranscript = transcript,
                durationSeconds = _uiState.value.speakingTimerSeconds.coerceAtLeast(30)
            )

            _uiState.update {
                it.copy(
                    isEvaluating = false,
                    feedback = feedback,
                    streakResult = streakResult,
                    statusMessage = "ارزیابی اسپیکینگ با موفقیت انجام و نمره آزمون ذخیره گردید (+۳۵ XP)!"
                )
            }
        }
    }

    fun populateSampleCandidateAnswer() {
        val sample = when (_uiState.value.selectedPart) {
            2 -> "Well, I would like to talk about an innovative environmental project in my city that aims to mitigate urban air pollution. It was launched approximately two years ago by local environmentalists. The initiative focuses on expanding urban green corridors and promoting electric public transit. In my opinion, this project is truly indispensable because it significantly reduces harmful emissions and enhances public well-being."
            1 -> "Personally speaking, I am currently pursuing a degree in computer engineering. I chose this field because technology has always been intellectually stimulating for me, and I aspire to build impactful software solutions in the future."
            else -> "From my perspective, rapid urbanization has undeniably contributed to social alienation. In modern metropolitan areas, people lead fast-paced lifestyles, leaving little time to cultivate meaningful community cohesion."
        }
        _uiState.update { it.copy(candidateTranscript = sample) }
    }

    fun dismissStreakModal() {
        _uiState.update { it.copy(streakResult = null) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
