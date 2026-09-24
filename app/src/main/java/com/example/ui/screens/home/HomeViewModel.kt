package com.example.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.StreakInfo
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyItem
import com.example.data.repository.DailyStreakRepository
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val userProfile: UserProfile = UserProfile(),
    val totalWordsCount: Int = 0,
    val learnedWordsCount: Int = 0,
    val dueWordsCount: Int = 0,
    val dueWords: List<VocabularyItem> = emptyList(),
    val dailyMinutesPlanned: Int = 35,
    val dailyMinutesCompleted: Int = 12,
    val dailyVocabReviewGoal: Int = 20,
    val streakDays: Int = 4,
    val isLoading: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val vocabRepo = VocabularyRepository(db.vocabularyDao(), db.vocabularyPackDao())
    private val profileRepo = UserProfileRepository(db.userProfileDao())
    val streakRepo = DailyStreakRepository(db.dailyStreakDao(), db.userProfileDao())

    val streakInfo: StateFlow<StreakInfo> = streakRepo.streakInfo.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StreakInfo()
    )

    val uiState: StateFlow<HomeUiState> = combine(
        profileRepo.profile,
        vocabRepo.totalCount,
        vocabRepo.learnedCount,
        vocabRepo.getDueVocabularies()
    ) { profile, total, learned, dueList ->
        HomeUiState(
            userProfile = profile ?: UserProfile(),
            totalWordsCount = total,
            learnedWordsCount = learned,
            dueWordsCount = dueList.size,
            dueWords = dueList.take(5),
            dailyMinutesPlanned = profile?.dailyMinutes ?: 35,
            streakDays = profile?.streakDays ?: 4
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun addXp(amount: Int) {
        viewModelScope.launch {
            profileRepo.addXp(amount)
        }
    }
}
