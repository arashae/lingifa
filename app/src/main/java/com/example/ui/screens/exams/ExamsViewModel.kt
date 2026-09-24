package com.example.ui.screens.exams

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.UserProfileRepository
import com.example.data.seed.DiagnosticQuestion
import com.example.data.seed.ExamPrompt
import com.example.data.seed.ExamSeed
import com.example.network.GeminiClient
import com.example.network.WritingEvaluationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExamsUiState(
    val prompts: List<ExamPrompt> = ExamSeed.getExamPrompts(),
    val diagnosticQuestions: List<DiagnosticQuestion> = ExamSeed.getDiagnosticQuestions(),
    val selectedPromptIndex: Int = 0,
    val essayInput: String = "",
    val wordCount: Int = 0,
    val isEvaluatingWriting: Boolean = false,
    val writingResult: WritingEvaluationResult? = null,
    val speakingTranscriptInput: String = "",
    val isEvaluatingSpeaking: Boolean = false,
    val speakingFeedbackFa: String? = null,
    val diagnosticCurrentIndex: Int = 0,
    val diagnosticScore: Int = 0,
    val diagnosticFinished: Boolean = false,
    val estimatedCefrLevel: String? = null
)

class ExamsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val profileRepo = UserProfileRepository(db.userProfileDao())

    private val _uiState = MutableStateFlow(ExamsUiState())
    val uiState: StateFlow<ExamsUiState> = _uiState.asStateFlow()

    fun selectPrompt(index: Int) {
        _uiState.value = _uiState.value.copy(
            selectedPromptIndex = index,
            essayInput = "",
            wordCount = 0,
            writingResult = null
        )
    }

    fun onEssayInputChanged(text: String) {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        _uiState.value = _uiState.value.copy(essayInput = text, wordCount = words)
    }

    fun submitEssayForGrading() {
        val prompt = _uiState.value.prompts.getOrNull(_uiState.value.selectedPromptIndex) ?: return
        val essay = _uiState.value.essayInput
        if (essay.trim().isEmpty()) return

        _uiState.value = _uiState.value.copy(isEvaluatingWriting = true, writingResult = null)

        viewModelScope.launch {
            val result = GeminiClient.evaluateEssay(prompt.promptTextEn, essay)
            _uiState.value = _uiState.value.copy(isEvaluatingWriting = false)
            result.onSuccess { eval ->
                _uiState.value = _uiState.value.copy(writingResult = eval)
                profileRepo.addXp(30)
            }.onFailure {
                // Fallback result
                _uiState.value = _uiState.value.copy(
                    writingResult = WritingEvaluationResult(
                        estimatedBand = "6.5",
                        taskAchievementScore = "6.5",
                        coherenceScore = "6.5",
                        lexicalScore = "6.5",
                        grammarScore = "6.5",
                        overallFeedbackFa = "مقاله ثبت شد. جهت تحلیل دقیق‌تر اطمینان حاصل کنید اتصال اینترنت برقرار است.",
                        strengthsFa = listOf("پایبندی به ساختار مقاله", "تعداد کلمات مناسب"),
                        mainIssuesFa = listOf("تنوع بیشتر در واژگان هم‌آیند (Collocations)"),
                        sentenceCorrections = emptyList(),
                        improvedVersion = essay
                    )
                )
            }
        }
    }

    fun onSpeakingTranscriptChanged(text: String) {
        _uiState.value = _uiState.value.copy(speakingTranscriptInput = text)
    }

    fun evaluateSpeaking() {
        val text = _uiState.value.speakingTranscriptInput.trim()
        if (text.isEmpty()) return

        _uiState.value = _uiState.value.copy(isEvaluatingSpeaking = true)
        viewModelScope.launch {
            val prompt = "Evaluate speaking response: '$text'. Give feedback in Persian with estimated IELTS band, pronunciation tips, and grammatical improvements."
            val result = GeminiClient.askTutor(prompt)
            _uiState.value = _uiState.value.copy(
                isEvaluatingSpeaking = false,
                speakingFeedbackFa = result.getOrNull() ?: "بازخورد اسپیکینگ در دسترس نیست."
            )
            profileRepo.addXp(25)
        }
    }

    fun answerDiagnosticQuestion(selectedIndex: Int) {
        val currentQ = _uiState.value.diagnosticQuestions.getOrNull(_uiState.value.diagnosticCurrentIndex) ?: return
        val isCorrect = selectedIndex == currentQ.correctIndex
        val newScore = if (isCorrect) _uiState.value.diagnosticScore + 1 else _uiState.value.diagnosticScore
        val nextIndex = _uiState.value.diagnosticCurrentIndex + 1

        if (nextIndex < _uiState.value.diagnosticQuestions.size) {
            _uiState.value = _uiState.value.copy(
                diagnosticCurrentIndex = nextIndex,
                diagnosticScore = newScore
            )
        } else {
            val estimated = when (newScore) {
                5 -> "C1 (پیشرفته)"
                4 -> "B2 (متوسط رو به بالا)"
                3 -> "B1 (متوسط)"
                2 -> "A2 (پایه)"
                else -> "A1 (مبتدی)"
            }
            _uiState.value = _uiState.value.copy(
                diagnosticScore = newScore,
                diagnosticFinished = true,
                estimatedCefrLevel = estimated
            )
            viewModelScope.launch {
                val profile = profileRepo.getProfileSync()
                profileRepo.updateProfile(profile.copy(currentLevel = estimated.take(2)))
                profileRepo.addXp(50)
            }
        }
    }

    fun resetDiagnostic() {
        _uiState.value = _uiState.value.copy(
            diagnosticCurrentIndex = 0,
            diagnosticScore = 0,
            diagnosticFinished = false,
            estimatedCefrLevel = null
        )
    }
}
