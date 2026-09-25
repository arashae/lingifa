package com.example.ui.screens.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.StreakInfo
import com.example.data.model.UserProfile
import com.example.data.repository.DailyStreakRepository
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.VocabularyRepository
import com.example.vocab.VocabularyStudyPolicy
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class HomeUiState(
    val userProfile: UserProfile = UserProfile(),
    val totalWordsCount: Int = 0,
    val learnedWordsCount: Int = 0,
    val dueWordsCount: Int = 0,
    val weakWordsCount: Int = 0,
    val dailyMinutesPlanned: Int = 35,
    val dailyMinutesCompleted: Int = 0,
    val dailyNewWordsLimit: Int = 15,
    val streakDays: Int = 0,
    val daysRemaining: Int? = null,
    val isLoading: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val vocabRepo = VocabularyRepository(
        db.vocabularyDao(),
        db.vocabularyPackDao(),
        db.vocabularyPackItemDao(),
        db.vocabularySenseDao()
    )
    private val profileRepo = UserProfileRepository(db.userProfileDao())
    val streakRepo = DailyStreakRepository(db.dailyStreakDao(), db.userProfileDao())

    private val studyPreferences = application.getSharedPreferences(
        VocabularyStudyPolicy.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    val streakInfo: StateFlow<StreakInfo> = streakRepo.streakInfo.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StreakInfo()
    )

    private val userProfileAndStreak = combine(
        profileRepo.profile,
        streakRepo.allStreakRecords
    ) { profile, streakRecords ->
        val today = streakRepo.getTodayDateString()
        val todayRecord = streakRecords.find { it.date == today }
        Triple(profile, todayRecord?.minutesSpent ?: 0, profile?.streakDays ?: 0)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        userProfileAndStreak,
        vocabRepo.totalCount,
        vocabRepo.learnedCount,
        vocabRepo.getDueCount(),
        vocabRepo.getWeakCount()
    ) { (profile, completedMinutes, streakDays), total, learned, dueCount, weakCount ->
        val dailyNewLimit = studyPreferences
            .getInt(VocabularyStudyPolicy.KEY_DAILY_NEW_LIMIT, 15)
            .takeIf { it in VocabularyStudyPolicy.DAILY_NEW_LIMIT_OPTIONS }
            ?: 15

        HomeUiState(
            userProfile = profile ?: UserProfile(),
            totalWordsCount = total,
            learnedWordsCount = learned,
            dueWordsCount = dueCount,
            weakWordsCount = weakCount,
            dailyMinutesPlanned = profile?.dailyMinutes ?: 35,
            dailyMinutesCompleted = completedMinutes,
            dailyNewWordsLimit = dailyNewLimit,
            streakDays = streakDays,
            daysRemaining = calculateDaysRemaining(profile?.testDateFa ?: "")
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    private fun calculateDaysRemaining(testDateStr: String): Int? {
        if (testDateStr.isBlank()) return null
        val trimmed = testDateStr.trim()
        trimmed.toIntOrNull()?.let { return it.coerceAtLeast(0) }

        for (pattern in listOf("yyyy-MM-dd", "yyyy/MM/dd")) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val date = sdf.parse(trimmed)
                if (date != null) {
                    val diffMs = date.time - System.currentTimeMillis()
                    return maxOf(0, (diffMs / (1000L * 60 * 60 * 24)).toInt())
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun addXp(amount: Int) {
        viewModelScope.launch {
            profileRepo.addXp(amount)
        }
    }
}
