package com.example.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.exporter.VocabularyExporter
import com.example.data.local.AppDatabase
import com.example.data.model.MistakeRecord
import com.example.data.model.UserProfile
import com.example.data.repository.MistakeRepository
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val mistakes: List<MistakeRecord> = emptyList(),
    val totalWordsCount: Int = 0,
    val learnedWordsCount: Int = 0,
    val exportedContent: String? = null,
    val exportFormat: String? = null,
    val statusMessage: String? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val profileRepo = UserProfileRepository(db.userProfileDao())
    private val mistakeRepo = MistakeRepository(db.mistakeDao())
    private val vocabRepo = VocabularyRepository(db.vocabularyDao(), db.vocabularyPackDao())

    private val _exportedContent = MutableStateFlow<String?>(null)
    private val _exportFormat = MutableStateFlow<String?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        profileRepo.profile,
        mistakeRepo.allMistakes,
        vocabRepo.totalCount,
        vocabRepo.learnedCount,
        _exportedContent,
        _exportFormat,
        _statusMessage
    ) { params ->
        val prof = params[0] as? UserProfile ?: UserProfile()
        @Suppress("UNCHECKED_CAST")
        val mistakes = params[1] as List<MistakeRecord>
        val total = params[2] as Int
        val learned = params[3] as Int
        val expContent = params[4] as? String
        val expFormat = params[5] as? String
        val status = params[6] as? String

        ProfileUiState(
            profile = prof,
            mistakes = mistakes,
            totalWordsCount = total,
            learnedWordsCount = learned,
            exportedContent = expContent,
            exportFormat = expFormat,
            statusMessage = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun updateProfile(
        name: String,
        goal: String,
        level: String,
        dailyMinutes: Int,
        targetScore: String
    ) {
        viewModelScope.launch {
            val current = profileRepo.getProfileSync()
            profileRepo.updateProfile(
                current.copy(
                    userName = name,
                    targetGoal = goal,
                    currentLevel = level,
                    dailyMinutes = dailyMinutes,
                    targetBandOrScore = targetScore
                )
            )
            _statusMessage.value = "پروفایل با موفقیت به‌روزرسانی شد."
        }
    }

    fun deleteMistake(id: Long) {
        viewModelScope.launch {
            mistakeRepo.deleteMistake(id)
            _statusMessage.value = "اشتباه از دفترچه حذف شد."
        }
    }

    fun markMistakeReviewed(id: Long, isReviewed: Boolean = true) {
        viewModelScope.launch {
            mistakeRepo.markReviewed(id, isReviewed)
            _statusMessage.value = if (isReviewed) "اشتباه به عنوان مرور شده و حل‌شده ثبت شد." else "اشتباه نیازمند مرور مجدد است."
        }
    }

    fun exportVocabulary(format: String) {
        viewModelScope.launch {
            val words = vocabRepo.allVocabularies.first()
            val result = if (format == "CSV") {
                VocabularyExporter.exportToCsv(words)
            } else {
                VocabularyExporter.exportToJson(words)
            }
            _exportedContent.value = result
            _exportFormat.value = format
            _statusMessage.value = "خروجی $format آماده شد."
        }
    }

    fun clearExport() {
        _exportedContent.value = null
        _exportFormat.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
