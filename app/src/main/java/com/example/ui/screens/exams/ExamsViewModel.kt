package com.example.ui.screens.exams

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.UserProfileRepository
import com.example.data.seed.DiagnosticQuestion
import com.example.data.seed.ExamPrompt
import com.example.data.seed.ExamSeed
import com.example.network.AiApiClient
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
            val result = AiApiClient.evaluateEssay(
                prompt = prompt.promptTextEn,
                essay = essay,
                context = getApplication()
            )
            _uiState.value = _uiState.value.copy(isEvaluatingWriting = false)
            result.onSuccess { evaluation ->
                _uiState.value = _uiState.value.copy(writingResult = evaluation)
                profileRepo.addXp(30)
            }.onFailure { error ->
                // Never fabricate an IELTS score when an evaluator was unavailable.
                _uiState.value = _uiState.value.copy(
                    writingResult = WritingEvaluationResult(
                        estimatedBand = "N/A",
                        taskAchievementScore = "N/A",
                        coherenceScore = "N/A",
                        lexicalScore = "N/A",
                        grammarScore = "N/A",
                        overallFeedbackFa = "ارزیابی هوش مصنوعی در دسترس نبود: ${error.message ?: "اتصال یا کلید API را بررسی کنید."}",
                        strengthsFa = emptyList(),
                        mainIssuesFa = emptyList(),
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

        val prompt = _uiState.value.prompts.getOrNull(_uiState.value.selectedPromptIndex)?.promptTextEn
            ?: "General speaking response"

        _uiState.value = _uiState.value.copy(isEvaluatingSpeaking = true)
        viewModelScope.launch {
            val result = AiApiClient.evaluateSpeaking(
                prompt = prompt,
                transcript = text,
                context = getApplication()
            )

            val feedback = result.fold(
                onSuccess = { evaluation ->
                    buildString {
                        append("برآورد متنی: ${evaluation.estimatedBand}\n\n")
                        if (evaluation.fluencyFeedbackFa.isNotBlank()) append("روانی/انسجام: ${evaluation.fluencyFeedbackFa}\n\n")
                        if (evaluation.lexicalFeedbackFa.isNotBlank()) append("واژگان: ${evaluation.lexicalFeedbackFa}\n\n")
                        if (evaluation.grammarFeedbackFa.isNotBlank()) append("گرامر: ${evaluation.grammarFeedbackFa}\n\n")
                        append(evaluation.pronunciationHintsFa.ifBlank {
                            "برای ارزیابی تلفظ باید صدای واقعی بررسی شود؛ متن به‌تنهایی برای نمره‌دادن تلفظ کافی نیست."
                        })
                        if (evaluation.betterPhrasings.isNotEmpty()) {
                            append("\n\nعبارت‌های بهتر:\n")
                            evaluation.betterPhrasings.forEach { append("• $it\n") }
                        }
                    }.trim()
                },
                onFailure = { error ->
                    "بازخورد اسپیکینگ در دسترس نیست: ${error.message ?: "کلید DeepSeek یا اتصال اینترنت را بررسی کنید."}"
                }
            )

            _uiState.value = _uiState.value.copy(
                isEvaluatingSpeaking = false,
                speakingFeedbackFa = feedback
            )
            if (result.isSuccess) profileRepo.addXp(25)
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
            // This five-question quiz is only a rough in-app starting estimate, not a formal CEFR assessment.
            val estimated = when (newScore) {
                5 -> "C1 (برآورد اولیه)"
                4 -> "B2 (برآورد اولیه)"
                3 -> "B1 (برآورد اولیه)"
                2 -> "A2 (برآورد اولیه)"
                else -> "A1 (برآورد اولیه)"
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
