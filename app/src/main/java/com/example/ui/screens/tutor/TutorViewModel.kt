package com.example.ui.screens.tutor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.GeminiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TutorUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            sender = "AI",
            text = "درود! من معلم هوشمند LinguaFa AI هستم 👋\nمی‌تونید هر سوالی در رابطه با گرامر، لغات آیلتس و تافل، تفاوت کلمات مشابه، یا اشکالات زبانی دارید از من به فارسی بپرسید."
        )
    ),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val suggestedPrompts: List<String> = listOf(
        "فرق affect و effect چیه؟",
        "تفاوت economic و economical با مثال",
        "توضیح زمان حال کامل به زبان ساده",
        "ازم چند تا لغت سطح C1 آیلتس بپرس",
        "اشتباهات گرامری رایج ایرانی‌ها در رایتینگ"
    )
)

class TutorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TutorUiState())
    val uiState: StateFlow<TutorUiState> = _uiState.asStateFlow()

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage(userText: String = _uiState.value.inputText) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val userMessage = ChatMessage(sender = "USER", text = trimmed)
        val updatedMessages = _uiState.value.messages + userMessage

        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            inputText = "",
            isLoading = true
        )

        viewModelScope.launch {
            val contextBuilder = StringBuilder()
            updatedMessages.takeLast(6).forEach { msg ->
                contextBuilder.append("${msg.sender}: ${msg.text}\n")
            }

            val result = GeminiClient.askTutor(trimmed, contextBuilder.toString())
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.onSuccess { responseText ->
                val aiMessage = ChatMessage(sender = "AI", text = responseText)
                _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + aiMessage)
            }.onFailure { err ->
                val errorMsg = ChatMessage(
                    sender = "AI",
                    text = "متاسفانه در حال حاضر امکان برقراری ارتباط وجود ندارد: ${err.message}"
                )
                _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + errorMsg)
            }
        }
    }
}
